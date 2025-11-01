package com.mobility.api.global.exception;

import com.mobility.api.global.enums.ApiResponseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ApiResponseCode status;


    public BusinessException(ApiResponseCode status) {
        super(status.getMessage());
        this.status = status;
    }
}
