package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;


// 배차 선택, 완료 res
public record DispatchRes(
        Long dispatcherId,
        Long transporterId
) {
    public static DispatchRes from(Dispatch dispatch) {
        if (dispatch.getTransporter() != null) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER);
        }

        return new DispatchRes(
                dispatch.getId(),
                dispatch.getTransporter().getId()
        );
    }
}
