package com.mobility.api.domain.transporter.dto.request;

import com.mobility.api.domain.transporter.TransporterStatus;

public record TransporterStatusUpdateReq(
        TransporterStatus status // 변경할 상태 (ACTIVE, REJECTED 등)
) {}