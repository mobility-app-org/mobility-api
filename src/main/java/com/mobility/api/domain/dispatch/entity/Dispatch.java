package com.mobility.api.domain.dispatch.entity;

import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.global.entity.BaseEntity;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Slf4j
public class Dispatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dispatchNumber; // 배차 번호 (예: 2024-0001)

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

    // Tags (경유 여부, 결제 방식, 톨비 방식)
    @Enumerated(EnumType.STRING)
    private ViaType viaType; // 경유 여부 (경유 / null)

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType; // 결제 방식 (현금 / 후불 / 완후)

    @Enumerated(EnumType.STRING)
    private TollType tollType; // 톨비 방식 (톨포 / 톨별 / 하이패스)

    // FIXME office_id : 사무실 id :: 외래키 설정 필요
    @Column(name = "office_id")
    private Long officeId;
//    private Office office;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    private String memo; // 메모

    private LocalDateTime assignedAt; // 배차 할당 시간

    private LocalDateTime completedAt; // 완료 시간

    private LocalDateTime canceledAt; // 취소 시간

    @Column(length = 500)
    private String cancelReason; // 취소 사유 (최대 200자)

    // 기사 배차 시
    public void assignDispatch(Transporter transporter) {

        // 1. 유효성 검증 : HOLD 또는 OPEN 상태에서만 배차 가능
        if (this.status != StatusType.OPEN && this.status != StatusType.HOLD) {
            throw new GlobalException(ResultCode.DISPATCH_NOT_OPEN);
        }

        this.transporter = transporter;
        this.status = StatusType.ASSIGNED;
        this.assignedAt = LocalDateTime.now();
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
        this.completedAt = LocalDateTime.now();
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

    /**
     * 노출 범위 토글 (자사 <-> 통합)
     * @return 변경된 상태값
     */
    public CallType toggleExposure() {
        if (this.call == CallType.INTERNAL) {
            this.call = CallType.INTEGRATED;
        } else {
            this.call = CallType.INTERNAL;
        }
        return this.call;
    }

}
