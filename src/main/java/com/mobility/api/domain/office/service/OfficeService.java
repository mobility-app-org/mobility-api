package com.mobility.api.domain.office.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
import com.mobility.api.global.enums.ApiResponseCode;
import com.mobility.api.global.exception.BusinessException;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OfficeService {

    private final DispatchRepository dispatchRepository;

    public List<Dispatch> findAllDispatch() {
        return dispatchRepository.findAll();
    }

    public void saveDispatch(CreateDispatchReq createDispatchReq) {
        dispatchRepository.save(createDispatchReq.toEntity());
    }

    @Transactional
    public void updateDispatch(Long dispatchId,  UpdateDispatchReq updateDispatchReq) {

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.DISPATCH_NOT_FOUND));

        // TODO setter 삭제 후 updateStartLocation 등 메서드 생성하는 것이 좋다고 함 (캡슐화)
        // TODO 전체 update 메서드도 Dispatch 내부에 생성하면 좋을듯
        if (updateDispatchReq.getStartLocation() != null) dispatch.setStartLocation(updateDispatchReq.getStartLocation());
        if (updateDispatchReq.getDestinationLocation() != null) dispatch.setDestinationLocation(updateDispatchReq.getDestinationLocation());
        if (updateDispatchReq.getCharge() != null) dispatch.setCharge(updateDispatchReq.getCharge());
        if (updateDispatchReq.getClientPhoneNumber() != null) dispatch.setClientPhoneNumber(updateDispatchReq.getClientPhoneNumber());
        if (updateDispatchReq.getStatus() != null) dispatch.setStatus(updateDispatchReq.getStatus());
        if (updateDispatchReq.getCall() != null) dispatch.setCall(updateDispatchReq.getCall());
        if (updateDispatchReq.getActive() != null) dispatch.setActive(updateDispatchReq.getActive());
        if (updateDispatchReq.getService() != null) dispatch.setService(updateDispatchReq.getService());

        // 메서드가 종료될 때, @Transactional이 변경된 내용을 감지(Dirty Checking)하여 자동으로 DB에 UPDATE 쿼리를 실행 (save() 호출 불필요)
    }

    @Transactional
    public void cancelDispatch(Long dispatchId) { // <- 메서드 이름도 delete -> cancel로 변경

        // 엔티티 조회
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new GlobalException(ResultCode.DISPATCH_TOO_MANY_RESULT));

        // (방어 로직) 이미 완료된 배차는 취소(삭제)할 수 없도록 막기
        if (dispatch.getStatus() == StatusType.COMPLETED) {
            throw new GlobalException(ResultCode.DISPATCH_IS_ALREADY_COMPLETED);
        }

        // TODO: dispatch.cancel() 같은 엔티티 메서드로 캡슐화
        dispatch.setStatus(StatusType.CANCELED);

        // @Transactional이 변경 감지(Dirty Checking)로 UPDATE
    }

}
