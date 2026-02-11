package com.mobility.api.domain.audit.dto.response;

import com.mobility.api.domain.audit.entity.AuditLog;
import com.mobility.api.domain.audit.entity.AuditLogChange;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record AuditLogRes(
        Long id,
        String action,
        String resourceType,
        Long resourceId,
        String resourceIdentifier,
        String userId,
        String userName,
        LocalDateTime createdAt,
        String ipAddress,
        List<ChangeRes> changes
) {
    public static AuditLogRes from(AuditLog log) {
        return AuditLogRes.builder()
                .id(log.getId())
                .action(log.getAction().name())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .resourceIdentifier(log.getResourceIdentifier())
                .userId(log.getUserId())
                .userName(log.getUserName())
                .createdAt(log.getCreatedAt())
                .ipAddress(log.getIpAddress())
                .changes(log.getChanges().stream().map(ChangeRes::from).toList())
                .build();
    }

    @Builder
    public record ChangeRes(
            String field,
            String fieldLabel,
            String oldValue,
            String newValue
    ) {
        public static ChangeRes from(AuditLogChange change) {
            return ChangeRes.builder()
                    .field(change.getField())
                    .fieldLabel(change.getFieldLabel())
                    .oldValue(change.getOldValue())
                    .newValue(change.getNewValue())
                    .build();
        }
    }
}