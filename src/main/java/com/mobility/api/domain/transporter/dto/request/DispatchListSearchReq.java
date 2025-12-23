package com.mobility.api.domain.transporter.dto.request;

import com.mobility.api.domain.dispatch.enums.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 기사용 배차 리스트 조회 Request DTO
 */
@Schema(description = "배차 리스트 조회 필터")
public record DispatchListSearchReq(
        @Schema(
                description = "필터링할 배차 상태 목록 (복수 선택 가능, 미입력 시 전체 조회)",
                example = "[\"OPEN\", \"ASSIGNED\"]"
        )
        List<StatusType> statuses
) {
    /**
     * 정적 팩토리 메서드
     */
    public static DispatchListSearchReq of(List<StatusType> statuses) {
        return new DispatchListSearchReq(statuses);
    }
}