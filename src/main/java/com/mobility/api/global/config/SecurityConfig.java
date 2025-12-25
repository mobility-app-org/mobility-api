package com.mobility.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SWAGGER_URLS = {
            "/swagger-ui.html", // 메인 UI 페이지
            "/swagger-ui/**",   // UI 리소스 (js, css)
            "/v3/api-docs/**"   // API 설계도(JSON)
    };

    private static final String[] WEBSOCKET_URLS = {
            "/ws/**",      // WebSocket handshake 엔드포인트
            "/app/**",     // STOMP 발행 경로 (기사 → 서버)
            "/queue/**",   // STOMP 구독 경로 (서버 → 기사, 개인)
            "/topic/**"    // STOMP 구독 경로 (서버 → 전체, 브로드캐스트)
    };

    /**
     * 'dev' 또는 'local' 프로필일 때 활성화되는 보안 설정
     * 프로필이 지정되지 않은 경우에도 기본으로 사용
     * - 모든 API(/api/**) 요청을 인증 없이 허용
     * - 'X-Temp-User-Id' 헤더를 사용한 임시 인증이 가능
     */
    @Bean
    @Profile({"dev", "local", "default"})
    public SecurityFilterChain devSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. CSRF 비활성화 (Stateless API이므로)
                .csrf(csrf -> csrf.disable())

                // 2. 세션 비활성화 (Stateless API이므로)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. HTTP Basic 인증 비활성화 (로그인 팝업 및 generated password 로그 제거)
                .httpBasic(httpBasic -> httpBasic.disable())

                // 4. Form 로그인 비활성화
                .formLogin(formLogin -> formLogin.disable())

                // 5. API 경로에 대한 접근 허용 설정
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/**").permitAll()
                        .requestMatchers(SWAGGER_URLS).permitAll()
                        .requestMatchers(WEBSOCKET_URLS).permitAll()  // WebSocket 경로 허용
                        .anyRequest().authenticated() // 그 외 모든 요청은 인증 필요 (사실상 거의 없음)
                );
        return http.build();
    }

    /**
     * 'prod' (운영) 프로필일 때 활성화되는 보안 설정
     * - 여기서는 '/api1/auth/**' (로그인/회원가입)만 허용하고
     * - 나머지 모든 요청은 JWT 토큰 검사 등을 통해 인증을 요구해야 합니다.
     */
    @Bean
    @Profile("prod")
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())

                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll() // 로그인 API 등은 허용
                        .requestMatchers("/api/**").authenticated() // 나머지 API는 인증 필요
                        .anyRequest().denyAll()
                );

        // .addFilterBefore( ... JWT 인증 필터 추가 ...)

        return http.build();
    }
}
