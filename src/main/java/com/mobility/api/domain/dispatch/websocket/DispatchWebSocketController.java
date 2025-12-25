package com.mobility.api.domain.dispatch.websocket;

import com.mobility.api.domain.dispatch.dto.request.DispatchResponseReq;
import com.mobility.api.domain.dispatch.service.AutoDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 배차 WebSocket 컨트롤러
 * - 기사가 WebSocket (STOMP)로 배차 수락/거절 메시지를 보낼 때 처리
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class DispatchWebSocketController {

    private final AutoDispatchService autoDispatchService;

    /**
     * 배차 수락
     * - 경로: /app/dispatch/accept
     * - 기사가 배차 알림을 받고 수락 버튼을 누를 때 호출
     *
     * @param request       배차 응답 요청 (offerId 포함)
     * @param transporterId 기사 ID (헤더에서 추출, dev/local: X-Temp-User-Id)
     */
    @MessageMapping("/dispatch/accept")
    public void acceptDispatch(
            @Payload DispatchResponseReq request,
            @Header(value = "X-Temp-User-Id", required = false) String transporterId) {

        log.info("[WebSocket] 배차 수락 요청 - offerId: {}, transporterId: {}", request.offerId(), transporterId);

        try {
            Long userId = parseTransporterId(transporterId);
            autoDispatchService.handleAccept(request.offerId(), userId);
            log.info("[WebSocket] 배차 수락 처리 완료 - offerId: {}", request.offerId());
        } catch (Exception e) {
            log.error("[WebSocket] 배차 수락 처리 실패 - offerId: {}", request.offerId(), e);
            // 에러는 로깅만 하고, 클라이언트에게는 별도 응답 없음 (필요 시 /queue/{userId}/errors로 전송)
        }
    }

    /**
     * 배차 거절
     * - 경로: /app/dispatch/reject
     * - 기사가 배차 알림을 받고 거절 버튼을 누를 때 호출
     *
     * @param request       배차 응답 요청 (offerId 포함)
     * @param transporterId 기사 ID (헤더에서 추출, dev/local: X-Temp-User-Id)
     */
    @MessageMapping("/dispatch/reject")
    public void rejectDispatch(
            @Payload DispatchResponseReq request,
            @Header(value = "X-Temp-User-Id", required = false) String transporterId) {

        log.info("[WebSocket] 배차 거절 요청 - offerId: {}, transporterId: {}", request.offerId(), transporterId);

        try {
            Long userId = parseTransporterId(transporterId);
            autoDispatchService.handleReject(request.offerId(), userId);
            log.info("[WebSocket] 배차 거절 처리 완료 - offerId: {}", request.offerId());
        } catch (Exception e) {
            log.error("[WebSocket] 배차 거절 처리 실패 - offerId: {}", request.offerId(), e);
        }
    }

    /**
     * transporterId 파싱
     * - 헤더에서 받은 문자열을 Long으로 변환
     */
    private Long parseTransporterId(String transporterId) {
        if (transporterId == null || transporterId.isBlank()) {
            throw new IllegalArgumentException("transporterId is required");
        }
        try {
            return Long.parseLong(transporterId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid transporterId: " + transporterId);
        }
    }
}
