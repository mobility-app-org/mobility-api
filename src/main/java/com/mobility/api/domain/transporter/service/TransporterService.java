package com.mobility.api.domain.transporter.service;

import com.mobility.api.domain.transporter.dto.response.TransporterMatchResponse;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransporterService {

    private final TransporterRepository transporterRepository;

    // 검색 반경 (미터 단위로 변환하여 사용)
    private static final double SEARCH_RADIUS_METERS = 10000.0; // 10km

    public List<TransporterMatchResponse> findNearbyTransporters(double lat, double lon) {
        // Repository 호출 (미터 단위 반경 전달)
        return transporterRepository.findNearbyTransporters(lat, lon, SEARCH_RADIUS_METERS)
                .stream()
                .map(TransporterMatchResponse::from) // km로 변환
                .collect(Collectors.toList());
    }
}