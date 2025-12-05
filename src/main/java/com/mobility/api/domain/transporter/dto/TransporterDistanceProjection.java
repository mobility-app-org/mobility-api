package com.mobility.api.domain.transporter.dto;

/**
 * Native Query 결과를 매핑할 인터페이스
 */

public interface TransporterDistanceProjection {
    Long getId();
    String getName();
    String getPhone();
    Double getDistanceInMeters(); // DB에서 계산된 미터 단위 거리
}
