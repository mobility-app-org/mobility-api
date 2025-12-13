package com.mobility.api.domain.transporter.service;

import static org.junit.jupiter.api.Assertions.*;

import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import com.mobility.api.domain.transporter.dto.response.TransporterMatchResponse;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
//import com.mobility.api.global.util.DispatchMockFactory; // 위에서 만든 MockFactory import
import com.mobility.api.global.util.DispatchMockFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TransporterServiceTest {

    @Mock
    private TransporterRepository transporterRepository;

    @InjectMocks
    private TransporterService transporterService;

    @Test
    @DisplayName("기사 위치 검색 시 미터(m) 단위 거리가 km 단위로 소수점 반올림되어 반환된다.")
    void verifyDistanceConversion() {
        // given
        double lat = 37.5547;
        double lon = 126.9707;

        // Mock 데이터 생성 (Factory 활용)
        // 기사1: 1523m 거리 -> 예상 1.52km
        TransporterDistanceProjection driver1 =
                DispatchMockFactory.createTransporterProjection(101L, "김기사", 1523.0);

        // 기사2: 500m 거리 -> 예상 0.5km
        TransporterDistanceProjection driver2 =
                DispatchMockFactory.createTransporterProjection(102L, "이기사", 500.0);

        // Repository Mocking: findNearbyTransporters 호출 시 위 리스트 반환
        given(transporterRepository.findNearbyTransporters(anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(driver1, driver2));

        // when
        List<TransporterMatchResponse> result = transporterService.findNearbyTransporters(lat, lon);

        // then
        assertThat(result).hasSize(2);

        // 첫 번째 기사 검증 (1523m -> 1.52km)
        assertThat(result.get(0).transporterId()).isEqualTo(101L);
        assertThat(result.get(0).distanceKm()).isEqualTo(1.52);

        // 두 번째 기사 검증 (500m -> 0.5km)
        assertThat(result.get(1).transporterId()).isEqualTo(102L);
        assertThat(result.get(1).distanceKm()).isEqualTo(0.5);
    }
}