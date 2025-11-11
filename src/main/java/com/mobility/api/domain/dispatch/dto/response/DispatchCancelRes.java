package com.mobility.api.domain.dispatch.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;

public record DispatchCancelRes (
        Long dispatcherId
){
    public static DispatchCancelRes from(Dispatch dispatch) {
        return new DispatchCancelRes(
                dispatch.getId()
        );
    }
}
