package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long>,
        JpaSpecificationExecutor<Dispatch> { // 정렬, 필터 등을 위해 추가

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")}) // 3初
    @Query("SELECT d FROM Dispatch d WHERE d.id = :dispatchId")
    Optional<Dispatch> findByIdWithPessimisticLock(@Param("dispatchId") Long dispatchId);
}
