package com.mobility.api.domain.office.repository;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    Optional<Manager> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);
}
