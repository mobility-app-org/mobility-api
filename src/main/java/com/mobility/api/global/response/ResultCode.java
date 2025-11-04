package com.mobility.api.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    /**
     * 1000번대 (글로벌)
     */
    SUCCESS(HttpStatus.OK, 0, "정상 처리 되었습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, 1000, "잘못된 입력값입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 1003, "사용자를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1006, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 1007, "권한이 없는 사용자입니다."),

    FIXME_FAIL(HttpStatus.NOT_FOUND, 9999, "임시 취소 응답 (수정 필요)");

    private final HttpStatus status;
    private final int code;
    private final String message;

}
