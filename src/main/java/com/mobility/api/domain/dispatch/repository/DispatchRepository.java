package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.dto.DispatchDistanceProjection;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.mobility.api.domain.dispatch.enums.StatusType;

import java.util.List;
import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long>,
        JpaSpecificationExecutor<Dispatch> { // 정렬, 필터 등을 위해 추가

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")}) // 3초
    @Query("SELECT d FROM Dispatch d WHERE d.id = :dispatchId")
    Optional<Dispatch> findByIdWithPessimisticLock(@Param("dispatchId") Long dispatchId);

    /**
     * PostGIS의 ST_DistanceSphere를 사용해 미터 단위 거리를 계산
     * 기사의 현재 위치(lat, lon) 기준, 전체 배차를 거리순으로 조회
     * @param lat 기사의 현재 위도
     * @param lon 기사의 현재 경도
     * @param statuses 필터링할 배차 상태 목록 (빈 리스트면 전체 조회)
     * @return 거리순으로 정렬된 배차 리스트
     */
    @Query(value = """
        SELECT d.id as id,
               d.service as serviceType,
               d.charge as charge,
               d.start_location as startLocation,
               d.destination_location as destinationLocation,
               d.status as status,
               ST_DistanceSphere(
                   ST_SetSRID(ST_MakePoint(d.start_longitude, d.start_latitude), 4326),
                   ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
               ) as distanceInMeters,
               d.via_type as viaType,
               d.payment_type as paymentType,
               d.toll_type as tollType
        FROM dispatch d
        WHERE d.active = true
        ORDER BY distanceInMeters ASC
        """, nativeQuery = true)
    List<DispatchDistanceProjection> findDispatchesByDistance(@Param("lat") double lat, @Param("lon") double lon);

    @Query(value = """
        SELECT d.id as id,
               d.service as serviceType,
               d.charge as charge,
               d.start_location as startLocation,
               d.destination_location as destinationLocation,
               d.status as status,
               ST_DistanceSphere(
                   ST_SetSRID(ST_MakePoint(d.start_longitude, d.start_latitude), 4326),
                   ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
               ) as distanceInMeters,
               d.via_type as viaType,
               d.payment_type as paymentType,
               d.toll_type as tollType
        FROM dispatch d
        WHERE d.active = true
          AND d.status IN (:statuses)
        ORDER BY distanceInMeters ASC
        """, nativeQuery = true)
    List<DispatchDistanceProjection> findDispatchesByDistanceAndStatus(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("statuses") List<String> statuses
    );

    /**
     * 상태별 배차 카운트 조회
     */
    @Query("SELECT d.status, COUNT(d) FROM Dispatch d GROUP BY d.status")
    List<Object[]> countByStatus();

    /**
     * 특정 사무실의 상태별 배차 카운트 조회
     */
    @Query("SELECT d.status, COUNT(d) FROM Dispatch d WHERE d.officeId = :officeId GROUP BY d.status")
    List<Object[]> countByStatusAndOfficeId(@Param("officeId") Long officeId);

    /**
     * 배차 목록 조회 (Transporter와 Fetch Join으로 N+1 문제 해결)
     * @param pageable 페이징 및 정렬 정보
     * @return 배차 목록 (Transporter 포함)
     */
    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.transporter")
    Page<Dispatch> findAllWithTransporter(Pageable pageable);

    /**
     * 특정 기사의 특정 상태 배차 조회
     * @param transporterId 기사 ID
     * @param status 배차 상태
     * @return 배차 정보
     */
    Optional<Dispatch> findByTransporterIdAndStatus(Long transporterId, StatusType status);

    /**
     * 특정 기사의 특정 상태 배차 조회 (최신순)
     * 여러 건이 있을 경우 가장 최근에 할당된 배차 반환
     * @param transporterId 기사 ID
     * @param status 배차 상태
     * @return 가장 최근 배차 정보
     */
    @Query("SELECT d FROM Dispatch d WHERE d.transporter.id = :transporterId AND d.status = :status ORDER BY d.assignedAt DESC LIMIT 1")
    Optional<Dispatch> findFirstByTransporterIdAndStatusOrderByAssignedAtDesc(
            @Param("transporterId") Long transporterId,
            @Param("status") StatusType status
    );

    /**
     * 특정 기사의 기간별 완료된 배차 조회
     * @param transporterId 기사 ID
     * @param fromDate 시작일 (00:00:00)
     * @param toDate 종료일 (23:59:59)
     * @return 완료된 배차 리스트 (assignedAt 최신순)
     */
    @Query("""
            SELECT d FROM Dispatch d
            WHERE d.transporter.id = :transporterId
            AND d.status = 'COMPLETED'
            AND d.completedAt >= :fromDate
            AND d.completedAt <= :toDate
            ORDER BY d.assignedAt DESC
            """)
    List<Dispatch> findCompletedDispatchesByTransporterIdAndDateRange(
            @Param("transporterId") Long transporterId,
            @Param("fromDate") java.time.LocalDateTime fromDate,
            @Param("toDate") java.time.LocalDateTime toDate
    );

    /**
     * 배차 상세 조회 (Transporter와 Fetch Join)
     * @param dispatchId 배차 ID
     * @return 배차 정보 (Transporter 포함)
     */
    @Query("SELECT d FROM Dispatch d LEFT JOIN FETCH d.transporter WHERE d.id = :dispatchId")
    Optional<Dispatch> findByIdWithTransporter(@Param("dispatchId") Long dispatchId);
}
