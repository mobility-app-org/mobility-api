package com.mobility.api.domain.office.controller;

import com.mobility.api.domain.office.dto.response.ManagerRes;
import com.mobility.api.domain.office.service.OfficeManagerService;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.security.PrincipalDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사무실 - 직원 관련 요청(/api/v1/office/employee/**)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/office/employee")
public class OfficeManagerV1Controller {

    // 새로 분리한 서비스 주입
    private final OfficeManagerService officeManagerService;

    /**
     * <pre>
     * 직원 리스트 조회
     * </pre>
     * @param user 로그인한 관리자
     * @param page 페이지 번호 (0부터)
     * @param size 페이지 크기
     * @return
     */
    @Operation(summary = "직원 리스트 조회", description = "사무실에 소속된 직원(Manager) 목록을 조회합니다.")
    @GetMapping("")
    public CommonResponse<Page<ManagerRes>> getManagers(
            @AuthenticationPrincipal PrincipalDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        // 최신순 or 이름순 정렬
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<ManagerRes> result = officeManagerService.getManagers(user.getManager(), pageable);

        return CommonResponse.success(result);
    }

}
