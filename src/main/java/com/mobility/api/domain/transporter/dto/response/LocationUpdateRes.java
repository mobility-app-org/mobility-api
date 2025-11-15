package com.mobility.api.domain.transporter.dto.response;

import com.mobility.api.domain.transporter.entity.LocationHistory;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;

public record LocationUpdateRes(
        Long transporterId
) {
    public static LocationUpdateRes from(LocationHistory locationHistory) {
        if (locationHistory.getTransporter() != null) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER);
        }

        return new LocationUpdateRes(
                locationHistory.getTransporter().getId()
        );
    }
}
