package com.mobility.api.global.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResponseStatus {

    // success ===================

    SUCCESS(2000, "성공"),

    // request error ==============

    BAD_REQUEST(4000, "요청 실패"),

    // server error ===============

    SERVER_ERROR(5000, "서버 오류"),
    DB_ERROR(5001, "DB 오류");

    // ============================

    private final int code;
    private final String message;

}