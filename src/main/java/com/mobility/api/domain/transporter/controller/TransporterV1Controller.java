package com.mobility.api.domain.transporter.controller;

import com.mobility.api.domain.dispatch.dto.response.CurrentDispatchDetailRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchAssignCompleteRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchListItemRes;
import com.mobility.api.domain.dispatch.enums.StatusType;
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
     * 현재 배차중인 오더 상세 정보 조회
     */
    @Operation(
            summary = "현재 배차중인 오더 상세 정보 조회",
            description = """
                    현재 로그인한 기사가 배차중인 오더의 상세 정보를 조회합니다.

                    - 기사의 dispatchStatus가 DISPATCH 상태일 때만 조회 가능합니다.
                    - EMPTY 상태(배차중인 오더가 없음)인 경우 에러가 반환됩니다.
                    - ASSIGNED 상태의 배차 정보를 반환합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "현재 배차중인 오더 상세 정보 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "배차중인 오더가 없음 (DISPATCH_NOT_ASSIGNED)"
            )
    })
    @GetMapping("/current-dispatch")
    public CommonResponse<CurrentDispatchDetailRes> getCurrentDispatch(
            @io.swagger.v3.oas.annotations.Parameter(hidden = true)
            @CurrentUser Transporter transporter
    ) {
        Long transporterId = getValidatedTransporterId(transporter);
        CurrentDispatchDetailRes currentDispatch = dispatcherService.getCurrentDispatch(transporterId);
        return CommonResponse.success(currentDispatch);
    }

    /**
     * 기사용 배차 리스트 조회 (거리순 정렬 + 상태 필터링)
     *
     * NOTE: 원래 @ModelAttribute + DispatchListSearchReq(Record) 방식을 사용하려 했으나,
     * Java Record는 불변 객체(모든 필드가 final)라서 setter가 없고,
     * Spring의 @ModelAttribute는 전통적으로 기본 생성자 + setter를 통해 바인딩하기 때문에
     * Record와의 호환성 문제로 파라미터 바인딩이 실패했습니다.
     * 따라서 @RequestParam으로 직접 받는 방식으로 구현했습니다.
     */
    @Operation(
            summary = "기사용 배차 리스트 조회 (거리순 정렬 + 상태 필터링)",
            description = """
                    현재 로그인한 기사의 위치를 기준으로 배차를 거리순으로 조회합니다.

                    - 기사의 최신 위치 정보를 기준으로 각 배차의 출발지까지의 직선거리를 계산합니다.
                    - status 파라미터로 특정 상태의 배차만 필터링할 수 있습니다.
                      - 예: ?status=OPEN (OPEN 상태만 조회)
                      - 예: ?status=OPEN&status=ASSIGNED (OPEN, ASSIGNED 상태 조회)
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
                    description = "필터링할 배차 상태 목록 (복수 선택 가능, 미입력 시 전체 조회)",
                    example = "OPEN"
            )
            @RequestParam(required = false) List<String> status
    ) {
        Long transporterId = getValidatedTransporterId(transporter);

        // String을 StatusType enum으로 수동 변환
        List<StatusType> statusTypes = null;
        if (status != null && !status.isEmpty()) {
            statusTypes = status.stream()
                    .map(String::toUpperCase)
                    .map(StatusType::valueOf)
                    .toList();
        }

        List<DispatchListItemRes> dispatchList = dispatcherService.getDispatchListByDistance(transporterId, statusTypes);
        return CommonResponse.success(dispatchList);
    }
}
