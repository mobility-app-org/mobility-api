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
    NOT_FOUND_USER(HttpStatus.NOT_FOUND, 1001, "사용자를 찾을 수 없습니다."),
    NOT_FOUND_DISPATCH(HttpStatus.NOT_FOUND, 1002, "배차 정보를 찾을 수 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, 1003, "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, 1004, "권한이 없는 사용자입니다."),

    /**
     * 2000번대 (배차 관련)
     */
    DISPATCH_ASSIGN_SUCCESS(HttpStatus.OK, 2001, "배차가 선택 되었습니다"),
    DISPATCH_CANCEL_SUCCESS(HttpStatus.OK, 2002, "배차가 취소 되었습니다"),
    DISPATCH_COMPLETE_SUCCESS(HttpStatus.OK, 2003, "배차가 완료 되었습니다"),
    DISPATCH_NOT_OPEN(HttpStatus.NOT_FOUND, 2004, "배차 상태가 OPEN이 아닙니다."),
    CANNOT_CANCEL_DISPATCH(HttpStatus.BAD_REQUEST, 2005, "배차를 취소할 수 없습니다"),
    CANNOT_COMPLETE_DISPATCH(HttpStatus.BAD_REQUEST, 2006, "배차를 완료할 수 없습니다"),
    DISPATCH_NOT_ASSIGNED(HttpStatus.NOT_FOUND, 2007, "배차 상태가 ASSIGNED이 아닙니다."),
  
    /**
     * 3000번대 (기사 관련)
     */


    /**
     * 4000번대 (사무실 관련)
     */

    FIXME_FAIL(HttpStatus.NOT_FOUND, 9999, "임시 취소 응답 (수정 필요)"),

    /**
     * dev
     */
    DEV_BAD_REQUEST(HttpStatus.BAD_REQUEST, 9998, "DEV: X-Temp-User-Id 헤더가 유효한 숫자가 아닙니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;



}
