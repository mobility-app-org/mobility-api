package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.DispatchSearchDto;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
import com.mobility.api.domain.office.dto.response.GetAllDispatchRes;
import com.mobility.api.domain.office.service.OfficeService;
import com.mobility.api.global.response.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
    @RequestMapping(path = "/dispatch-list", method = RequestMethod.GET)
    public CommonResponse<Page<GetAllDispatchRes>> getAllDispatch(
            @ModelAttribute DispatchSearchDto searchDto, // 필터용 DTO
            Pageable pageable                           // 페이징/정렬용
    ) {
        return CommonResponse.success(officeService.findAllDispatch(searchDto, pageable));
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
    @RequestMapping(path = "/dispatch/{dispatch_id}", method = RequestMethod.PATCH)
    public CommonResponse<Dispatch> updateDispatch(
            @PathVariable("dispatch_id") Long dispatchId,
            @RequestBody UpdateDispatchReq updateDispatchReq
    ) {
        officeService.updateDispatch(dispatchId, updateDispatchReq);
        return CommonResponse.success(null);
    }

    @RequestMapping(path = "/dispatch-cancel/{dispatch_id}", method = RequestMethod.POST)
    public CommonResponse<Object> cancelDispatch(
            @PathVariable("dispatch_id") Long dispatchId
    ) {
        officeService.cancelDispatch(dispatchId);
        return CommonResponse.success(null); // FIXME return값 수정
    }

    @RequestMapping(path = "/billings", method = RequestMethod.GET)
    public CommonResponse<String> getBillings() {

        return CommonResponse.success("프론트 개발 후 작업 예정입니다.");
    }

    @RequestMapping(path = "/statistics", method = RequestMethod.GET)
    public CommonResponse<String> getStatistics() {

        return CommonResponse.success("프론트 개발 후 작업 예정입니다.");
    }

}
