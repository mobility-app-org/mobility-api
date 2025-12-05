package com.mobility.api.domain.dispatch.entity;

import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String startLocation; // 출발지

    private double startLatitude; // 출발지 위도
    private double startLongitude; // 출발지 경도

    private String destinationLocation; // 도착지

    private double destinationLatitude; // 도착지 위도
    private double destinationLongitude; // 도착지 경도

    private Integer charge; // 요금
    private String clientPhoneNumber; // 고객 전화번호

    @Enumerated(EnumType.STRING)
    private StatusType status; // 배차 상태

    @Column(name = "call_type")
    @Enumerated(EnumType.STRING)
    private CallType call; // 콜 타입

    @Enumerated(EnumType.STRING)
    private ServiceType service; // 탁송 / 대리

    private Boolean active; // 활성화 여부 :: 임시저장 등에 사용

    // FIXME office_id : 사무실 id :: 외래키 설정 필요
    @Column(name = "office_id")
    private Long officeId;
//    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    private LocalDateTime createdAt; // 생성일자

    // 기사 배차 시
    public void assignDispatch(Transporter transporter) {

        // 1. 유효성 검증 : 이미 배차되어있는지 확인
        if (this.status != StatusType.OPEN) {
            throw new GlobalException(ResultCode.DISPATCH_NOT_OPEN);
        }

        this.transporter = transporter;
        this.status = StatusType.ASSIGNED;
    }

    public void cancelDispatch(Transporter transporter) {
        validateOwner(transporter);

        if (this.status != StatusType.ASSIGNED) {
            throw new GlobalException(ResultCode.CANNOT_CANCEL_DISPATCH);
        }

        this.transporter = null;
        this.status = StatusType.OPEN;
    }

    public void completeDispatch(Transporter transporter) {
        validateOwner(transporter);

        if (this.status != StatusType.ASSIGNED) {
            throw new GlobalException(ResultCode.CANNOT_COMPLETE_DISPATCH);
        }

        this.status = StatusType.COMPLETED;
    }

    private void validateOwner(Transporter transporter) {
        if (this.transporter == null) {
            log.info(">>> [Error] validateOwner 실패: DB에는 있다는데 앱에서는 transporter가 NULL입니다!");
            throw new GlobalException(ResultCode.DISPATCH_NOT_ASSIGNED);
        }
//        if (!this.transporter.getId().equals(transporter.getId())) {
//            throw new GlobalException(ResultCode.FORBIDDEN);
//        }
        // 2. ID 비교 값 직접 출력
        Long assignedId = this.transporter.getId();
        Long requestId = transporter.getId();

        log.info(">>> [Check] 검증 시작");
        log.info(">>> 배차의 주인 ID (DB): " + assignedId);
        log.info(">>> 요청자 ID (Header): " + requestId);

        if (!assignedId.equals(requestId)) {
            log.info(">>> [Error] ID 불일치! 403 예외 발생");
            throw new GlobalException(ResultCode.FORBIDDEN);
        }
    }
}
