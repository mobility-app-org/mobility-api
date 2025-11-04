package com.mobility.api.domain.transporter.controller;

import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.transporter.service.TransporterService;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api1/v1/transporter")
public class TransporterController {

    private final TransporterService transporterService;
    private final DispatcherService dispatcherService;

    @PatchMapping("/dispatch-assign/{dispatch_id}")
    public CommonResponse<ResultCode> assignDispatch(@PathVariable Long dispatch_id, @AuthenticationPrincipal UserDetails userDetails) { // // todo spring security 로그인한 사용자 정보 가져오기

        Long transporterId = userDetails.getTransporterId();
        dispatcherService.assignDispatch(dispatch_id, transporterId);

        return CommonResponse.success(ResultCode.DISPATCH_ASSIGN_SUCCESS);
    }

    @PatchMapping("dispatch-cancel/{dispatchId}")

    @PatchMapping("/dispatch-complete/{dispatchId}")

}
