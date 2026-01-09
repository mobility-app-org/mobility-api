package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.dispatch.enums.StatusType;

import java.util.Map;

public record DispatchSummaryRes(
        Map<StatusType, Long> statusCounts
) {
    public static DispatchSummaryRes from(Map<StatusType, Long> counts) {
        return new DispatchSummaryRes(counts);
    }
}
