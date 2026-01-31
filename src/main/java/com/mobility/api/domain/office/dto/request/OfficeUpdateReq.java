package com.mobility.api.domain.office.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OfficeUpdateReq(

        String name,            // officeName 매핑

        String registrationNumber, // officeRegistrationNumber 매핑

        String address,         // officeAddress 매핑

        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식이 아닙니다.")
        String telNumber        // officeTelNumber 매핑
) {}