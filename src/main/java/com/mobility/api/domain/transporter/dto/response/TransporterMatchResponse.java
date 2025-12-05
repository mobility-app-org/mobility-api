package com.mobility.api.domain.transporter.dto.response;

import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import lombok.Builder;

/**
 * Projection의 미터 값을 받아 km로 변환하여 프론트에 전달
 */

@Builder
public record TransporterMatchResponse(
        Long transporterId,
        String name,
        String phone,
        Double distanceKm // km 단위
) {
    public static TransporterMatchResponse from(TransporterDistanceProjection proj) {
        // 미터(m) -> 킬로미터(km) 변환
        // 값이 없으면 0.0 처리
        double meters = proj.getDistanceInMeters() != null ? proj.getDistanceInMeters() : 0.0;

        // 1000으로 나누고, 소수점 둘째 자리까지 반올림 (예: 1.25 km)
        double km = Math.round((meters / 1000.0) * 100.0) / 100.0;

        return TransporterMatchResponse.builder()
                .transporterId(proj.getId())
                .name(proj.getName())
                .phone(proj.getPhone())
                .distanceKm(km)
                .build();
    }
}