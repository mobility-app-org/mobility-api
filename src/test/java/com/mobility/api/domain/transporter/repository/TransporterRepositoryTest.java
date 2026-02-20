package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.repository.OfficeRepository;
import com.mobility.api.domain.transporter.DispatchStatus;
import com.mobility.api.domain.transporter.TransporterStatus;
import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import com.mobility.api.domain.transporter.entity.Transporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("TransporterRepository 테스트")
class TransporterRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>(
            DockerImageName.parse("postgis/postgis:16-3.4")
                    .asCompatibleSubstituteFor("postgres")
    )
            .withDatabaseName("mobility")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private OfficeRepository officeRepository;

    private GeometryFactory geometryFactory;
    private Office testOffice;
    private Transporter testTransporter;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();

        // 테스트용 사무실 생성
        testOffice = Office.builder()
                .officeName("테스트 사무실")
                .officeRegistrationNumber("123-45-67890")
                .officeAddress("서울시 강남구")
                .officeTelNumber("02-1234-5678")
                .build();
        testOffice = officeRepository.save(testOffice);

        // 테스트용 기사 생성 (강남역 근처)
        Point gangnamLocation = createPoint(127.0276, 37.4979); // 강남역 (경도, 위도)

        testTransporter = Transporter.builder()
                .name("김철수")
                .phone("010-1234-5678")
                .currentLocation(gangnamLocation)
                .isAutoDispatch(true)
                .status(TransporterStatus.ACTIVE)
                .dispatchStatus(DispatchStatus.EMPTY)
                .office(testOffice)
                .build();
        testTransporter = transporterRepository.save(testTransporter);
    }

    @Test
    @DisplayName("기사 저장 및 조회")
    void saveAndFindTransporter() {
        // when
        Optional<Transporter> found = transporterRepository.findById(testTransporter.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
        assertThat(found.get().getPhone()).isEqualTo("010-1234-5678");
        assertThat(found.get().isAutoDispatch()).isTrue();
    }

    @Test
    @DisplayName("전화번호로 기사 조회")
    void findByPhone() {
        // when
        Optional<Transporter> found = transporterRepository.findByPhone("010-1234-5678");

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("김철수");
    }

    @Test
    @DisplayName("전화번호 중복 체크 - 존재함")
    void existsByPhone_True() {
        // when
        boolean exists = transporterRepository.existsByPhone("010-1234-5678");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("전화번호 중복 체크 - 존재하지 않음")
    void existsByPhone_False() {
        // when
        boolean exists = transporterRepository.existsByPhone("010-9999-9999");

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("비관적 락으로 기사 조회")
    void findByIdWithPessimisticLock() {
        // when
        Optional<Transporter> found = transporterRepository.findByIdWithPessimisticLock(testTransporter.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testTransporter.getId());
    }

    @Test
    @DisplayName("근처 기사 조회 - 반경 내 기사 찾기")
    void findNearbyTransporters() {
        // given
        // 강남역 근처에 추가 기사들 배치
        Point location1 = createPoint(127.0286, 37.4989); // 약 100m 거리
        Point location2 = createPoint(127.0376, 37.5079); // 약 1.2km 거리
        Point location3 = createPoint(127.1000, 37.5500); // 약 8km 거리

        Transporter nearby1 = createTransporter("이영희", "010-1111-2222", location1, true);
        Transporter nearby2 = createTransporter("박민수", "010-2222-3333", location2, true);
        Transporter faraway = createTransporter("최동수", "010-3333-4444", location3, true);

        transporterRepository.saveAll(List.of(nearby1, nearby2, faraway));

        // when - 강남역 기준 2km 반경 내 기사 조회
        double lat = 37.4979;
        double lon = 127.0276;
        double radiusMeters = 2000; // 2km

        List<TransporterDistanceProjection> result = transporterRepository.findNearbyTransporters(
                lat, lon, radiusMeters
        );

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSizeGreaterThanOrEqualTo(3); // testTransporter + nearby1 + nearby2
        // faraway는 2km 밖이므로 제외되어야 함
        assertThat(result).allMatch(t -> t.getDistanceInMeters() <= radiusMeters);
        // 거리순으로 정렬되어야 함
        assertThat(result.get(0).getDistanceInMeters()).isLessThan(
                result.get(result.size() - 1).getDistanceInMeters()
        );
    }

    @Test
    @DisplayName("자동배차 적격 기사 조회 - 50km 내 자동배차 ON 기사만")
    void findEligibleDriversForAutoDispatch() {
        // given
        Point location1 = createPoint(127.0286, 37.4989);
        Point location2 = createPoint(127.0376, 37.5079);

        // 자동배차 ON
        Transporter autoOn1 = createTransporter("자동ON1", "010-1111-1111", location1, true);
        Transporter autoOn2 = createTransporter("자동ON2", "010-2222-2222", location2, true);

        // 자동배차 OFF
        Transporter autoOff = createTransporter("자동OFF", "010-3333-3333", location1, false);

        transporterRepository.saveAll(List.of(autoOn1, autoOn2, autoOff));

        // when - 강남역 기준 자동배차 적격 기사 조회
        List<TransporterDistanceProjection> result = transporterRepository.findEligibleDriversForAutoDispatch(
                37.4979, 127.0276
        );

        // then
        assertThat(result).isNotEmpty();
        // 자동배차 OFF인 기사는 제외
        assertThat(result).allMatch(t -> !t.getName().equals("자동OFF"));
        // 최대 10명까지만 조회
        assertThat(result).hasSizeLessThanOrEqualTo(10);
        // 거리순 정렬
        if (result.size() > 1) {
            assertThat(result.get(0).getDistanceInMeters()).isLessThanOrEqualTo(
                    result.get(1).getDistanceInMeters()
            );
        }
    }

    @Test
    @DisplayName("1km 반경 내 자동배차 ON 기사 존재 여부 - 있음")
    void existsEligibleDriversWithinRadius_True() {
        // given
        Point nearbyLocation = createPoint(127.0286, 37.4989); // 약 100m 거리
        Transporter nearby = createTransporter("근처기사", "010-5555-5555", nearbyLocation, true);
        transporterRepository.save(nearby);

        // when
        boolean exists = transporterRepository.existsEligibleDriversWithinRadius(
                37.4979, 127.0276
        );

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("1km 반경 내 자동배차 ON 기사 존재 여부 - 없음")
    void existsEligibleDriversWithinRadius_False() {
        // given
        // 기존 testTransporter를 자동배차 OFF로 변경하기 위해 새로 저장
        transporterRepository.deleteAll();

        Point farLocation = createPoint(127.0500, 37.5200); // 약 3km 거리
        Transporter farTransporter = createTransporter("먼기사", "010-6666-6666", farLocation, true);
        transporterRepository.save(farTransporter);

        // when - 1km 반경 내에는 기사가 없음
        boolean exists = transporterRepository.existsEligibleDriversWithinRadius(
                37.4979, 127.0276
        );

        // then
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("특정 사무실 소속 기사 목록 조회")
    void findAllByOffice() {
        // given
        Point location = createPoint(127.0300, 37.5000);
        Transporter transporter1 = createTransporter("사무실1소속1", "010-7777-7777", location, true);
        Transporter transporter2 = createTransporter("사무실1소속2", "010-8888-8888", location, true);

        transporterRepository.saveAll(List.of(transporter1, transporter2));

        // when
        List<Transporter> result = transporterRepository.findAllByOffice(testOffice);

        // then
        assertThat(result).hasSizeGreaterThanOrEqualTo(3); // testTransporter + 2명
        assertThat(result).allMatch(t -> t.getOffice().getId().equals(testOffice.getId()));
    }

    @Test
    @DisplayName("특정 사무실 소속 기사 목록 조회 - 페이징")
    void findAllByOffice_Pageable() {
        // given
        Point location = createPoint(127.0300, 37.5000);
        Transporter transporter1 = createTransporter("기사1", "010-1001-1001", location, true);
        Transporter transporter2 = createTransporter("기사2", "010-1002-1002", location, true);

        transporterRepository.saveAll(List.of(transporter1, transporter2));

        // when
        Page<Transporter> result = transporterRepository.findAllByOffice(
                testOffice, PageRequest.of(0, 2)
        );

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("특정 사무실 소속 + 상태 필터 기사 조회")
    void findAllByOfficeAndStatus() {
        // given
        Point location = createPoint(127.0300, 37.5000);

        Transporter activeTransporter = Transporter.builder()
                .name("활성기사")
                .phone("010-9001-9001")
                .currentLocation(location)
                .isAutoDispatch(true)
                .status(TransporterStatus.ACTIVE)
                .office(testOffice)
                .build();

        Transporter pendingTransporter = Transporter.builder()
                .name("대기기사")
                .phone("010-9002-9002")
                .currentLocation(location)
                .isAutoDispatch(false)
                .status(TransporterStatus.PENDING)
                .office(testOffice)
                .build();

        transporterRepository.saveAll(List.of(activeTransporter, pendingTransporter));

        // when
        Page<Transporter> result = transporterRepository.findAllByOfficeAndStatus(
                testOffice, TransporterStatus.ACTIVE, PageRequest.of(0, 10)
        );

        // then
        assertThat(result.getContent()).isNotEmpty();
        assertThat(result.getContent()).allMatch(t -> t.getStatus() == TransporterStatus.ACTIVE);
        assertThat(result.getContent()).allMatch(t -> t.getOffice().getId().equals(testOffice.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 기사 조회 시 빈 Optional 반환")
    void findByIdNotFound() {
        // when
        Optional<Transporter> found = transporterRepository.findById(99999L);

        // then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("위치 정보가 null인 기사는 근처 조회에서 제외")
    void findNearbyTransporters_ExcludeNullLocation() {
        // given
        Transporter noLocation = Transporter.builder()
                .name("위치없음")
                .phone("010-0000-0000")
                .currentLocation(null) // 위치 정보 없음
                .isAutoDispatch(true)
                .office(testOffice)
                .build();
        transporterRepository.save(noLocation);

        // when
        List<TransporterDistanceProjection> result = transporterRepository.findNearbyTransporters(
                37.4979, 127.0276, 10000
        );

        // then
        // 위치 정보가 없는 기사는 결과에 포함되지 않아야 함
        assertThat(result).noneMatch(t -> t.getName().equals("위치없음"));
    }

    // === 헬퍼 메서드 ===

    /**
     * PostGIS Point 생성 (SRID 4326)
     * @param lon 경도 (X)
     * @param lat 위도 (Y)
     * @return Point
     */
    private Point createPoint(double lon, double lat) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return point;
    }

    /**
     * 테스트용 기사 생성
     */
    private Transporter createTransporter(String name, String phone, Point location, boolean isAutoDispatch) {
        return Transporter.builder()
                .name(name)
                .phone(phone)
                .currentLocation(location)
                .isAutoDispatch(isAutoDispatch)
                .status(TransporterStatus.ACTIVE)
                .dispatchStatus(DispatchStatus.EMPTY)
                .office(testOffice)
                .build();
    }
}
