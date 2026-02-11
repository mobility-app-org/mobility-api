package com.mobility.api.domain.audit.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(name = "audit_log_change")
public class AuditLogChange {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String field;      // 변경된 필드명 (charge)
    private String fieldLabel; // 필드 설명 (요금)
    private String oldValue;   // 변경 전
    private String newValue;   // 변경 후

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "audit_log_id")
    private AuditLog auditLog;
}