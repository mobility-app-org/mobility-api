package com.mobility.api.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record TransporterLoginReq(

        @Schema(description = "기사 전화번호", example = "010-1234-1234")
        String phone
) {}