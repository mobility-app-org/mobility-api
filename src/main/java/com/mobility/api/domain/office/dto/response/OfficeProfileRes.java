package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.office.entity.Office;
import lombok.Builder;

@Builder
public record OfficeProfileRes(
        Long id,
        String name,           // 사무실 이름
        String registrationNumber, // 사업자 번호
        String telNumber,          // 사업장 전화번호
        String address,        // 주소
        String createdAt,       // 가입일
        String updatedAt       // 수정일
) {
    public static OfficeProfileRes from(Office office) {
        return OfficeProfileRes.builder()
                .id(office.getId())
                .name(office.getOfficeName())
                .registrationNumber(office.getOfficeRegistrationNumber())
                .telNumber(office.getOfficeTelNumber())
                .address(office.getOfficeAddress())
                .createdAt(office.getCreatedAt().toString())
                .updatedAt(office.getUpdatedAt().toString())
                .build();
    }
}