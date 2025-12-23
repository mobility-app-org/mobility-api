package com.mobility.api.domain.dispatch.dto;

/**
 * Native Query 결과를 매핑할 인터페이스
 * 기사의 현재 위치 기준 배차 리스트 조회 시 사용
 */
public interface DispatchDistanceProjection {
    Long getId();
    String getServiceType();
    Integer getCharge();
    String getStartLocation();
    String getDestinationLocation();
    String getStatus();
    Double getDistanceInMeters(); // DB에서 계산된 미터 단위 거리
    String getViaType();
    String getPaymentType();
    String getTollType();
}