package com.mobility.api.domain.office.dto.request;

import com.mobility.api.domain.dispatch.enums.StatusType;
import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *     배차 조회 - 검색, 필터 등 필요한 조건 // TODO: 프론트 요구사항에 맞게 수정 필요
 * </pre>
 */
@Getter
@Setter
public class DispatchSearchDto {

    private String keyword; // 검색어 (예: 출발지, 도착지 동시 검색)
    private StatusType status; // 배차 상태 필터
    private Long officeId;
}
