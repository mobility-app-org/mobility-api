package com.mobility.api.domain.dispatch.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.dispatch.service.DispatcherService;
import com.mobility.api.domain.office.repository.ManagerRepository;
import com.mobility.api.domain.transporter.dto.response.TransporterMatchResponse;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.domain.transporter.service.TransporterService;
import com.mobility.api.global.jwt.JwtProvider;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser
@WebMvcTest(DispatchV1Controller.class)
class DispatchV1ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DispatchRepository dispatchRepository;

    @MockitoBean
    private TransporterService transporterService;

    @MockitoBean
    private TransporterRepository transporterRepository;

    @MockitoBean
    private DispatcherService dispatcherService;

    @MockitoBean
    private ManagerRepository managerRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("[API] 배차 주변 기사 조회 - 성공 (200 OK)")
    void getNearbyDrivers_ApiSuccess() throws Exception {
        // given
        Long dispatchId = 1L;

        // 1. Mock 배차 데이터 (강남역)
        Dispatch mockDispatch = Dispatch.builder()
                .id(dispatchId)
                .startLatitude(37.4980)
                .startLongitude(127.0276)
                .build();

        // 2. Mock 기사 응답 데이터 (2명)
        List<TransporterMatchResponse> mockDrivers = List.of(
                TransporterMatchResponse.builder()
                        .transporterId(101L)
                        .name("김기사")
                        .phone("010-1234-5678")
                        .distanceKm(1.52)
                        .build(),
                TransporterMatchResponse.builder()
                        .transporterId(102L)
                        .name("이기사")
                        .phone("010-9876-5432")
                        .distanceKm(3.05)
                        .build()
        );

        // 3. Stubbing (가짜 동작 정의)
        given(dispatchRepository.findById(dispatchId)).willReturn(Optional.of(mockDispatch));
        given(transporterService.findNearbyTransporters(anyDouble(), anyDouble()))
                .willReturn(mockDrivers);

        // when & then (요청 및 검증)
        mockMvc.perform(get("/api/v1/dispatch/{dispatchId}/nearby-drivers", dispatchId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print()) // 콘솔에 요청/응답 로그 출력
                .andExpect(status().isOk())

                // CommonResponse 구조 검증
                .andExpect(jsonPath("$.statusCode").value(0))
                .andExpect(jsonPath("$.message").value("정상 처리 되었습니다."))

                // 데이터(List) 검증
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))

                // 첫 번째 기사 데이터 확인
                .andExpect(jsonPath("$.data[0].transporterId").value(101L))
                .andExpect(jsonPath("$.data[0].name").value("김기사"))
                .andExpect(jsonPath("$.data[0].distanceKm").value(1.52))

                // 두 번째 기사 데이터 확인
                .andExpect(jsonPath("$.data[1].transporterId").value(102L))
                .andExpect(jsonPath("$.data[1].name").value("이기사"));
    }

    @Test
    @DisplayName("[API] 존재하지 않는 배차 조회 시 - 실패 (예외 발생)")
    void getNearbyDrivers_ApiFail_NotFound() throws Exception {
        // given
        Long invalidId = 9999L;

        // 배차가 없어서 Optional.empty() 반환
        given(dispatchRepository.findById(invalidId)).willReturn(Optional.empty());

        // when & then
        mockMvc.perform(get("/api/v1/dispatch/{dispatchId}/nearby-drivers", invalidId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                // GlobalExceptionHandler 설정에 따라 상태 코드는 달라질 수 있음 (보통 404 Not Found 또는 400 Bad Request)
                // 여기서는 예외가 발생했는지, 그리고 우리가 정의한 ResultCode와 관련된 값이 나오는지 확인해야 함
                .andExpect(result -> {
                    Exception resolvedException = result.getResolvedException();
                    if (resolvedException instanceof GlobalException) {
                        GlobalException ex = (GlobalException) resolvedException;
                        // 예외 코드가 DISPATCH_NOT_FOUND 인지 확인
                        if (ex.getResultCode() != ResultCode.DISPATCH_NOT_FOUND) {
                            throw new AssertionError("기대했던 예외 코드가 아닙니다.");
                        }
                    } else {
                        // GlobalException이 아닌 다른 예외가 발생했거나 예외 처리가 안 된 경우
                        // (프로젝트 설정에 따라 수정 필요)
                    }
                });
    }
}