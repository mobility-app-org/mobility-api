package com.mobility.api.domain.audit.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditAction {
    DISPATCH_CREATED("배차 생성"),
    DISPATCH_UPDATED("배차 수정"),
    DISPATCH_CANCELED("배차 취소"),
    EXPOSURE_CHANGED("노출 상태 변경");

    private final String description;
}
