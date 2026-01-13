package com.mobility.api.domain.transporter.dto.response;

import com.mobility.api.domain.transporter.entity.Transporter;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record TransporterRes(
        Long transporterId,
        String name,
        String phoneNumber,
        Boolean isAutoDispatch,
        LocalDateTime createdAt
) {
    // Entity -> DTO 변환 메서드
    public static TransporterRes from(Transporter transporter) {
        return TransporterRes.builder()
                .transporterId(transporter.getId())
                .name(transporter.getName())
                .phoneNumber(transporter.getPhone())
                .isAutoDispatch(transporter.isAutoDispatch())
                .createdAt(transporter.getCreatedAt())
                .build();
    }
}