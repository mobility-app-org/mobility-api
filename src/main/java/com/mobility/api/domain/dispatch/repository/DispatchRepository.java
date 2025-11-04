package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Dispatch d WHERE d.id = :dispatchId")
    Optional<Dispatch> findByIdWithPessimisticLock(@Param("dispatchId") Long dispatchId);

}
