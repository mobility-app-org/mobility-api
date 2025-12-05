package com.mobility.api.domain.dispatch.controller;

// DispatchMatchingService 내부에서 TransporterService를 호출한다고 가정
// 혹은 여기서 바로 TransporterService를 호출해도 무방

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

@Tag(name = "Dispatch Matching", description = "배차 매칭 관련 API")
@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
public class DispatchV1Controller {

    private final DispatchRepository dispatchRepository;
    private final TransporterService transporterService;

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