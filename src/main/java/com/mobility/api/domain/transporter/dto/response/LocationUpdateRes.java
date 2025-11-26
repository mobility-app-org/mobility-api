package com.mobility.api.domain.transporter.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record LocationUpdateRes(
        @JsonProperty("transporter_id")
        @Schema(description = "위치가 업데이트된 기사의 고유 ID", example = "2")
        Long transporterId
) {
    public static LocationUpdateRes from(Long transporterId) {
            return new LocationUpdateRes(transporterId);
        }

//    public static LocationUpdateRes from(LocationHistory locationHistory) {
//        return new LocationUpdateRes(
//                locationHistory.getTransporter().getId()
//        );
//    }
}
