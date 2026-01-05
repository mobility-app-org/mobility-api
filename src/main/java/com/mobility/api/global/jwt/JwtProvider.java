package com.mobility.api.global.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private final Key key;
    private final long accessTokenValidityInMilliseconds;

    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-validity-in-seconds:3600}") long seconds) {

        // 시크릿 키를 Base64로 디코딩해서 Key 객체로 변환
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);

        // 토큰 유효 시간 (초 단위 -> 밀리초 변환)
        this.accessTokenValidityInMilliseconds = seconds * 1000;
    }

    /**
     * 1. 토큰 생성
     * @param subject  사용자 식별자 (loginId 또는 phone)
     * @param role     사용자 권한 (ROLE_OFFICE, ROLE_TRANSPORTER)
     * @return String  생성된 JWT 토큰
     */
    public String createToken(String subject, String role) {
        long now = (new Date()).getTime();
        Date validity = new Date(now + this.accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(subject)               // "sub": "tak123"
                .claim("role", role)               // "role": "ROLE_OFFICE" (커스텀 클레임)
                .setIssuedAt(new Date(now))        // "iat": 현재시간
                .setExpiration(validity)           // "exp": 만료시간
                .signWith(key, SignatureAlgorithm.HS256) // 암호화 알고리즘
                .compact();
    }

    /**
     * 2. 토큰에서 ID(Subject) 꺼내기
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 3. 토큰에서 권한(Role) 꺼내기
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 4. 토큰 유효성 검증
     * - 위변조 확인, 만료 시간 확인 등
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }

    // 내부적으로 토큰을 파싱해서 Claims(내용물)를 꺼내는 헬퍼 메서드
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}