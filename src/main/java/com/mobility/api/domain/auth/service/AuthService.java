package com.mobility.api.domain.auth.service;

import com.mobility.api.domain.auth.dto.request.TransporterLoginReq;
import com.mobility.api.domain.auth.dto.request.TransporterSignupReq;
import com.mobility.api.domain.auth.dto.response.TokenDto;
import com.mobility.api.domain.auth.dto.request.OfficeLoginReq;
import com.mobility.api.domain.auth.dto.request.OfficeSignupReq;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.enums.ManagerRole;
import com.mobility.api.domain.office.repository.ManagerRepository;
import com.mobility.api.domain.office.repository.OfficeRepository;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.jwt.JwtProvider;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ManagerRepository managerRepository;
    private final OfficeRepository officeRepository;
    private final TransporterRepository transporterRepository;

    private final JwtProvider jwtProvider;           // 토큰 발급기
    private final PasswordEncoder passwordEncoder;   // 비밀번호 검사기

    /**
     * 사무실 회원가입 (사무실 생성 + 사장님 계정 생성)
     */
    @Transactional
    public void signupOffice(OfficeSignupReq req) {

        // 1. 아이디 중복 검사
        if (managerRepository.existsByLoginId(req.loginId())) {
//            throw new GlobalException(ResultCode.DUPLICATE_USER_ID); // 에러 코드 추가 필요
            throw new GlobalException(ResultCode.FIXME_FAIL);
        }

        // 2. 사무실 정보 저장
        Office office = Office.builder()
                .officeName(req.officeName())
                .officeRegistrationNumber(req.officeRegistrationNumber())
                .officeAddress(req.officeAddress())
                .officeTelNumber(req.officeTelNumber())
                .build();

        officeRepository.save(office); // DB에 사무실 Insert (이때 ID 생성됨)

        // 3. 사장님(Manager) 정보 저장
        Manager manager = Manager.builder()
                .loginId(req.loginId())
                .password(passwordEncoder.encode(req.password())) // 비밀번호 암호화
//                .password(req.password()) // 비밀번호 암호화
                .name(req.managerName())
                .phone(req.managerPhone())
                .email(req.managerEmail())
                .role(ManagerRole.OWNER) // 가입 시점엔 무조건 사장님(OWNER)
                .office(office)          // 위에서 만든 사무실 연결
                .build();

        managerRepository.save(manager);
    }

    /**
     * 사무실 관리자 (Manager) 로그인
     */
    @Transactional
    public TokenDto officeLogin(OfficeLoginReq req) {
        // 1. 아이디(loginId)로 매니저 찾기
        Manager manager = managerRepository.findByLoginId(req.loginId())
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(req.password(), manager.getPassword())) {
//            throw new GlobalException(ResultCode.PASSWORD_NOT_MATCH);
            throw new GlobalException(ResultCode.FIXME_FAIL);
        }

        // 3. 토큰 생성
        // Subject: loginId (나중에 이걸로 DB 조회함)
        // Role: "ROLE_OFFICE" (일단 고정, 필요하면 manager.getRole().name() 사용)
        String accessToken = jwtProvider.createToken(manager.getLoginId(), "ROLE_OFFICE");

        return new TokenDto(accessToken, "Bearer");
    }

    /**
     * [기사] 회원가입
     */
    @Transactional
    public void signupTransporter(TransporterSignupReq req) {
        // 1. 전화번호 중복 검사
        if (transporterRepository.existsByPhone(req.phone())) {
            throw new GlobalException(ResultCode.FIXME_FAIL); // 이미 가입된 번호
        }

        // 2. 기사 정보 생성 및 저장
        Transporter transporter = Transporter.builder()
                .name(req.name())
                .phone(req.phone())
                .isAutoDispatch(req.isAutoDispatch())
                .build();

        transporterRepository.save(transporter);
    }

    /**
     * [기사] 로그인 (전화번호만 일치하면 통과)
     */
    @Transactional
    public TokenDto transporterLogin(TransporterLoginReq req) {
        // 1. 전화번호로 기사 찾기
        Transporter transporter = transporterRepository.findByPhone(req.phone())
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER)); // 가입되지 않은 기사

        // 2. 비밀번호 검증 없음 (요구사항 반영: 전화번호만 맞으면 바로 로그인 처리)

        // 3. 토큰 생성
        // Subject: phone (기사는 ID 대신 전화번호를 식별자로 씀)
        // Role: "ROLE_TRANSPORTER" (권한 분리)
        String accessToken = jwtProvider.createToken(transporter.getPhone(), "ROLE_TRANSPORTER");

        return new TokenDto(accessToken, "Bearer");
    }

}
