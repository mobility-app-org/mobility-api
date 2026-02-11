package com.mobility.api.domain.audit.dto.request;

import com.mobility.api.domain.audit.enums.AuditAction;
import java.time.LocalDate;

public record AuditLogSearchCond(
        LocalDate startDate, // 시작일
        LocalDate endDate,   // 종료일
        AuditAction action,  // 액션 필터
        String userId,       // 사용자 ID 필터
        String search        // 검색어 (배차번호, 사용자명)
) {}