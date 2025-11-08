package com.mobility.api.domain.office.dto.request;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateDispatchReq(

        @NotBlank(message = "출발지가 입력되지 않았습니다.")
        String startLocation,       // 출발지

        @NotBlank(message = "도착지가 입력되지 않았습니다.")
        String destinationLocation, // 도착지

        @NotNull(message = "요금이 입력되지 않았습니다.")
        Integer charge,             // 요금

        @NotBlank(message = "고객 전화번호가 입력되지 않았습니다.")
        String clientPhoneNumber,   // 고객 전화번호

//        StatusType status,        // 배차 상태
        Long officeId,              // FIXME 사무실 id, 토큰 등에서 받아오도록 변경해야 함
//        CallType call,            // 콜 타입
        Boolean active,             // 활성화 여부
        ServiceType service,        // 탁송 / 대리

        LocalDateTime createdAt     // 생성 시간
) {
    public CreateDispatchReq {
        officeId = 1L; // FIXME 임시로 사무실 id는 1로 고정
    }

    public Dispatch toEntity() {
        return Dispatch.builder()
                .startLocation(this.startLocation())
                .destinationLocation(this.destinationLocation())
                .charge(this.charge())
                .clientPhoneNumber(this.clientPhoneNumber())
                .status(StatusType.OPEN)
                .officeId(this.officeId())
                .call(CallType.INTERNAL)
                .active(this.active())
                .service(this.service())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
