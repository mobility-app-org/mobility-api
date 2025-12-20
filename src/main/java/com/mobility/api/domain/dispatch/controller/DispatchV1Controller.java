package com.mobility.api.domain.dispatch.controller;

import com.mobility.api.domain.dispatch.dto.response.DispatchDetailRes;
import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.transporter.dto.response.TransporterMatchResponse;
import com.mobility.api.domain.transporter.service.TransporterService;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.response.ResultCode;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Dispatch Matching", description = "배차 관련 API")
@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchV1Controller {

    private final DispatchRepository dispatchRepository;
    private final TransporterService transporterService;
    private final DispatcherService dispatcherService;

    // 배차 상세 조회
    @Operation(
            summary = "배차 상세 조회",
            description = """
                    배차 ID로 배차 상세 정보를 조회합니다.

                    - 배차의 기본 정보 (서비스 타입, 요금, 출발지, 도착지, 상태)를 반환합니다.
                    - 현재 로그인한 기사의 최신 위치와 출발지 간 직선거리(km)를 계산하여 반환합니다.
                    - 기사의 위치 정보가 없는 경우 distanceKm은 null로 반환됩니다.
                    - 태그 정보(경유 여부, 결제 방식, 톨비 방식)를 포함합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "배차 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "배차를 찾을 수 없음"
            )
    })
    @GetMapping("/{dispatchId}")
    public CommonResponse<DispatchDetailRes> getDispatchDetail(
            @io.swagger.v3.oas.annotations.Parameter(description = "배차 ID", example = "1", required = true)
            @PathVariable Long dispatchId,
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @com.mobility.api.global.annotation.CurrentUser Long currentUserId
    ) {
        DispatchDetailRes dispatchDetail = dispatcherService.getDispatchDetail(dispatchId, currentUserId);
        return CommonResponse.success(dispatchDetail);
    }

    // 배차 등록 시, 주변의 기사 조회 (km 단위 반환)
    @Operation(summary = "배차 주변 기사 추천", description = "해당 배차의 출발지 기준 반경 50km 이내 기사를 가까운 순으로 조회합니다.")
    @GetMapping("/{dispatchId}/nearby-drivers")
    public CommonResponse<List<TransporterMatchResponse>> getNearbyDrivers(@PathVariable Long dispatchId) {

        // 1. 배차 정보 조회 (출발지 좌표 획득)
        var dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.DISPATCH_NOT_FOUND));

        // 2. 기사 검색 서비스 호출
        List<TransporterMatchResponse> drivers = transporterService.findNearbyTransporters(
                dispatch.getStartLatitude(),
                dispatch.getStartLongitude()
        );

        return CommonResponse.success(drivers);
    }
}