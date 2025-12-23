package com.mobility.api.domain.transporter.controller;

import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchAssignCompleteRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchListItemRes;
import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.transporter.dto.request.DispatchListSearchReq;
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

import java.util.List;

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
     * 기사용 배차 리스트 조회 (거리순 정렬 + 상태 필터링)
     */
    @Operation(
            summary = "기사용 배차 리스트 조회 (거리순 정렬 + 상태 필터링)",
            description = """
                    현재 로그인한 기사의 위치를 기준으로 배차를 거리순으로 조회합니다.

                    - 기사의 최신 위치 정보를 기준으로 각 배차의 출발지까지의 직선거리를 계산합니다.
                    - statuses 파라미터로 특정 상태의 배차만 필터링할 수 있습니다.
                      - 예: ?statuses=OPEN (OPEN 상태만 조회)
                      - 예: ?statuses=OPEN&statuses=ASSIGNED (OPEN, ASSIGNED 상태 조회)
                      - 미입력 시 전체 배차 조회
                    - 거리가 가까운 순서대로 정렬되어 반환됩니다.
                    - 거리는 km 단위로 반환됩니다.

                    배차 상태 종류: OPEN(대기), ASSIGNED(배정), COMPLETED(완료), CANCELED(취소)
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "배차 리스트 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "기사의 위치 정보를 찾을 수 없음"
            )
    })
    @GetMapping("/dispatch-list")
    public CommonResponse<List<DispatchListItemRes>> getDispatchList(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @CurrentUser Transporter transporter,
            @io.swagger.v3.oas.annotations.Parameter(
                    description = "필터링할 배차 상태 (복수 선택 가능, 미입력 시 전체 조회)",
                    example = "OPEN"
            )
            @RequestParam(required = false) List<StatusType> statuses
    ) {
        Long transporterId = getValidatedTransporterId(transporter);
        List<DispatchListItemRes> dispatchList = dispatcherService.getDispatchListByDistance(transporterId, statuses);
        return CommonResponse.success(dispatchList);
    }
}
