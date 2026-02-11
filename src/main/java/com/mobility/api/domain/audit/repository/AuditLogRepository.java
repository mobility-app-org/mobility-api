package com.mobility.api.domain.audit.repository;

import com.mobility.api.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    // N+1 문제 방지를 위해 changes까지 한 번에 가져오기 (EntityGraph)
    @Override
    @EntityGraph(attributePaths = {"changes"})
    Page<AuditLog> findAll(Specification<AuditLog> spec, Pageable pageable);
}