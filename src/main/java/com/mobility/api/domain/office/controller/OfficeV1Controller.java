package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
import com.mobility.api.domain.office.service.OfficeService;
import com.mobility.api.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/office")
public class OfficeV1Controller {

    private final OfficeService officeService;

    /**
     * <pre>
     *     사무실 - 배차 리스트 조회
     * </pre>
     * @return
     */
    @RequestMapping(path = "/dispatch-list", method = RequestMethod.GET)
    public ApiResponse<List<Dispatch>> getAllDispatch() {
        // FIXME 사무실, 정렬, 검색 등 조정 필요
        return ApiResponse.success(officeService.findAllDispatch());
    }

    /**
     * <pre>
     *     사무실 - 배차 등록
     * </pre>
     * @param createDispatchReq
     */
    @RequestMapping(path = "/dispatch", method = RequestMethod.POST)
    public void createDispatch(
            @RequestBody CreateDispatchReq createDispatchReq
    ) {
        officeService.saveDispatch(createDispatchReq); // FIXME 응답 수정
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
    public ApiResponse<Dispatch> updateDispatch(
            @PathVariable("dispatch_id") Long dispatchId,
            @RequestBody UpdateDispatchReq updateDispatchReq
    ) {
        officeService.updateDispatch(dispatchId, updateDispatchReq);
        return ApiResponse.success(null); // FIXME return값 수정
    }

}
