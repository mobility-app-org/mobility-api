package com.mobility.api.domain.office.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.dto.req.CreateDispatchReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

}
