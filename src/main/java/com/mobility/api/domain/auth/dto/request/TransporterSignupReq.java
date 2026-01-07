package com.mobility.api.domain.auth.dto.request;

public record TransporterSignupReq(
        String name,
        String phone,
        Boolean isAutoDispatch
) {}