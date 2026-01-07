package com.mobility.api.domain.auth.controller;

import com.mobility.api.domain.auth.dto.response.TokenDto;
import com.mobility.api.domain.auth.service.AuthService;
import com.mobility.api.domain.auth.dto.request.OfficeLoginReq;
import com.mobility.api.domain.auth.dto.request.OfficeSignupReq;
import com.mobility.api.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사무실 인증 관련 요청(/api/v1/auth/office/...)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/office")
public class AuthOfficeV1Controller {

    private final AuthService authService;

    @Operation(summary = "사무실 회원가입")
    @PostMapping("/signup")
    public CommonResponse<String> signupOffice(
            @RequestBody OfficeSignupReq req
    ) {
        authService.signupOffice(req);
        return CommonResponse.success(null);
    }

    /**
     * <pre>
     * 사무실 - 로그인
     * </pre>
     */
    @Operation(summary = "사무실 로그인 요청", description = "ID와 비밀번호를 입력받아 Access Token을 발급합니다.")
    @PostMapping("/login") // RequestMapping(method=POST)와 같습니다.
    public CommonResponse<TokenDto> login(@RequestBody OfficeLoginReq req) {

        // 1. 서비스 호출 (로그인 로직 수행)
        TokenDto tokenDto = authService.officeLogin(req);

        // 2. 결과 반환
        return CommonResponse.success(tokenDto);
    }

}
