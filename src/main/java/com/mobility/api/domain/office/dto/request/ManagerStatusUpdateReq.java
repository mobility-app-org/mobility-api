package com.mobility.api.domain.office.dto.request;

import com.mobility.api.domain.office.enums.ManagerStatus;
import jakarta.validation.constraints.NotNull;

public record ManagerStatusUpdateReq(
        @NotNull(message = "변경할 상태값은 필수입니다.")
        ManagerStatus status
) {}
