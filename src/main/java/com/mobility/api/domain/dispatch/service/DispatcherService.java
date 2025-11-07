package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.transporter.dto.response.DispatchnRes;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatchRepository dispatchRepository;
    private final TransporterRepository transporterRepository;

    @Transactional
    public DispatchnRes assignDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 배차 정보 조회 -> DB 로우에 락이 걸림
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        // 3. 배차 할당
        dispatch.assignDispatch(transporter);

        return DispatchnRes.from(dispatch);
    }

    @Transactional
    public DispatchnRes cancelDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 현재 배차된 기사가 맞는지
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        dispatch.cancelDispatch(transporter);

        return DispatchnRes.from(dispatch);

    }

    // 기사가 배차를 완료
    @Transactional
    public DispatchnRes completeDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 배차 정보 조회 (동시성 제어를 위해 Lock)
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        dispatch.completeDispatch(transporter);

        return DispatchnRes.from(dispatch);
    }
}
