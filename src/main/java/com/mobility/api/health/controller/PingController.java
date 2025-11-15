package com.mobility.api.health.controller;

import com.mobility.api.global.response.ApiResponse;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.health.entity.Sample;
import com.mobility.api.health.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "통신 테스트")
@RestController
@RequiredArgsConstructor
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

}
