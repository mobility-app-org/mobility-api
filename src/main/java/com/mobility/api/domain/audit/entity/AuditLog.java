package com.mobility.api.domain.audit.entity;

import com.mobility.api.domain.audit.enums.AuditAction;
import com.mobility.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "audit_log")
public class AuditLog extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AuditAction action;       // 액션 타입

    private String resourceType;      // 대상 (dispatch, manager...)
    private Long resourceId;          // 대상 PK
    private String resourceIdentifier;// 식별자 (2024-0001 등)

    private String userId;            // 수행자 ID (staff-001)
    private String userName;          // 수행자 이름 (김규원) - 당시 이름 스냅샷
    private String ipAddress;         // IP 주소

    // 변경 내역 (1:N 관계)
    @OneToMany(mappedBy = "auditLog", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AuditLogChange> changes = new ArrayList<>();

    // 생성자, 연관관계 편의 메서드 등 생략 (필요시 추가)
}