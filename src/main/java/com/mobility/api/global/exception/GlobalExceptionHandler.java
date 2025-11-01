package com.mobility.api.global.exception;

import com.mobility.api.global.enums.ApiResponseCode;
import com.mobility.api.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 직접 정의한 BusinessException 처리
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST) // <- HTTP 상태 코드도 설정 (예: 400) // TODO BusinessException - 500 에러도 추가 필요할듯
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: code={}, message={}",
                e.getStatus().getCode(), e.getStatus().getMessage());

        return ApiResponse.fail(e.getStatus());
    }

    // 예상치 못한 모든 서버 오류 처리 (Catch-All)
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR) // <- HTTP 500
    public ApiResponse<Void> handleException(Exception e) {
        // 이 오류는 심각한 문제일 수 있으니 Error 레벨로 로깅
        log.error("Unhandled Exception: ", e);

        return ApiResponse.fail(ApiResponseCode.SERVER_ERROR);
    }

}
