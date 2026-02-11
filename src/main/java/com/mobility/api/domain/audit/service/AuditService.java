package com.mobility.api.domain.audit.service;

import com.mobility.api.domain.audit.dto.request.AuditLogSearchCond;
import com.mobility.api.domain.audit.dto.response.AuditLogRes;
import com.mobility.api.domain.audit.entity.AuditLog;
import com.mobility.api.domain.audit.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogRes> getAuditLogs(AuditLogSearchCond cond, Pageable pageable) {
        // 동적 쿼리 생성
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. 날짜 필터 (createdAt 기준)
            if (cond.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), cond.startDate().atStartOfDay()));
            }
            if (cond.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), cond.endDate().atTime(23, 59, 59)));
            }

            // 2. Action 필터
            if (cond.action() != null) {
                predicates.add(cb.equal(root.get("action"), cond.action()));
            }

            // 3. userId 필터
            if (StringUtils.hasText(cond.userId())) {
                predicates.add(cb.equal(root.get("userId"), cond.userId()));
            }

            // 4. 검색어 (resourceIdentifier OR userName)
            if (StringUtils.hasText(cond.search())) {
                String likePattern = "%" + cond.search() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("resourceIdentifier"), likePattern),
                        cb.like(root.get("userName"), likePattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 조회 및 변환
        return auditLogRepository.findAll(spec, pageable)
                .map(AuditLogRes::from);
    }
}