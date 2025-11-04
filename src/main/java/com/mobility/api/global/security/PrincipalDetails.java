package com.mobility.api.global.security;

import com.mobility.api.domain.transporter.entity.Transporter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class PrincipalDetails implements UserDetails {

    private final Transporter transporter; //  사용자 엔티티를 직접 보유

    public PrincipalDetails(Transporter transporter) {
        this.transporter = transporter;
    }

    // ArgumentResolver가 사용할 메서드
//    public Transporter getTransporter() {
//        return transporter;
//    }

    public Long getTransporterId() {
//        return transporter.getId();
        return null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return "";
    }
}
