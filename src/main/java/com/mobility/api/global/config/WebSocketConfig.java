package com.mobility.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 메시지 브로커 설정
     * - /queue: 1:1 개인 메시지 (특정 기사에게 배차 알림)
     * - /topic: 1:N 브로드캐스트 메시지 (선택사항)
     * - /app: 클라이언트가 서버로 메시지를 보낼 때 사용하는 prefix
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 구독 경로 prefix (/queue, /topic)
        registry.enableSimpleBroker("/queue", "/topic");

        // 클라이언트가 메시지를 보낼 때 사용하는 prefix
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * WebSocket 엔드포인트 등록
     * - /ws: WebSocket handshake 엔드포인트
     * - SockJS fallback 지원 (WebSocket을 지원하지 않는 브라우저 대응)
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 설정 (개발 환경용)
                .withSockJS();  // SockJS fallback 지원
    }
}
