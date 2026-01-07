package com.mobility.api.domain.auth.controller;

import com.mobility.api.domain.auth.dto.request.OfficeLoginReq;
import com.mobility.api.domain.auth.dto.request.TransporterLoginReq;
import com.mobility.api.domain.auth.dto.request.TransporterSignupReq;
import com.mobility.api.domain.auth.dto.response.TokenDto;
import com.mobility.api.domain.auth.service.AuthService;
import com.mobility.api.domain.auth.dto.request.OfficeSignupReq;
import com.mobility.api.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "기사 인증 관련 요청(/api/v1/auth/transporter/...)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/transporter")
public class AuthTransporterV1Controller {

    private final AuthService authService;

    @Operation(summary = "기사 회원가입")
    @PostMapping("/signup")
    public CommonResponse<String> signupOffice(
            @RequestBody TransporterSignupReq req
    ) {
        authService.signupTransporter(req);
        return CommonResponse.success(null);
    }

    /**
     * <pre>
     * 기사 - 로그인
     * </pre>
     */
    @Operation(summary = "기사 로그인 요청", description = "전화번호를 입력받아 Access Token을 발급합니다.")
    @PostMapping("/login") // RequestMapping(method=POST)와 같습니다.
    public CommonResponse<TokenDto> login(@RequestBody TransporterLoginReq req) {

        // 1. 서비스 호출 (로그인 로직 수행)
        TokenDto tokenDto = authService.transporterLogin(req);

        // 2. 결과 반환
        return CommonResponse.success(tokenDto);
    }

}
