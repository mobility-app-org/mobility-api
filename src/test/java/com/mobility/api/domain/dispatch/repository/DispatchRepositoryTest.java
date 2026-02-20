package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.dto.DispatchDistanceProjection;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@DisplayName("DispatchRepository 테스트")
class DispatchRepositoryTest {

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
    private DispatchRepository dispatchRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    private Dispatch testDispatch;
    private Transporter testTransporter;

    @BeforeEach
    void setUp() {
        // 테스트용 기사 생성
        testTransporter = Transporter.builder()
                .name("김철수")
                .phone("010-1234-5678")
                .isAutoDispatch(true)
                .build();
        testTransporter = transporterRepository.save(testTransporter);

        // 테스트용 배차 생성
        testDispatch = Dispatch.builder()
                .dispatchNumber("2024-0001")
                .startLocation("서울특별시 강남구 테헤란로 123")
                .startLatitude(37.5012)
                .startLongitude(127.0396)
                .destinationLocation("부산광역시 해운대구 우동 456")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(150000)
                .clientPhoneNumber("010-9876-5432")
                .status(StatusType.OPEN)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .paymentType(PaymentType.CASH)
                .tollType(TollType.HIPASS)
                .active(true)
                .officeId(1L)
                .memo("테스트 메모")
                .build();
        testDispatch = dispatchRepository.save(testDispatch);
    }

    @Test
    @DisplayName("배차 저장 및 조회")
    void saveAndFindDispatch() {
        // when
        Optional<Dispatch> found = dispatchRepository.findById(testDispatch.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getDispatchNumber()).isEqualTo("2024-0001");
        assertThat(found.get().getStatus()).isEqualTo(StatusType.OPEN);
        assertThat(found.get().getCharge()).isEqualTo(150000);
    }

    @Test
    @DisplayName("비관적 락으로 배차 조회")
    void findByIdWithPessimisticLock() {
        // when
        Optional<Dispatch> found = dispatchRepository.findByIdWithPessimisticLock(testDispatch.getId());

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(testDispatch.getId());
    }

    @Test
    @DisplayName("거리순 배차 조회 - 전체 상태")
    void findDispatchesByDistance_AllStatuses() {
        // given
        // 여러 배차 생성 (서울 강남 기준 다양한 거리)
        Dispatch dispatch1 = createDispatch("2024-0002", 37.5000, 127.0400, StatusType.OPEN); // 가까움
        Dispatch dispatch2 = createDispatch("2024-0003", 37.6000, 127.1000, StatusType.ASSIGNED); // 중간
        Dispatch dispatch3 = createDispatch("2024-0004", 37.4000, 126.9000, StatusType.COMPLETED); // 먼 곳

        dispatchRepository.saveAll(List.of(dispatch1, dispatch2, dispatch3));

        // when - 강남역 기준 (37.4979, 127.0276)에서 거리순 조회 (상태 필터 없음)
        List<DispatchDistanceProjection> result = dispatchRepository.findDispatchesByDistance(
                37.4979, 127.0276
        );

        // then
        assertThat(result).isNotEmpty();
        // 거리순으로 정렬되어야 함 (첫 번째가 가장 가까움)
        assertThat(result.get(0).getDistanceInMeters()).isLessThan(
                result.get(result.size() - 1).getDistanceInMeters()
        );
    }

    @Test
    @DisplayName("거리순 배차 조회 - 상태 필터링 (OPEN만)")
    void findDispatchesByDistance_FilterByStatus() {
        // given
        Dispatch openDispatch = createDispatch("2024-0005", 37.5000, 127.0400, StatusType.OPEN);
        Dispatch assignedDispatch = createDispatch("2024-0006", 37.5000, 127.0400, StatusType.ASSIGNED);

        dispatchRepository.saveAll(List.of(openDispatch, assignedDispatch));

        // when - OPEN 상태만 조회
        List<DispatchDistanceProjection> result = dispatchRepository.findDispatchesByDistanceAndStatus(
                37.4979, 127.0276, List.of("OPEN")
        );

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).allMatch(d -> d.getStatus().equals("OPEN"));
    }

    @Test
    @DisplayName("상태별 배차 카운트 조회")
    void countByStatus() {
        // given
        createDispatch("2024-0007", 37.5000, 127.0400, StatusType.OPEN);
        createDispatch("2024-0008", 37.5000, 127.0400, StatusType.OPEN);
        createDispatch("2024-0009", 37.5000, 127.0400, StatusType.ASSIGNED);

        dispatchRepository.flush();

        // when
        List<Object[]> result = dispatchRepository.countByStatus();

        // then
        assertThat(result).isNotEmpty();
        // OPEN 상태가 3개 (기존 1개 + 새로 추가한 2개)
        // ASSIGNED 상태가 1개
    }

    @Test
    @DisplayName("특정 사무실의 상태별 배차 카운트 조회")
    void countByStatusAndOfficeId() {
        // given
        Long officeId = 1L;
        createDispatchWithOffice("2024-0010", officeId, StatusType.OPEN);
        createDispatchWithOffice("2024-0011", officeId, StatusType.ASSIGNED);
        createDispatchWithOffice("2024-0012", 2L, StatusType.OPEN); // 다른 사무실

        dispatchRepository.flush();

        // when
        List<Object[]> result = dispatchRepository.countByStatusAndOfficeId(officeId);

        // then
        assertThat(result).isNotEmpty();
        // officeId=1인 배차만 카운트되어야 함
    }

    @Test
    @DisplayName("Transporter와 Fetch Join으로 배차 목록 조회")
    void findAllWithTransporter() {
        // given
        Dispatch dispatchWithTransporter = Dispatch.builder()
                .dispatchNumber("2024-0013")
                .startLocation("서울시 강남구")
                .startLatitude(37.5000)
                .startLongitude(127.0400)
                .destinationLocation("서울시 서초구")
                .destinationLatitude(37.4800)
                .destinationLongitude(127.0300)
                .charge(50000)
                .status(StatusType.ASSIGNED)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .active(true)
                .officeId(1L)
                .transporter(testTransporter)
                .build();
        dispatchRepository.save(dispatchWithTransporter);

        // when
        Page<Dispatch> result = dispatchRepository.findAllWithTransporter(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).isNotEmpty();
        // Fetch Join이 되어 있으므로 N+1 문제 없이 Transporter 조회 가능
        assertThat(result.getContent().stream()
                .filter(d -> d.getTransporter() != null)
                .findFirst()).isPresent();
    }

    @Test
    @DisplayName("특정 기사의 특정 상태 배차 조회")
    void findByTransporterIdAndStatus() {
        // given
        Dispatch assignedDispatch = Dispatch.builder()
                .dispatchNumber("2024-0014")
                .startLocation("서울시")
                .startLatitude(37.5000)
                .startLongitude(127.0400)
                .destinationLocation("부산시")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(200000)
                .status(StatusType.ASSIGNED)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .active(true)
                .officeId(1L)
                .transporter(testTransporter)
                .assignedAt(LocalDateTime.now())
                .build();
        dispatchRepository.save(assignedDispatch);

        // when
        Optional<Dispatch> found = dispatchRepository.findByTransporterIdAndStatus(
                testTransporter.getId(), StatusType.ASSIGNED
        );

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTransporter().getId()).isEqualTo(testTransporter.getId());
        assertThat(found.get().getStatus()).isEqualTo(StatusType.ASSIGNED);
    }

    @Test
    @DisplayName("특정 기사의 최신 배차 조회 (assignedAt 최신순)")
    void findFirstByTransporterIdAndStatusOrderByAssignedAtDesc() {
        // given
        // 같은 기사에게 여러 배차 할당 (시간차)
        Dispatch oldDispatch = createDispatchWithTransporter(
                "2024-0015", testTransporter, LocalDateTime.now().minusHours(2)
        );
        Dispatch newDispatch = createDispatchWithTransporter(
                "2024-0016", testTransporter, LocalDateTime.now()
        );

        dispatchRepository.saveAll(List.of(oldDispatch, newDispatch));

        // when
        Optional<Dispatch> found = dispatchRepository.findFirstByTransporterIdAndStatusOrderByAssignedAtDesc(
                testTransporter.getId(), StatusType.ASSIGNED
        );

        // then
        assertThat(found).isPresent();
        // 가장 최근 배차가 조회되어야 함
        assertThat(found.get().getDispatchNumber()).isEqualTo("2024-0016");
    }

    @Test
    @DisplayName("존재하지 않는 배차 조회 시 빈 Optional 반환")
    void findByIdNotFound() {
        // when
        Optional<Dispatch> found = dispatchRepository.findById(99999L);

        // then
        assertThat(found).isEmpty();
    }

    // === 헬퍼 메서드 ===

    private Dispatch createDispatch(String dispatchNumber, double lat, double lon, StatusType status) {
        return Dispatch.builder()
                .dispatchNumber(dispatchNumber)
                .startLocation("서울시")
                .startLatitude(lat)
                .startLongitude(lon)
                .destinationLocation("부산시")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(100000)
                .status(status)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .active(true)
                .officeId(1L)
                .build();
    }

    private Dispatch createDispatchWithOffice(String dispatchNumber, Long officeId, StatusType status) {
        return dispatchRepository.save(Dispatch.builder()
                .dispatchNumber(dispatchNumber)
                .startLocation("서울시")
                .startLatitude(37.5000)
                .startLongitude(127.0400)
                .destinationLocation("부산시")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(100000)
                .status(status)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .active(true)
                .officeId(officeId)
                .build());
    }

    private Dispatch createDispatchWithTransporter(String dispatchNumber, Transporter transporter, LocalDateTime assignedAt) {
        return Dispatch.builder()
                .dispatchNumber(dispatchNumber)
                .startLocation("서울시")
                .startLatitude(37.5000)
                .startLongitude(127.0400)
                .destinationLocation("부산시")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(100000)
                .status(StatusType.ASSIGNED)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .active(true)
                .officeId(1L)
                .transporter(transporter)
                .assignedAt(assignedAt)
                .build();
    }
}
