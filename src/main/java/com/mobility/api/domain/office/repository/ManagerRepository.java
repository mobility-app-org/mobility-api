package com.mobility.api.domain.office.repository;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

    Optional<Manager> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    // 사무실 직원 목록 조회 (페이징)
    Page<Manager> findAllByOffice(Office office, Pageable pageable);
}
