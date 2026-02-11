package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.audit.dto.request.AuditLogSearchCond;
import com.mobility.api.domain.audit.dto.response.AuditLogRes;
import com.mobility.api.domain.audit.service.AuditService;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사무실 감사 로그", description = "사무실 관리자용 감사 로그 API")
@RestController
@RequestMapping("/api/v1/office/audit") // 사무실 전용 URL
@RequiredArgsConstructor
public class OfficeAuditV1Controller {

    private final AuditService auditService;

    @Operation(summary = "사무실 로그 목록 조회")
    @GetMapping
    public CommonResponse<Page<AuditLogRes>> getAuditLogs(
            @AuthenticationPrincipal PrincipalDetails user, // 로그인한 관리자 정보 필요
            @ModelAttribute AuditLogSearchCond condition,
            Pageable pageable
    ) {
        // ★ 핵심: Service에 넘길 때 '사무실 ID' 같은 제약 조건을 추가해서 넘깁니다.
        // 현재는 AuditLog에 officeId 컬럼이 없어서 구현하지 않았지만,
        // 나중엔 user.getManager().getOffice().getId()를 조건에 넣어줘야
        // 남의 사무실 로그를 못 보게 막을 수 있습니다.

        return CommonResponse.success(auditService.getAuditLogs(condition, pageable));
    }

}
