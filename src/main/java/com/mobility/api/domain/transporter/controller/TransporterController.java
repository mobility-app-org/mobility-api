package com.mobility.api.domain.transporter.controller;

import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchRes;
import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.global.annotation.CurrentUser;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api1/v1/transporter")
public class TransporterController {

    private final DispatcherService dispatcherService;

    @PatchMapping("/dispatch-assign/{dispatch_id}")
    public CommonResponse<DispatchRes> assignDispatch(
            @PathVariable Long dispatchId, @CurrentUser Transporter transporter) {

        Long transporterId = getValidatedTransporterId(transporter);

        return CommonResponse.success(dispatcherService.assignDispatch(dispatchId, transporterId));
    }

    @PatchMapping("dispatch-cancel/{dispatchId}")
    public CommonResponse<DispatchCancelRes> cancelDispatch(
            @PathVariable Long dispatchId, @CurrentUser Transporter transporter) {

        Long transporterId = getValidatedTransporterId(transporter);

        return CommonResponse.success(dispatcherService.cancelDispatch(dispatchId, transporterId));
    }

    @PatchMapping("/dispatch-complete/{dispatchId}")
    public CommonResponse<DispatchRes> completeDispatch(
            @PathVariable Long dispatchId, @CurrentUser Transporter transporter) {

        Long transporterId = getValidatedTransporterId(transporter);

        return CommonResponse.success(dispatcherService.completeDispatch(dispatchId, transporterId));
    }

    private Long getValidatedTransporterId(Transporter transporter) {
        Long transporterId = transporter.getId();

        if  (transporterId == null) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER);
        }
        return transporterId;
    }
}
