package com.mobility.api.health.controller;

import com.mobility.api.global.response.ApiResponse;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.global.security.PrincipalDetails;
import com.mobility.api.health.entity.Sample;
import com.mobility.api.health.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "통신 테스트")
@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class PingController {

    private final SampleService sampleService;

    @Operation(summary = "client - server 통신 테스트", description = "성공 시 api 요청 및 응답 정상")
    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public CommonResponse<String> ping() {
        return CommonResponse.success("pong");
//        return ApiResponse.success("pong");
    }

    @Operation(summary = "client - server - db 통신 테스트", description = "성공 시 db 연결도 정상 동작")
    @RequestMapping(path = "/ping-db", method = RequestMethod.GET)
    public CommonResponse<List<Sample>> pingDb() {
        return CommonResponse.success(sampleService.findAll());
//        return ApiResponse.success(sampleService.findAll());
    }

    @Operation(summary = "내 정보 조회 (토큰 테스트)", description = "토큰을 헤더에 넣고 요청하면, 해당 유저의 정보를 반환합니다.")
    @RequestMapping(path = "/me", method = RequestMethod.GET) // GET /api/v1/auth/office/me
    public CommonResponse<String> getMyInfo(@AuthenticationPrincipal PrincipalDetails user) {

        // 토큰이 유효하지 않으면 여기까지 오지도 못함 (Filter에서 막힘)

        if (user == null) {
            return CommonResponse.success("유저 정보 없음 (뭔가 이상함)");
        }

        // [%s] 사무실의
        String info = String.format("안녕하세요! 당신은 [%s]님 이시군요. (권한: %s)",
                user.getManager().getName(),
                user.getManager().getRole()
        );

        return CommonResponse.success(info);
    }

}
