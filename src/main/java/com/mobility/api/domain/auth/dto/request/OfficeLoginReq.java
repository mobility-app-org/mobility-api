package com.mobility.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record OfficeLoginReq(
        @Schema(description = "관리자 웹 아이디", example = "tak123")
        String loginId,

        @Schema(description = "비밀번호", example = "tak123!")
        String password
) {}