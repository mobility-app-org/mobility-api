package com.mobility.api.domain.transporter.controller;

import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchAssignCompleteRes;
import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.transporter.dto.request.LocationUpdateReq;
import com.mobility.api.domain.transporter.dto.response.LocationUpdateRes;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.service.LocationService;
import com.mobility.api.global.annotation.CurrentUser;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.response.ResultCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "기사 관련 요청(/api/v1/transporter/...)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transporter")
public class TransporterV1Controller {

    private final DispatcherService dispatcherService;
    private final LocationService locationService;

    @Operation(summary = "배차 할당", description = "")
    @PatchMapping("/dispatch-assign/{dispatchId}")
    public CommonResponse<DispatchAssignCompleteRes> assignDispatch(
            @PathVariable Long dispatchId, @CurrentUser Transporter transporter) {

        Long transporterId = getValidatedTransporterId(transporter);

        return CommonResponse.success(dispatcherService.assignDispatch(dispatchId, transporterId));
    }

    @Operation(summary = "배차 취소", description = "")
    @PatchMapping("/dispatch-cancel/{dispatchId}")
    public CommonResponse<DispatchCancelRes> cancelDispatch(
            @PathVariable Long dispatchId, @CurrentUser Transporter transporter) {

        Long transporterId = getValidatedTransporterId(transporter);

        return CommonResponse.success(dispatcherService.cancelDispatch(dispatchId, transporterId));
    }

    @Operation(summary = "배차 완료", description = "")
    @PatchMapping("/dispatch-complete/{dispatchId}")
    public CommonResponse<DispatchAssignCompleteRes> completeDispatch(
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

    /**
        ** 기사 위치 정보 수집 관련
     */
    @Operation(summary = "기사 실시간 위치 업데이트", description = "기사 앱에서 전송된 위도(latitude), 경도(longitude) 정보를 저장")
    @PostMapping("/location/update")
    public CommonResponse<LocationUpdateRes> updateTransporterLocation(
            @Valid @RequestBody LocationUpdateReq locationUpdateReq,
            @CurrentUser Transporter transporter){

        Long transporterId = getValidatedTransporterId(transporter);
        Long savedId = locationService.processLocationUpdate(transporterId, locationUpdateReq);
        LocationUpdateRes response = LocationUpdateRes.from(savedId);

        return CommonResponse.success(response);
    }

    /**
     * 기사의 배차 리스트 정렬 등을 위해, 현재 기사 위치 기준으로 배차
     */
}
