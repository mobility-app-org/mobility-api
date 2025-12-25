package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.entity.DispatchOffer;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DispatchOfferRepository extends JpaRepository<DispatchOffer, Long> {

    /**
     * 특정 배차에 대한 모든 제안 조회 (순서대로)
     * - 배차 이력 확인용
     */
    List<DispatchOffer> findByDispatchIdOrderBySequenceAsc(Long dispatchId);

    /**
     * 제안 조회 with 비관적 락
     * - 동시성 제어: 여러 기사가 동시에 응답하는 것을 방지
     * - 3초 타임아웃
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT o FROM DispatchOffer o WHERE o.id = :offerId")
    Optional<DispatchOffer> findByIdWithLock(@Param("offerId") Long offerId);

    /**
     * 기사의 PENDING 상태 제안 개수 조회
     * - 중복 알림 방지용 (필요 시 사용)
     */
    @Query("SELECT COUNT(o) FROM DispatchOffer o WHERE o.transporter.id = :transporterId AND o.status = 'PENDING'")
    int countPendingOffersByTransporter(@Param("transporterId") Long transporterId);
}
