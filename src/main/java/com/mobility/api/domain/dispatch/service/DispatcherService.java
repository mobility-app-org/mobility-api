package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchDetailRes;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.dispatch.dto.response.DispatchAssignCompleteRes;
import com.mobility.api.domain.transporter.entity.LocationHistory;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.LocationRepository;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatchRepository dispatchRepository;
    private final TransporterRepository transporterRepository;
    private final LocationRepository locationRepository;

    @Transactional
    public DispatchAssignCompleteRes assignDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 배차 정보 조회 -> DB 로우에 락이 걸림
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        // 3. 배차 할당
        dispatch.assignDispatch(transporter);

        return DispatchAssignCompleteRes.from(dispatch);
    }

    @Transactional
    public DispatchCancelRes cancelDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 현재 배차된 기사가 맞는지
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        dispatch.cancelDispatch(transporter);

        return DispatchCancelRes.from(dispatch);
    }

    @Transactional
    public DispatchAssignCompleteRes completeDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 배차 정보 조회
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        dispatch.completeDispatch(transporter);

        return DispatchAssignCompleteRes.from(dispatch);
    }

    /**
     * 배차 상세 조회
     * @param dispatchId 배차 ID
     * @param currentUserId 현재 로그인한 기사 ID
     * @return DispatchDetailRes (배차 정보 + 현재 기사와의 거리)
     */
    public DispatchDetailRes getDispatchDetail(Long dispatchId, Long currentUserId) {
        // 1. 배차 정보 조회
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        // 2. 현재 로그인한 기사의 최신 위치 조회 및 거리 계산
        Double distanceKm = null;
        if (currentUserId != null) {
            // LocationHistory 테이블에서 기사의 가장 최근 위치 조회
            var latestLocationOpt = locationRepository.findFirstByTransporter_IdOrderByIdDesc(currentUserId);
            if (latestLocationOpt.isPresent()) {
                LocationHistory locationHistory = latestLocationOpt.get();
                // 최신 위치가 있으면 출발지와의 거리 계산
                distanceKm = calculateDistance(
                        dispatch.getStartLatitude(),
                        dispatch.getStartLongitude(),
                        locationHistory.getLocation()
                );
            }
        }

        // 3. DTO 변환 및 반환
        return DispatchDetailRes.from(dispatch, distanceKm);
    }

    /**
     * PostGIS를 사용하여 두 지점 간 거리 계산 (km 단위)
     * @param startLat 출발지 위도
     * @param startLon 출발지 경도
     * @param transporterLocation 기사 현재 위치 (PostGIS Point)
     * @return 거리 (km)
     */
    private Double calculateDistance(double startLat, double startLon, org.locationtech.jts.geom.Point transporterLocation) {
        // TransporterRepository에 거리 계산 쿼리가 있지만, 단일 지점 간 계산이 필요하므로
        // 여기서는 JTS 라이브러리를 사용하여 직접 계산
        // (또는 Repository에 별도 메서드를 추가할 수 있음)

        // JTS Point의 X는 경도(longitude), Y는 위도(latitude)
        double transporterLat = transporterLocation.getY();
        double transporterLon = transporterLocation.getX();

        // Haversine 공식을 사용한 거리 계산 (미터 단위)
        double earthRadiusKm = 6371.0; // 지구 반지름 (km)

        double dLat = Math.toRadians(transporterLat - startLat);
        double dLon = Math.toRadians(transporterLon - startLon);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(startLat)) * Math.cos(Math.toRadians(transporterLat)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return earthRadiusKm * c; // km 단위로 반환
    }
}
