package com.mobility.api.domain.dispatch.entity;

import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String startLocation; // 출발지
    private String destinationLocation; // 도착지
    private Integer charge; // 요금
    private String clientPhoneNumber; // 고객 전화번호

    @Enumerated(EnumType.STRING)
    private StatusType status; // 배차 상태

    @Enumerated(EnumType.STRING)
    private CallType call; // 콜 타입

    @Enumerated(EnumType.STRING)
    private ServiceType service; // 탁송 / 대리

    private Boolean active; // 활성화 여부 :: 임시저장 등에 사용

    // FIXME office_id : 사무실 id :: 외래키 설정 필요
    private Long officeId;

    // TODO transporter_id : 기사 id

    private LocalDateTime createdAt; // 생성일자

}
