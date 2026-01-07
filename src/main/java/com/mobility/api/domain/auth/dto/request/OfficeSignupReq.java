package com.mobility.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record OfficeSignupReq(

        // --- 사무실 정보 ---
        @Schema(description = "사업장 이름", example = "mobi 탁송")
        String officeName,

        @Schema(description = "사업자 등록번호", example = "123-45-67890")
        String officeRegistrationNumber,

        @Schema(description = "사업장 주소", example = "전북 전주시 완산구 용머리로 29")
        String officeAddress,

        @Schema(description = "사무실 전화번호", example = "063-123-4567")
        String officeTelNumber,

        // --- 사장님(관리자) 정보 ---
        @Schema(description = "관리자 웹 아이디", example = "tak123")
        String loginId,

        @Schema(description = "비밀번호", example = "tak123!")
        String password,

        @Schema(description = "관리자 이름", example = "김규원")
        String managerName,

        @Schema(description = "관리자 휴대폰 번호", example = "010-5244-4070")
        String managerPhone,

        @Schema(description = "관리자 이메일", example = "rlarbdnjs0630@gmail.com")
        String managerEmail
) {}