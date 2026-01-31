package com.mobility.api.domain.office.service;

import com.mobility.api.domain.office.dto.response.ManagerRes;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.enums.ManagerRole;
import com.mobility.api.domain.office.enums.ManagerStatus;
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

    /**
     * 직원 상태 변경
     */
    @Transactional // 상태 변경(Dirty Checking)을 위해 필수
    public void updateManagerStatus(Long targetManagerId, ManagerStatus newStatus, Manager currentManager) {

        // 1. 권한 체크: OWNER(대표)만 직원의 상태를 변경할 수 있음
        if (currentManager.getRole() != ManagerRole.OWNER) {
            throw new GlobalException(ResultCode.FORBIDDEN); // 권한 없음 예외
        }

        // 2. 대상 직원 조회
        Manager targetManager = managerRepository.findById(targetManagerId)
                .orElseThrow(() -> new GlobalException(ResultCode.NOT_FOUND_USER));

        // 3. 소속 사무실 체크: 내 사무실 직원이 맞는지?
        if (!targetManager.getOffice().getId().equals(currentManager.getOffice().getId())) {
            throw new GlobalException(ResultCode.NOT_FOUND_USER); // 보안상 '없음'으로 처리하거나 권한 에러
        }

        // 4. 셀프 변경 방지: 자기 자신을 비활성화해서 로그인 못하게 되는 상황 방지
        if (targetManager.getId().equals(currentManager.getId()) && newStatus == ManagerStatus.INACTIVE) {
            throw new GlobalException(ResultCode.CANNOT_INACTIVATE_SELF);
        }

        // 5. 상태 변경 (Dirty Checking)
        targetManager.updateStatus(newStatus);
    }
}