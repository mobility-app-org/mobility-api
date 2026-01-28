package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.dto.request.CancelDispatchReq;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.DispatchSearchDto;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
import com.mobility.api.domain.office.dto.response.DispatchFeedRes;
import com.mobility.api.domain.office.dto.response.DispatchSummaryRes;
import com.mobility.api.domain.office.dto.response.GetAllDispatchRes;
import com.mobility.api.domain.office.dto.response.GetDispatchDetailRes;
import com.mobility.api.domain.office.service.OfficeService;
import com.mobility.api.domain.transporter.dto.request.TransporterCreateReq;
import com.mobility.api.domain.transporter.dto.request.TransporterStatusUpdateReq;
import com.mobility.api.domain.transporter.dto.response.TransporterRes;
import com.mobility.api.global.annotation.SwaggerPageable;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "사무실 관련 요청(/api/v1/office/...)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/office")
public class OfficeV1Controller {

    private final OfficeService officeService;

    /**
     * <pre>
     *     사무실 - 배차 리스트 조회
     * </pre>
     *
     * @return
     */
    @Operation(summary = "배차 리스트 조회", description = "")
    @SwaggerPageable
    @RequestMapping(path = "/dispatch-list", method = RequestMethod.GET)
    public CommonResponse<Page<GetAllDispatchRes>> getAllDispatch(
            @ModelAttribute DispatchSearchDto searchDto, // 필터용 DTO
            @Parameter(hidden = true) Pageable pageable  // 페이징/정렬용
    ) {
        return CommonResponse.success(officeService.findAllDispatch(searchDto, pageable));
    }

    /**
     * <pre>
     *     사무실 - 배차 상세 조회
     * </pre>
     *
     * @param dispatchId
     * @return
     */
    @Operation(summary = "배차 상세 조회", description = "")
    @RequestMapping(path = "/dispatch/{dispatch_id}", method = RequestMethod.GET)
    public CommonResponse<GetDispatchDetailRes> getDispatchDetail(
            @PathVariable("dispatch_id") Long dispatchId
    ) {
        return CommonResponse.success(officeService.getDispatchDetail(dispatchId));
    }

    /**
     * <pre>
     *     사무실 - 배차 상태별 카운트 조회
     * </pre>
     *
     * @return 상태별 배차 개수
     */
    @Operation(summary = "배차 상태별 카운트 조회", description = "OPEN, ASSIGNED, COMPLETED, CANCELED 상태별 배차 개수를 조회합니다.")
    @RequestMapping(path = "/dispatch/summary", method = RequestMethod.GET)
    public CommonResponse<DispatchSummaryRes> getDispatchSummary() {
        return CommonResponse.success(officeService.getDispatchSummary());
    }

    /**
     * <pre>
     *     사무실 - 대시보드 실시간 피드 조회
     * </pre>
     *
     * @param limit 조회 개수 (기본: 20)
     * @return 최근 배차 이벤트 피드 목록
     */
    @Operation(
            summary = "대시보드 실시간 피드 조회",
            description = """
                    최근 배차 이벤트를 시간순으로 조회합니다.

                    **반환되는 배차 상태 (4가지):**
                    - `open`: 배차 등록 (대기 중)
                    - `assigned`: 배차 할당 (기사 배정 완료)
                    - `completed`: 운송 완료
                    - `canceled`: 배차 취소

                    **제외되는 상태:**
                    - `HOLD`: 자동배차 진행 중 상태는 요구사항에 따라 피드에서 제외됩니다.
                      (HOLD는 임시 상태로, 최대 50초 이내에 OPEN 또는 ASSIGNED로 전환됨)

                    **특징:**
                    - 현재 로그인한 사무실의 배차만 조회됩니다 (officeId 필터링)
                    - 최신순 정렬 (createdAt DESC)
                    - Transporter 정보 포함 (N+1 최적화 적용)
                    - transporterName은 assigned/completed 타입에만 값이 있고, open/canceled는 null

                    **응답 예시:**
                    ```json
                    {
                      "code": "SUCCESS",
                      "data": [
                        {
                          "id": "feed-01",
                          "type": "assigned",
                          "dispatchId": 123,
                          "dispatchNumber": "2024-0001",
                          "transporterName": "김철수",
                          "message": "김철수 기사가 콜 #2024-0001을 배차 받았습니다",
                          "timestamp": "2024-01-15T10:32:00"
                        },
                        {
                          "id": "feed-02",
                          "type": "open",
                          "dispatchId": 124,
                          "dispatchNumber": "2024-0002",
                          "transporterName": null,
                          "message": "배차 #2024-0002가 등록되었습니다",
                          "timestamp": "2024-01-15T10:30:00"
                        }
                      ]
                    }
                    ```
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            )
    })
    @RequestMapping(path = "/dispatch/feed", method = RequestMethod.GET)
    public CommonResponse<List<DispatchFeedRes>> getDispatchFeed(
            @Parameter(
                    description = "조회할 피드 개수 (기본값: 20, 최대 권장: 100)",
                    example = "20",
                    required = false
            )
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        return CommonResponse.success(officeService.getDispatchFeed(limit, user.getManager()));
    }

    /**
     * <pre>
     *     사무실 - 배차 등록
     * </pre>
     * @param createDispatchReq
     */
    @Operation(summary = "배차 등록", description = "")
    @RequestMapping(path = "/dispatch", method = RequestMethod.POST)
    public CommonResponse<Object> createDispatch(
            @AuthenticationPrincipal PrincipalDetails user,
            @Valid @RequestBody CreateDispatchReq createDispatchReq
    ) {

        officeService.saveDispatch(createDispatchReq, user.getManager());

        return CommonResponse.success(null);
    }

    /**
     * <pre>
     *     사무실 - 배차 수정
     * </pre>
     * @param dispatchId
     * @param updateDispatchReq
     * @return
     */
    @Operation(summary = "배차 수정", description = "")
    @RequestMapping(path = "/dispatch/{dispatch_id}", method = RequestMethod.PATCH)
    public CommonResponse<Dispatch> updateDispatch(
            @AuthenticationPrincipal PrincipalDetails user,
            @PathVariable("dispatch_id") Long dispatchId,
            @RequestBody UpdateDispatchReq updateDispatchReq
    ) {
        officeService.updateDispatch(dispatchId, updateDispatchReq);
        return CommonResponse.success(null);
    }

    /**
     * <pre>
     *     배차 취소
     * </pre>
     * @param dispatchId
     * @return
     */
    @Operation(summary = "배차 취소 (삭제)", description = "")
    @RequestMapping(path = "/dispatch-cancel/{dispatch_id}", method = RequestMethod.POST)
    public CommonResponse<Object> cancelDispatch(
            @AuthenticationPrincipal PrincipalDetails user,
            @PathVariable("dispatch_id") Long dispatchId,
            @RequestBody CancelDispatchReq req
    ) {
        officeService.cancelDispatch(dispatchId, req, user.getManager());
        return CommonResponse.success(null); // FIXME return값 수정
    }

    @Operation(summary = "거래 내역 :: 임시 보류", description = "")
    @RequestMapping(path = "/billings", method = RequestMethod.GET)
    public CommonResponse<String> getBillings() {

        return CommonResponse.success("프론트 개발 후 작업 예정입니다.");
    }

    @Operation(summary = "통계 :: 임시 보류", description = "")
    @RequestMapping(path = "/statistics", method = RequestMethod.GET)
    public CommonResponse<String> getStatistics() {

        return CommonResponse.success("프론트 개발 후 작업 예정입니다.");
    }

    /**
     * <pre>
     *     기사 등록
     * </pre>
     * @param req
     * @param user
     * @return
     */
    @Operation(summary = "기사 등록", description = "")
    @RequestMapping(path = "/transporter", method = RequestMethod.POST)
    public CommonResponse<String> createTransporter(
            @RequestBody TransporterCreateReq req,
            @AuthenticationPrincipal PrincipalDetails user // 👈 토큰에서 사용자 정보 추출
    ) {

        // userDetails.getUsername()에는 토큰에 넣었던 subject(loginId)가 들어있습니다.
        officeService.createTransporter(req, user.getManager());

        return CommonResponse.success("기사 등록 성공");
    }

    /**
     * <pre>
     *     기사 리스트 조회
     * </pre>
     * @param user
     * @return
     */
    @Operation(summary = "기사 리스트 조회", description = "")
    @RequestMapping(path = "/transporter", method = RequestMethod.GET)
    public CommonResponse<Page<TransporterRes>> getMyTransporters(
            @AuthenticationPrincipal PrincipalDetails user,
            @RequestParam(required = false) String status,     // 필터: 없을 수도 있음
            @RequestParam(defaultValue = "0") int page,        // 페이지: 안 보내면 0
            @RequestParam(defaultValue = "20") int size        // 크기: 안 보내면 20
    ) {

        // 페이징 객체 생성 (최신순 정렬 예시: id 내림차순)
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        // userDetails.getUsername() -> 로그인한 관리자의 ID
//        List<TransporterRes> result = officeService.getMyTransporters(user.getManager());

        // 서비스 호출
        Page<TransporterRes> result = officeService.getMyTransporters(
                user.getManager(),
                status,
                pageable
        );

        return CommonResponse.success(result);
    }

    /**
     * <pre>
     *     기사 상태 변경
     * </pre>
     * @param user
     * @return
     */
    @Operation(summary = "기사 상태 변경", description = "")
    @RequestMapping(path = "/transporter/{transporterId}/status", method = RequestMethod.PATCH)
    public CommonResponse<String> changeTransporterStatus(
            @AuthenticationPrincipal PrincipalDetails user,
            @PathVariable Long transporterId,
            @RequestBody TransporterStatusUpdateReq req
    ) {

        officeService.changeTransporterStatus(
                transporterId,
                req.status(),
                user.getManager()
        );

        return CommonResponse.success("기사 상태 변경 성공");
    }

    /**
     * <pre>
     *     배차 노출범위 변경
     * </pre>
     * @param user
     * @return
     */
    @Operation(summary = "배차 노출범위 변경", description = "")
    @RequestMapping(path = "/dispatch/{dispatchId}/exposure", method = RequestMethod.PATCH)
    public CommonResponse<String> changeDispatchExposure(
            @AuthenticationPrincipal PrincipalDetails user,
            @PathVariable Long dispatchId
    ) {

        // 서비스 호출 및 결과 받기
        String changedStatus = officeService.changeDispatchExposure(dispatchId, user.getManager());

        // 변경된 상태를 메시지나 데이터로 주면 프론트에서 UI 갱신하기 편함
        return CommonResponse.success(changedStatus);
    }

}
