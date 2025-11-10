package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.transporter.entity.Transporter;
import jakarta.persistence.*;

import java.time.LocalDateTime;

public record GetAllDispatchRes(
        String startLocation, // 출발지
        String destinationLocation, // 도착지
        Integer charge, // 요금
        String clientPhoneNumber, // 고객 전화번호
        StatusType status, // 배차 상태
        CallType call, // 콜 타입
        ServiceType service, // 탁송 / 대리
        Boolean active, // 활성화 여부 :: 임시저장 등에 사용
        Transporter transporter,
        LocalDateTime createdAt // 생성일자
) {
    public GetAllDispatchRes(Dispatch dispatch) {
        this(
                dispatch.getStartLocation(),
                dispatch.getDestinationLocation(),
                dispatch.getCharge(),
                dispatch.getClientPhoneNumber(),
                dispatch.getStatus(),
                dispatch.getCall(),
                dispatch.getService(),
                dispatch.getActive(),
                dispatch.getTransporter(),
                dispatch.getCreatedAt()
        );
    }
}
