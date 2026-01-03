package com.mobility.api.global.security;

import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.transporter.entity.Transporter;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter // 컨트롤러에서 꺼내 쓰기 편하게 Getter 추가
public class PrincipalDetails implements UserDetails {

    private Manager manager;         // 사무실 객체 (null일 수 있음)
    private Transporter transporter; // 기사 객체 (null일 수 있음)
    private String role;             // 현재 로그인한 사람의 역할 (ROLE_OFFICE 등)

    // 1. 사무실 로그인용 생성자
    public PrincipalDetails(Manager manager) {
        this.manager = manager;
        this.role = "ROLE_OFFICE";
    }

    // 2. 기사 로그인용 생성자
    public PrincipalDetails(Transporter transporter) {
        this.transporter = transporter;
        this.role = "ROLE_TRANSPORTER";
    }

    // 권한(Role) 반환: Spring Security가 "이 사람 권한이 뭐야?"라고 물어볼 때 씀
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    // 비밀번호 반환: 로그인 시 비번 검사할 때 씀
    @Override
    public String getPassword() {
        if (manager != null) return manager.getPassword();
//        if (transporter != null) return transporter.getPassword();
        if (transporter != null) return null;
        return null;
    }

    // 아이디(식별자) 반환: 로그에 찍히거나 할 때 씀
    @Override
    public String getUsername() {
        if (manager != null) return manager.getLoginId();
        if (transporter != null) return transporter.getPhone(); // 기사는 전화번호가 ID라면
        return null;
    }

    // 계정 만료/잠김 여부 (일단 모두 true로 설정)
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    // ============================================================================

    // ArgumentResolver가 사용할 메서드
//    public Transporter getTransporter() {
//        return transporter;
//    }

//    public Long getTransporterId() {
////        return transporter.getId();
//        return null;
//    }
//
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        return List.of();
//    }
//
//    @Override
//    public String getPassword() {
//        return null;
//    }
//
//    @Override
//    public String getUsername() {
//        return "";
//    }
}
