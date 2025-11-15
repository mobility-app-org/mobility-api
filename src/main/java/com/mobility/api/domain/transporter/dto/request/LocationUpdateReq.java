package com.mobility.api.domain.transporter.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LocationUpdateReq(

        @NotNull(message = "위도는 필수값입니다.")
        @Min(value = -90, message = "위도는 -90에서 90 사이여야 합니다.")
        @Max(value = 90, message = "위도는 -90에서 90 사이여야 합니다.")
        Double latitude,

        @NotNull(message = "경도는 필수값입니다.")
        @Min(value = -180, message = "경도는 -180에서 180 사이여야 합니다.")
        @Max(value = 180, message = "경도는 -180에서 180 사이여야 합니다.")
        Double longitude
){
    public static LocationUpdateReq of(Double latitude, Double longitude) {
        return new LocationUpdateReq(latitude, longitude);
    }
}
