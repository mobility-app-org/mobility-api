package com.mobility.api.domain.dispatch.entity;

import com.mobility.api.domain.dispatch.enums.OfferStatus;
import com.mobility.api.domain.transporter.entity.Transporter;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 배차 제안 엔티티
 * - 배차가 등록되면 거리순으로 기사에게 순차적으로 제안
 * - 각 제안의 이력을 저장하여 통계 및 분석에 활용
 */
@Entity
@Table(name = "dispatch_offer")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id", nullable = false)
    private Dispatch dispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id", nullable = false)
    private Transporter transporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OfferStatus status;

    @Column(nullable = false)
    private Integer sequence;  // 순서 (1~10)

    @Column(nullable = false)
    private LocalDateTime offeredAt;  // 제안 시각

    @Column
    private LocalDateTime respondedAt;  // 응답 시각 (수락/거절/타임아웃)

    @PrePersist
    protected void onCreate() {
        if (offeredAt == null) {
            offeredAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OfferStatus.PENDING;
        }
    }

    /**
     * 배차 수락 처리
     */
    public void accept() {
        this.status = OfferStatus.ACCEPTED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * 배차 거절 처리
     */
    public void reject() {
        this.status = OfferStatus.REJECTED;
        this.respondedAt = LocalDateTime.now();
    }

    /**
     * 타임아웃 처리 (5초 미응답)
     */
    public void timeout() {
        this.status = OfferStatus.TIMEOUT;
        this.respondedAt = LocalDateTime.now();
    }
}
