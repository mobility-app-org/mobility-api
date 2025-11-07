package com.mobility.api.domain.transporter.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;

public record DispatchnRes(
        Long dispatcherId,
        Long transporterId
) {
    public static DispatchnRes from(Dispatch dispatch) {
        if (dispatch.getTransporter() != null) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER);
        }

        return new DispatchnRes(
                dispatch.getId(),
                dispatch.getTransporter().getId()
        );
    }
}
