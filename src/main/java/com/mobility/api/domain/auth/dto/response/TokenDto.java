package com.mobility.api.domain.auth.dto.response;

public record TokenDto(
        String accessToken,
        String grantType
) {}