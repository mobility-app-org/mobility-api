package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.dto.DispatchDistanceProjection;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
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
          AND (:statuses IS NULL OR d.status IN (:statuses))
        ORDER BY distanceInMeters ASC
        """, nativeQuery = true)
    List<DispatchDistanceProjection> findDispatchesByDistance(
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
}
