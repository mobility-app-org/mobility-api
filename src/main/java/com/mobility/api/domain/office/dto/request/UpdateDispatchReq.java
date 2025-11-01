package com.mobility.api.domain.office.dto.request;

import com.mobility.api.domain.dispatch.enums.CallType;
import com.mobility.api.domain.dispatch.enums.ServiceType;
import com.mobility.api.domain.dispatch.enums.StatusType;
import lombok.Getter;

@Getter
public class UpdateDispatchReq {

    private String startLocation;
    private String destinationLocation;
    private Integer charge;
    private String clientPhoneNumber;
    private StatusType status;
    private CallType call;
    private Boolean active;
    private ServiceType service;
}
