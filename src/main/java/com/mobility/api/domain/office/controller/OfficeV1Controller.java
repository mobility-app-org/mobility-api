package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.DispatchSearchDto;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
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
import org.springframework.data.domain.Pageable;
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
     *     사무실 - 배차 등록
     * </pre>
     * @param createDispatchReq
     */
    @Operation(summary = "배차 등록", description = "")
    @RequestMapping(path = "/dispatch", method = RequestMethod.POST)
    public CommonResponse<Object> createDispatch(
            @Valid @RequestBody CreateDispatchReq createDispatchReq
    ) {
        officeService.saveDispatch(createDispatchReq);

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
            @PathVariable("dispatch_id") Long dispatchId
    ) {
        officeService.cancelDispatch(dispatchId);
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
    public CommonResponse<List<TransporterRes>> getMyTransporters(
            @AuthenticationPrincipal PrincipalDetails user
    ) {
        // userDetails.getUsername() -> 로그인한 관리자의 ID
        List<TransporterRes> result = officeService.getMyTransporters(user.getManager());

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
    public CommonResponse<Integer> changeTransporterStatus(
            @AuthenticationPrincipal PrincipalDetails user,
            @PathVariable Long transporterId,
            @RequestBody TransporterStatusUpdateReq req
    ) {

        officeService.changeTransporterStatus(
                transporterId,
                req.status(),
                user.getManager()
        );


        return CommonResponse.success(0);
    }



}
