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
    DISPATCH_TOO_MANY_RESULT(HttpStatus.CONFLICT, 2004, "배차를 조회하는 데 오류가 발생하였습니다."),
    DISPATCH_IS_ALREADY_COMPLETED(HttpStatus.CONFLICT, 2005, "이미 완료된 배차입니다."),
    DISPATCH_NOT_OPEN(HttpStatus.NOT_FOUND, 2006, "배차 상태가 OPEN이 아닙니다."),
    CANNOT_CANCEL_DISPATCH(HttpStatus.BAD_REQUEST, 2007, "배차를 취소할 수 없습니다"),
    CANNOT_COMPLETE_DISPATCH(HttpStatus.BAD_REQUEST, 2008, "배차를 완료할 수 없습니다"),
    DISPATCH_NOT_ASSIGNED(HttpStatus.NOT_FOUND, 2009, "배차 상태가 ASSIGNED이 아닙니다."),
    DISPATCH_NOT_FOUND(HttpStatus.NOT_FOUND, 2010, "배차를 찾을 수 없습니다."),

    /**
     * 3000번대 (기사 관련)
     */
    TRANSPORTER_LOCATION_SAVE_SUCCESS(HttpStatus.OK, 3001, "기사 위도 경도 정보가 저장되었습니다"),
    NOT_FOUND_TRANSPORTER(HttpStatus.NOT_FOUND, 3002, "기사 정보를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCESS(HttpStatus.NOT_FOUND, 3003, "해당 기사 수정 권한이 없습니다."),
    TRANSPORTER_ALREADY_DISPATCHED(HttpStatus.CONFLICT, 3004, "이미 배차중인 오더가 있습니다."),

    /**
     * 4000번대 (사무실 관련)
     */
    NOT_FOUND_OFFICE(HttpStatus.NOT_FOUND, 4001, "사무실 정보를 찾을 수 없습니다."),

    /**
     * 5000번대 (배차 제안 관련)
     */
    OFFER_NOT_FOUND(HttpStatus.NOT_FOUND, 5001, "배차 제안을 찾을 수 없습니다."),
    OFFER_ALREADY_RESPONDED(HttpStatus.CONFLICT, 5002, "이미 응답한 배차 제안입니다."),
    OFFER_EXPIRED(HttpStatus.GONE, 5003, "만료된 배차 제안입니다."),
    DISPATCH_ALREADY_ASSIGNED(HttpStatus.CONFLICT, 5006, "이미 다른 기사에게 할당된 배차입니다."),

    FIXME_FAIL(HttpStatus.NOT_FOUND, 9999, "임시 취소 응답 (수정 필요)"),

    /**
     * 6000번대 (직원 관련)
     */
    CANNOT_INACTIVATE_SELF(HttpStatus.BAD_REQUEST, 6001, "본인 계정은 비활성화할 수 없습니다."),

    /**
     * dev
     */
    DEV_BAD_REQUEST(HttpStatus.BAD_REQUEST, 9998, "DEV: X-Temp-User-Id 헤더가 유효한 숫자가 아닙니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;



}
