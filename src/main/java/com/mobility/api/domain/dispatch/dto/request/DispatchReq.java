package com.mobility.api.domain.dispatch.dto.request;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record DispatchReq(
        @NotBlank(message = "출발지는 필수입니다.")
        String startLocation,

        @NotNull(message = "출발지 위도는 필수입니다.")
        Double startLatitude,

        @NotNull(message = "출발지 경도는 필수입니다.")
        Double startLongitude,

        @NotBlank(message = "도착지는 필수입니다.")
        String destinationLocation,

        @NotNull(message = "도착지 위도는 필수입니다.")
        Double destinationLatitude,

        @NotNull(message = "도착지 경도는 필수입니다.")
        Double destinationLongitude,

        @NotNull(message = "요금은 필수입니다.")
        Integer charge,

        @NotBlank(message = "고객 전화번호는 필수입니다.")
        String clientPhoneNumber,

        @NotNull(message = "콜 타입은 필수입니다.")
        CallType callType,

        @NotNull(message = "서비스 타입은 필수입니다.")
        ServiceType serviceType,

        Long officeId
) {
    // DTO -> Entity 변환 메서드
    public Dispatch toEntity() {
        return Dispatch.builder()
                .startLocation(startLocation)
                .startLatitude(startLatitude)
                .startLongitude(startLongitude)
                .destinationLocation(destinationLocation)
                .destinationLatitude(destinationLatitude)
                .destinationLongitude(destinationLongitude)
                .charge(charge)
                .clientPhoneNumber(clientPhoneNumber)
                .call(callType)
                .service(serviceType)
                .officeId(officeId)
                .status(StatusType.OPEN) // 생성 시 기본 상태는 OPEN
                .active(true)            // 생성 시 기본 활성화
                .build();
    }
}