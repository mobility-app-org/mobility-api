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

    /**
     * 'dev' 또는 'local' 프로필일 때 활성화되는 보안 설정
     * - 모든 API(/api1/**) 요청을 인증 없이 허용합니다.
     * - 'X-Temp-User-Id' 헤더를 사용한 임시 인증이 가능해집니다.
     */
    @Bean
    @Profile({"dev", "local"}) // 'dev' 또는 'local' 프로필에서만 이 설정을 사용
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
                        .requestMatchers("/api1/**").permitAll() // /api1/로 시작하는 모든 요청 허용
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
                        .requestMatchers("/api1/auth/**").permitAll() // 로그인 API 등은 허용
                        .requestMatchers("/api1/**").authenticated() // 나머지 API는 인증 필요
                        .anyRequest().denyAll()
                );

        // .addFilterBefore( ... JWT 인증 필터 추가 ...)

        return http.build();
    }
}
