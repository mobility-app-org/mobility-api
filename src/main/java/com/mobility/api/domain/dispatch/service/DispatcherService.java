package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.transporter.dto.request.DispatchAssignReq;
import com.mobility.api.domain.transporter.dto.response.DispatchAssignRes;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatchRepository dispatchRepository;
    private final TransporterRepository transporterRepository;

    // 기사가 배차를 선택
    public DispatchAssignRes assignDispatch(Long dispatchId, Long transporterId) {

        // 1. 기사 정보 조회
        Transporter transporter = transporterRepository.findById(transporterId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 2. 배차 정보 조회 -> DB 로우에 락이 걸림
        Dispatch dispatch = dispatchRepository.findByIdWithPessimisticLock(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_DISPATCH));

        // 3. 배차 할당
        dispatch.assignDispatch(transporter);

        return DispatchAssignRes.from(dispatch);
    }

}
