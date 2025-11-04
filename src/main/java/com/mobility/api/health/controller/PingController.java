package com.mobility.api.health.controller;

import com.mobility.api.global.response.ApiResponse;
import com.mobility.api.global.response.CommonResponse;
import com.mobility.api.health.entity.Sample;
import com.mobility.api.health.service.SampleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PingController {

    private final SampleService sampleService;

    @RequestMapping(path = "/ping", method = RequestMethod.GET)
    public CommonResponse<String> ping() {
        return CommonResponse.success("pong");
//        return ApiResponse.success("pong");
    }

    @RequestMapping(path = "/ping-db", method = RequestMethod.GET)
    public CommonResponse<List<Sample>> pingDb() {
        return CommonResponse.success(sampleService.findAll());
//        return ApiResponse.success(sampleService.findAll());
    }

}
