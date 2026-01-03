package com.mobility.api.global.security;

import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.repository.ManagerRepository;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final ManagerRepository managerRepository;
    private final TransporterRepository transporterRepository;

    /**
     * 토큰 필터(JwtAuthenticationFilter)에서 호출하는 메서드
     * @param username : 토큰에 담긴 ID (Manager는 loginId, Transporter는 phone)
     * @param role : 토큰에 담긴 권한 (ROLE_OFFICE, ROLE_TRANSPORTER)
     */
    public UserDetails loadUserByUsernameAndRole(String username, String role) {

        // 1. 사무실 관리자 (Manager)인 경우 -> loginId로 찾기
        if ("ROLE_OFFICE".equals(role)) {
            Manager manager = managerRepository.findByLoginId(username)
                    .orElseThrow(() -> new UsernameNotFoundException("해당 아이디를 가진 관리자가 없습니다: " + username));

            return new PrincipalDetails(manager);
        }

        // 2. 기사 (Transporter)인 경우 -> phone으로 찾기
        else if ("ROLE_TRANSPORTER".equals(role)) {
            Transporter transporter = transporterRepository.findByPhone(username)
                    .orElseThrow(() -> new UsernameNotFoundException("해당 번호를 가진 기사가 없습니다: " + username));

            return new PrincipalDetails(transporter);
        }

        throw new UsernameNotFoundException("알 수 없는 권한입니다: " + role);
    }
}