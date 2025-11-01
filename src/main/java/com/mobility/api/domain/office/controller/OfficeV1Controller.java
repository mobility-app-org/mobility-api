package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.service.OfficeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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
    public List<Dispatch> getAllDispatch() {
        // FIXME 사무실, 정렬, 검색 등 조정 필요
        return officeService.findAllDispatch();
    }

    @RequestMapping(path = "/dispatch", method = RequestMethod.POST)
    public void createDispatch(CreateDispatchReq createDispatchReq) {
        // FIXME nullCheck 필요
        officeService.saveDispatch(createDispatchReq);
    }

}
