package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.office.entity.Manager;
import lombok.Builder;

@Builder
public record ManagerRes(
        Long id,
        String loginId,
        String name,
        String phone,
        String email,
        String role, // "OWNER", "STAFF" 등 권한 정보
        String status,
        String createdAt
) {
    public static ManagerRes from(Manager manager) {
        return ManagerRes.builder()
                .id(manager.getId())
                .loginId(manager.getLoginId())
                .name(manager.getName())
                .phone(manager.getPhone())
                .email(manager.getEmail())
                .role(manager.getRole().name())
                .status(manager.getStatus().name())
                .createdAt(manager.getCreatedAt().toString())
                .build();
    }
}