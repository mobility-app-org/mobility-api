package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.transporter.TransporterStatus;
import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import com.mobility.api.domain.transporter.entity.Transporter;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {

    /**
     * PostGIS의 ST_DistanceSphere를 사용해 미터 단위 거리를 계산
     * 특정 좌표(lat, lon) 기준, 반경(radiusMeters) 내의 기사를 거리순 조회
     */
    @Query(value = """
        SELECT t.transporter_id as id,
               t.name,
               t.phone,
               ST_DistanceSphere(t.current_location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) as distanceInMeters
        FROM transporters t
        WHERE t.current_location IS NOT NULL
          -- PostGIS 반경 검색 함수 (인덱스 활용)
          AND ST_DWithin(t.current_location::geography, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusMeters)
        ORDER BY distanceInMeters ASC
        """, nativeQuery = true)
    List<TransporterDistanceProjection> findNearbyTransporters(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("radiusMeters") double radiusMeters
    );

    /**
     * 자동 배차용 적격 기사 조회
     * - 50km 반경 내 기사
     * - is_auto_dispatch = true (자동 배차 수신 동의한 기사만)
     * - 거리순 정렬
     * - 최대 10명
     */
    @Query(value = """
        SELECT t.transporter_id as id,
               t.name,
               t.phone,
               ST_DistanceSphere(t.current_location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) as distanceInMeters
        FROM transporters t
        WHERE t.current_location IS NOT NULL
          AND t.is_auto_dispatch = true
          AND ST_DWithin(t.current_location::geography,
                         ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                         50000)
        ORDER BY distanceInMeters ASC
        LIMIT 10
        """, nativeQuery = true)
    List<TransporterDistanceProjection> findEligibleDriversForAutoDispatch(
            @Param("lat") double lat,
            @Param("lon") double lon
    );

    /**
     * 1km 반경 내 자동배차 ON 기사 존재 여부 확인
     */
    @Query(value = """
        SELECT COUNT(*) > 0
        FROM transporters t
        WHERE t.current_location IS NOT NULL
          AND t.is_auto_dispatch = true
          AND ST_DWithin(t.current_location::geography,
                         ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                         1000)
        """, nativeQuery = true)
    boolean existsEligibleDriversWithinRadius(
            @Param("lat") double lat,
            @Param("lon") double lon
    );

    // 전화번호 중복 가입 체크용
    boolean existsByPhone(String phoneNumber);

    // 로그인 시 기사 조회용 (기사 id는 전화번호이므로 username을 받아서 phone을 조회함)
    Optional<Transporter> findByPhone(String username);

    // 특정 사무실에 소속된 기사 목록 조회
    List<Transporter> findAllByOffice(Office office);

    // 1. 상태 필터 없이 전체 조회 (페이징)
    Page<Transporter> findAllByOffice(Office office, Pageable pageable);

    // 2. 상태 필터 적용 조회 (페이징)
    Page<Transporter> findAllByOfficeAndStatus(Office office, TransporterStatus status, Pageable pageable);

    /**
     * 비관적 락을 사용한 기사 조회 (동시성 제어)
     * 배차 할당 시 동시에 여러 배차가 같은 기사에게 할당되는 것을 방지
     * @param transporterId 기사 ID
     * @return 기사 정보
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT t FROM Transporter t WHERE t.id = :transporterId")
    Optional<Transporter> findByIdWithPessimisticLock(@Param("transporterId") Long transporterId);

}
