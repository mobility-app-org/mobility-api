package com.mobility.api.domain.transporter.controller;

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
    public CommonResponse<ResultCode> assignDispatch(@PathVariable Long dispatch_id, @CurrentUser Transporter transporter) {

        Long transporterId = transporter.getId();

        if  (transporterId == null) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER);
        }

        dispatcherService.assignDispatch(dispatch_id, transporterId);

        return CommonResponse.success(ResultCode.DISPATCH_ASSIGN_SUCCESS);
    }

//    @PatchMapping("dispatch-cancel/{dispatchId}")

//    @PatchMapping("/dispatch-complete/{dispatchId}")

}
