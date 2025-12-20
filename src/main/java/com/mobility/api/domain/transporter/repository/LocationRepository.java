package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.transporter.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LocationRepository extends JpaRepository<LocationHistory, Integer> {

    /**
     * 특정 기사의 가장 최근 위치 정보 조회
     * Spring Data JPA 메서드명 규칙: findFirst = 첫 번째 결과만, OrderBy = 정렬
     * @param transporterId 기사 ID
     * @return 가장 최근 LocationHistory
     */
    Optional<LocationHistory> findFirstByTransporter_IdOrderByIdDesc(Long transporterId);
}
