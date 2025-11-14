package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.transporter.dto.request.LocationUpdateReq;
import com.mobility.api.domain.transporter.dto.response.LocationUpdateRes;
import com.mobility.api.domain.transporter.entity.LocationHistory;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.LocationRepository;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final TransporterRepository transporterRepository;

    @Transactional
    public void processLocationUpdate(Long transporterId, @Valid LocationUpdateReq req) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. LocationHistory 엔티티 생성
        LocationHistory locationHistory = LocationHistory.of(
                transporter,
                req.latitude(),
                req.longitude()
        );

        locationRepository.save(locationHistory);
    }
}
