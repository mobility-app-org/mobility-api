package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.dto.websocket.DispatchOfferNotification;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.entity.DispatchOffer;
import com.mobility.api.domain.dispatch.enums.OfferResult;
import com.mobility.api.domain.dispatch.enums.OfferStatus;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.repository.DispatchOfferRepository;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 자동 배차 알림 서비스
 * - 배차 등록 시 거리순으로 기사에게 순차적으로 알림 전송
 * - WebSocket (STOMP)를 통한 실시간 알림
 * - 5초 타임아웃 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AutoDispatchService {

    private final DispatchRepository dispatchRepository;
    private final DispatchOfferRepository offerRepository;
    private final TransporterRepository transporterRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Qualifier("dispatchTimeoutScheduler")
    private final ScheduledExecutorService scheduledExecutor;

    // offerId -> CompletableFuture 매핑 (응답 대기용)
    private final ConcurrentHashMap<Long, CompletableFuture<OfferResult>> offerFutureMap = new ConcurrentHashMap<>();

    /**
     * 순차 알림 전송 시작 (비동기)
     * - 거리순으로 최대 10명의 기사에게 순차적으로 알림
     * - 각 기사당 5초 응답 대기
     * - 수락 시 종료, 거절/타임아웃 시 다음 기사에게 전송
     *
     * @param dispatchId 배차 ID
     */
    @Async("autoDispatchExecutor")
    public void startSequentialNotification(Long dispatchId) {
        log.info("[AutoDispatch] 순차 알림 시작 - dispatchId: {}", dispatchId);

        try {
            // 1. 배차 정보 조회
            Dispatch dispatch = dispatchRepository.findById(dispatchId)
                    .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

            // 2. 적격 기사 조회 (isAutoDispatch = true, 거리순, 최대 10명)
            List<Transporter> eligibleDrivers = findEligibleDrivers(dispatch);

            if (eligibleDrivers.isEmpty()) {
                log.info("[AutoDispatch] 적격 기사 없음 - dispatchId: {}", dispatchId);
                return;
            }

            log.info("[AutoDispatch] 적격 기사 {}명 발견 - dispatchId: {}", eligibleDrivers.size(), dispatchId);

            // 3. 순차적으로 알림 전송
            for (int i = 0; i < eligibleDrivers.size(); i++) {
                Transporter driver = eligibleDrivers.get(i);

                // 배차 상태 재확인 (이미 할당되었을 수 있음)
                dispatch = dispatchRepository.findById(dispatchId)
                        .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

                if (dispatch.getStatus() != StatusType.HOLD) {
                    log.info("[AutoDispatch] 배차가 더 이상 HOLD 상태가 아님. 종료 - dispatchId: {}, status: {}",
                            dispatchId, dispatch.getStatus());
                    break;
                }

                // 3-1. DispatchOffer 생성 (별도 트랜잭션)
                DispatchOffer offer = createOfferInTransaction(dispatch, driver, i + 1);

                // 3-2. WebSocket 알림 전송
                Double distanceKm = calculateDistanceKm(dispatch, driver);
                DispatchOfferNotification notification = DispatchOfferNotification.from(offer, distanceKm);
                sendNotificationToDriver(driver.getId(), notification);

                log.info("[AutoDispatch] 알림 전송 완료 - offerId: {}, transporterId: {}, sequence: {}",
                        offer.getId(), driver.getId(), i + 1);

                // 3-3. 5초 대기 (응답 대기)
                CompletableFuture<OfferResult> future = waitForResponseWithTimeout(offer.getId());

                try {
                    OfferResult result = future.get(); // Blocking

                    if (result == OfferResult.ACCEPTED) {
                        log.info("[AutoDispatch] 기사가 수락함. 프로세스 종료 - dispatchId: {}, transporterId: {}",
                                dispatchId, driver.getId());
                        return; // 프로세스 종료
                    } else {
                        log.info("[AutoDispatch] 기사가 거절/타임아웃. 다음 기사에게 전송 - dispatchId: {}, result: {}",
                                dispatchId, result);
                        // continue to next driver
                    }

                } catch (Exception e) {
                    log.error("[AutoDispatch] 응답 대기 중 오류 발생 - offerId: {}", offer.getId(), e);
                    // continue to next driver
                }
            }

            // 4. 모든 기사가 거절/미응답 → HOLD에서 OPEN으로 변경
            updateDispatchStatusToOpen(dispatchId);
            log.info("[AutoDispatch] 모든 기사가 거절/미응답. 배차를 OPEN 상태로 변경 - dispatchId: {}", dispatchId);

        } catch (Exception e) {
            log.error("[AutoDispatch] 순차 알림 처리 중 오류 발생 - dispatchId: {}", dispatchId, e);
        }
    }

    /**
     * 배차 수락 처리
     *
     * @param offerId       제안 ID
     * @param transporterId 기사 ID
     */
    @Transactional
    public void handleAccept(Long offerId, Long transporterId) {
        log.info("[AutoDispatch] 수락 처리 시작 - offerId: {}, transporterId: {}", offerId, transporterId);

        // 1. Offer 조회 with Lock
        DispatchOffer offer = offerRepository.findByIdWithLock(offerId)
                .orElseThrow(() -> new GlobalException(ResultCode.OFFER_NOT_FOUND));

        // 2. 이미 응답한 제안인지 확인
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new GlobalException(ResultCode.OFFER_ALREADY_RESPONDED);
        }

        // 3. 기사 본인 확인
        if (!offer.getTransporter().getId().equals(transporterId)) {
            throw new GlobalException(ResultCode.FORBIDDEN);
        }

        // 4. 배차 조회 with Lock
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(offer.getDispatch().getId())
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        // 5. 배차 상태 확인 (HOLD 상태에서만 수락 가능)
        if (dispatch.getStatus() != StatusType.HOLD) {
            // Offer는 거절로 처리
            offer.reject();
            offerRepository.save(offer);
            throw new GlobalException(ResultCode.DISPATCH_ALREADY_ASSIGNED);
        }

        // 6. 배차 할당
        Transporter transporter = offer.getTransporter();
        dispatch.assignDispatch(transporter);
        dispatchRepository.save(dispatch);

        // 7. Offer 수락 처리
        offer.accept();
        offerRepository.save(offer);

        // 8. CompletableFuture 완료 (타임아웃 취소)
        completeOfferFuture(offerId, OfferResult.ACCEPTED);

        log.info("[AutoDispatch] 배차 할당 완료 - dispatchId: {}, transporterId: {}", dispatch.getId(), transporterId);
    }

    /**
     * 배차 거절 처리
     *
     * @param offerId       제안 ID
     * @param transporterId 기사 ID
     */
    @Transactional
    public void handleReject(Long offerId, Long transporterId) {
        log.info("[AutoDispatch] 거절 처리 시작 - offerId: {}, transporterId: {}", offerId, transporterId);

        // 1. Offer 조회 with Lock
        DispatchOffer offer = offerRepository.findByIdWithLock(offerId)
                .orElseThrow(() -> new GlobalException(ResultCode.OFFER_NOT_FOUND));

        // 2. 이미 응답한 제안인지 확인
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new GlobalException(ResultCode.OFFER_ALREADY_RESPONDED);
        }

        // 3. 기사 본인 확인
        if (!offer.getTransporter().getId().equals(transporterId)) {
            throw new GlobalException(ResultCode.FORBIDDEN);
        }

        // 4. Offer 거절 처리
        offer.reject();
        offerRepository.save(offer);

        // 5. CompletableFuture 완료
        completeOfferFuture(offerId, OfferResult.REJECTED);

        log.info("[AutoDispatch] 거절 처리 완료 - offerId: {}", offerId);
    }

    /**
     * 적격 기사 조회
     * - isAutoDispatch = true
     * - 거리순 정렬
     * - 최대 10명
     */
    @Transactional(readOnly = true)
    protected List<Transporter> findEligibleDrivers(Dispatch dispatch) {
        // 1. PostGIS 기반 거리순 조회 (50km 반경, 최대 10명)
        List<TransporterDistanceProjection> projections = transporterRepository.findEligibleDriversForAutoDispatch(
                dispatch.getStartLatitude(),
                dispatch.getStartLongitude()
        );

        // 2. Projection -> Entity 변환 (순서 유지)
        List<Long> transporterIds = projections.stream()
                .map(TransporterDistanceProjection::getId)
                .collect(Collectors.toList());

        if (transporterIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Entity 조회 (순서 유지)
        Map<Long, Transporter> transporterMap = transporterRepository
                .findAllById(transporterIds).stream()
                .collect(Collectors.toMap(Transporter::getId, t -> t));

        return transporterIds.stream()
                .map(transporterMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 5초 타임아웃 처리
     */
    private CompletableFuture<OfferResult> waitForResponseWithTimeout(Long offerId) {
        CompletableFuture<OfferResult> future = new CompletableFuture<>();

        // Map에 저장 (WebSocket 응답 수신 시 complete 호출)
        offerFutureMap.put(offerId, future);

        // 5초 후 타임아웃 처리
        scheduledExecutor.schedule(() -> {
            CompletableFuture<OfferResult> f = offerFutureMap.remove(offerId);
            if (f != null && !f.isDone()) {
                f.complete(OfferResult.TIMEOUT);

                // DB 업데이트 (타임아웃 처리)
                updateOfferStatusToTimeout(offerId);
            }
        }, 5, TimeUnit.SECONDS);

        return future;
    }

    /**
     * CompletableFuture 완료 처리
     */
    private void completeOfferFuture(Long offerId, OfferResult result) {
        CompletableFuture<OfferResult> future = offerFutureMap.remove(offerId);
        if (future != null && !future.isDone()) {
            future.complete(result);
        }
    }

    /**
     * 타임아웃 시 DB 업데이트
     */
    @Transactional
    protected void updateOfferStatusToTimeout(Long offerId) {
        offerRepository.findById(offerId).ifPresent(offer -> {
            if (offer.getStatus() == OfferStatus.PENDING) {
                offer.timeout();
                offerRepository.save(offer);
                log.info("[AutoDispatch] 타임아웃 처리 완료 - offerId: {}", offerId);
            }
        });
    }

    /**
     * DispatchOffer 생성 (별도 트랜잭션)
     */
    @Transactional
    protected DispatchOffer createOfferInTransaction(Dispatch dispatch, Transporter driver, int sequence) {
        DispatchOffer offer = DispatchOffer.builder()
                .dispatch(dispatch)
                .transporter(driver)
                .sequence(sequence)
                .build();
        return offerRepository.save(offer);
    }

    /**
     * WebSocket으로 기사에게 알림 전송
     */
    private void sendNotificationToDriver(Long transporterId, DispatchOfferNotification notification) {
        String destination = "/queue/" + transporterId + "/dispatch";
        messagingTemplate.convertAndSend(destination, notification);
        log.debug("[AutoDispatch] WebSocket 메시지 전송 - destination: {}, offerId: {}",
                destination, notification.offerId());
    }

    /**
     * 거리 계산 (km)
     */
    private Double calculateDistanceKm(Dispatch dispatch, Transporter driver) {
        if (driver.getCurrentLocation() == null) {
            return null;
        }

        double driverLat = driver.getCurrentLocation().getY();
        double driverLon = driver.getCurrentLocation().getX();

        return calculateDistance(dispatch.getStartLatitude(), dispatch.getStartLongitude(), driverLat, driverLon);
    }

    /**
     * Haversine 공식을 사용한 거리 계산 (km 단위)
     */
    private Double calculateDistance(double startLat, double startLon, double endLat, double endLon) {
        double earthRadiusKm = 6371.0;

        double dLat = Math.toRadians(endLat - startLat);
        double dLon = Math.toRadians(endLon - startLon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(endLat)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c;
    }

    /**
     * 배차 상태를 OPEN으로 변경 (모든 자동배차 대상 기사가 거절/타임아웃한 경우)
     */
    @Transactional
    protected void updateDispatchStatusToOpen(Long dispatchId) {
        dispatchRepository.findById(dispatchId).ifPresent(dispatch -> {
            if (dispatch.getStatus() == StatusType.HOLD) {
                dispatch.setStatus(StatusType.OPEN);
                dispatchRepository.save(dispatch);
                log.info("[AutoDispatch] 배차 상태 HOLD → OPEN 변경 완료 - dispatchId: {}", dispatchId);
            }
        });
    }
}
