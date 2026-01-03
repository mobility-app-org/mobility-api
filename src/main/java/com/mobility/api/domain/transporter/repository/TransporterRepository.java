package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import com.mobility.api.domain.transporter.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    Optional<Transporter> findByPhone(String username);

}
