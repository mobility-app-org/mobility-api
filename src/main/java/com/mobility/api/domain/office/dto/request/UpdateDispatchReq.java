package com.mobility.api.domain.office.dto.request;

import com.mobility.api.domain.dispatch.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "배차 수정 request 객체")
public record UpdateDispatchReq(

        @Schema(description = "출발지", example = "서울 강남구")
        String startLocation,

        @Schema(description = "출발지 위도", example = "37.5547125")
        double startLatitude,

        @Schema(description = "출발지 경도", example = "37.5547125")
        double startLongitude,

        @Schema(description = "도착지", example = "경기 성남시")
        String destinationLocation,

        @Schema(description = "도착지 위도", example = "37.5547125")
        double destinationLatitude,

        @Schema(description = "도착지 경도", example = "37.5547125")
        double destinationLongitude,

        @Schema(description = "요금", example = "20000")
        Integer charge,

        @Schema(description = "고객 전화번호", example = "010-1234-5678")
        String clientPhoneNumber,

        @Schema(description = "배차 상태", example = "OPEN")
        StatusType status,

        @Schema(description = "콜 타입", example = "INTERNAL")
        CallType call,

        @Schema(description = "활성화 여부", example = "true")
        Boolean active,

        @Schema(description = "서비스 타입", example = "DELIVERY")
        ServiceType service,

        @Schema(description = "배차 번호", example = "2024-0001")
        String dispatchNumber,

        @Schema(description = "메모", example = "memo")
        String memo,

        @Schema(description = "결제 방식", example = "CASH")
        PaymentType paymentType,

        @Schema(description = "톨비 방식", example = "HIPASS")
        TollType tollType

        ) {
}