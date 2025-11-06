package com.mobility.api.health.service;

import com.mobility.api.global.enums.ApiResponseCode;
import com.mobility.api.global.exception.BusinessException;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import com.mobility.api.health.entity.Sample;
import com.mobility.api.health.repository.SampleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;

    public List<Sample> findAll() {

        // exception test
//        if(1 > 0) {
////            throw new RuntimeException();
////            throw new BusinessException(ApiResponseCode.SERVER_ERROR);
//            throw new GlobalException(ResultCode.NOT_FOUND_USER);
//        }

        return sampleRepository.findAll();
    }

}
