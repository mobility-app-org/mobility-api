package com.mobility.api.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *     2xxx : 성공 / 4xxx : 요청 실패 / 5xxx : 서버 오류
 *     x1xx : 배차 관련
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum ApiResponseCode {

    // success ===================

    SUCCESS(2000, "성공"),

    // request error (4xxx) ==============

    BAD_REQUEST(4000, "요청 실패"),
    DISPATCH_NOT_FOUND(4100, "해당 배차 정보를 찾을 수 없습니다."),

    // server error (5xxx) ===============

    SERVER_ERROR(5000, "서버 오류"),
    DB_ERROR(5001, "DB 오류");

    // ============================

    private final int code;
    private final String message;

}