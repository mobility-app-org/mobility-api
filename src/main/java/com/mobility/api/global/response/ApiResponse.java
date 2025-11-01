package com.mobility.api.global.response;

import com.mobility.api.global.enums.ApiResponseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T result;

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
        this.result = null;
    }

    public ApiResponse(ApiResponseCode apiResponseCode, T result) {
        this.code = apiResponseCode.getCode();
        this.message = apiResponseCode.getMessage();
        this.result = result;
    }

    /**
     * <pre>
     *     성공(2000) 응답
     * </pre>
     * @param result
     * @return
     * @param <T>
     */
    public static <T> ApiResponse<T> success(T result) {
        return new ApiResponse<>(ApiResponseCode.SUCCESS, result);
    }


}
