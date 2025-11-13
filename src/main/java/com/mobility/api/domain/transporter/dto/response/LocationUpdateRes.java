package com.mobility.api.domain.transporter.dto.response;

import com.mobility.api.domain.transporter.entity.LocationHistory;

public record LocationUpdateRes(
        Long transporterId
) {
    public static LocationUpdateRes from(LocationHistory locationHistory) {
        return new LocationUpdateRes(
                locationHistory.getTransporter().getId()
        );
    }
}
