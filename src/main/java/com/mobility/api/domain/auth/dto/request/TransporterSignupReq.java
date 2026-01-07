package com.mobility.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransporterSignupReq(
        @Schema(description = "기사 이름", example = "김기사")
        String name,

        @Schema(description = "기사 전화번호", example = "010-1234-1234")
        String phone,

        @Schema(description = "자동배차 여부", example = "false")
        Boolean isAutoDispatch
) {}