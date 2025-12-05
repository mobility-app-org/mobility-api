package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;

// 배차 선택, 완료 res
public record DispatchAssignCompleteRes(
        Long dispatcherId,
        Long transporterId
) {
    public static DispatchAssignCompleteRes from(Dispatch dispatch) {
//        if (dispatch.getTransporter() != null) {
//            throw new GlobalException(ResultCode.NOT_FOUND_USER);
//        }

        return new DispatchAssignCompleteRes(
                dispatch.getId(),
                dispatch.getTransporter().getId()
        );
    }
}
