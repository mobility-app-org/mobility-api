package com.mobility.api.domain.transporter;

public enum TransporterStatus {
    PENDING,   // 승인 대기 (심사 중)
    ACTIVE,    // 승인 완료 (활동 중)
    INACTIVE,  // 비활성화 (휴면, 잠금 등)
    REJECTED   // 승인 거절
}
