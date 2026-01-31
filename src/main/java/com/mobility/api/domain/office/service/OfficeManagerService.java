package com.mobility.api.domain.office.service;

import com.mobility.api.domain.office.dto.response.ManagerRes;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.repository.ManagerRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfficeManagerService {

    private final ManagerRepository managerRepository;

    /**
     * 직원 목록 조회
     */
    public Page<ManagerRes> getManagers(Manager currentManager, Pageable pageable) {

        // 1. 내 사무실 확인
        Office office = currentManager.getOffice();
        if (office == null) {
            throw new GlobalException(ResultCode.NOT_FOUND_OFFICE);
        }

        // 2. 사무실 소속 직원(Manager) 조회
        Page<Manager> managers = managerRepository.findAllByOffice(office, pageable);

        // 3. DTO 변환
        return managers.map(ManagerRes::from);
    }
}