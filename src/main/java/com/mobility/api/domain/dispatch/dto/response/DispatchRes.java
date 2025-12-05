package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 배차 등록 res
 */

@Builder
public record DispatchRes(Long id,
                          String startLocation,
                          Double startLatitude,
                          Double startLongitude,
                          String destinationLocation,
                          Double destinationLatitude,
                          Double destinationLongitude,
                          Integer charge,
                          String clientPhoneNumber,
                          StatusType status,
                          CallType callType,
                          ServiceType serviceType,
                          Long officeId,
                          Long transporterId,      // 기사 ID (null 가능)
                          String transporterName,  // 기사 이름 (null 가능)
                          LocalDateTime createdAt
) {
    // Entity -> DTO 변환 메서드 (거리는 별도 계산 없을 시 null)
    public static DispatchRes from(Dispatch dispatch) {
        return from(dispatch, null);
    }

    // Entity + 거리 -> DTO 변환 메서드
    public static DispatchRes from(Dispatch dispatch, Double distance) {
        return DispatchRes.builder()
                .id(dispatch.getId())
                .startLocation(dispatch.getStartLocation())
                .startLatitude(dispatch.getStartLatitude())
                .startLongitude(dispatch.getStartLongitude())
                .destinationLocation(dispatch.getDestinationLocation())
                .destinationLatitude(dispatch.getDestinationLatitude())
                .destinationLongitude(dispatch.getDestinationLongitude())
                .charge(dispatch.getCharge())
                .clientPhoneNumber(dispatch.getClientPhoneNumber())
                .status(dispatch.getStatus())
                .callType(dispatch.getCall())
                .serviceType(dispatch.getService())
                .officeId(dispatch.getOfficeId())
                // Transporter가 lazy loading이거나 null일 수 있으므로 체크 필요
                .transporterId(dispatch.getTransporter() != null ? dispatch.getTransporter().getId() : null)
                .transporterName(dispatch.getTransporter() != null ? dispatch.getTransporter().getName() : null)
                .createdAt(dispatch.getCreatedAt())
                .build();
    }
}