package com.mobility.api.domain.office.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.dispatch.service.AutoDispatchService;
import com.mobility.api.domain.office.dto.request.CreateDispatchReq;
import com.mobility.api.domain.office.dto.request.DispatchSearchDto;
import com.mobility.api.domain.office.dto.request.UpdateDispatchReq;
import com.mobility.api.domain.office.dto.response.GetAllDispatchRes;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.transporter.dto.request.TransporterCreateReq;
import com.mobility.api.domain.transporter.dto.response.TransporterRes;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.enums.ApiResponseCode;
import com.mobility.api.global.exception.BusinessException;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OfficeService {

    private final DispatchRepository dispatchRepository;
    private final TransporterRepository transporterRepository;
    private final AutoDispatchService autoDispatchService;

    public Page<GetAllDispatchRes> findAllDispatch(DispatchSearchDto searchDto, Pageable pageable) {

        // Specification 객체 생성 (동적 쿼리 정의)
        Specification<Dispatch> spec = (root, query, cb) -> {
            // cb: CriteriaBuilder, root: Dispatch 엔티티
            List<Predicate> predicates = new ArrayList<>();

            // TODO 임시 조건, 프론트 요구사항에 맞게 수정 필요
            if (searchDto.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), searchDto.getStatus()));
            }
            if (searchDto.getOfficeId() != null) {
                predicates.add(cb.equal(root.get("officeId"), searchDto.getOfficeId()));
            }
            if (searchDto.getKeyword() != null && !searchDto.getKeyword().isBlank()) {
                // 예: 출발지(startLocation) 또는 도착지(destinationLocation)에서 키워드 검색
                predicates.add(
                        cb.or(
                                cb.like(root.get("startLocation"), "%" + searchDto.getKeyword() + "%"),
                                cb.like(root.get("destinationLocation"), "%" + searchDto.getKeyword() + "%")
                        )
                );
            }
            // 모든 'AND' 조건을 조합
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // Repository에 'Specification'과 'Pageable'을 둘 다 전달
        Page<Dispatch> dispatchPage = dispatchRepository.findAll(spec, pageable);

        // Page<Entity> -> Page<Dto>로 변환
        return dispatchPage.map(dispatch -> new GetAllDispatchRes(dispatch)); // DTO 변환
    }

    @Transactional
    public void saveDispatch(CreateDispatchReq createDispatchReq) {
        // 1. 주변 1km 내 자동배차 ON 기사 존재 여부 확인
        boolean hasEligibleDrivers = transporterRepository.existsEligibleDriversWithinRadius(
                createDispatchReq.startLatitude(),
                createDispatchReq.startLongitude()
        );

        // 2. 배차 엔티티 생성
        Dispatch dispatch = createDispatchReq.toEntity();

        // 3. 적격 기사 유무에 따라 상태 결정
        if (hasEligibleDrivers) {
            // 주변에 자동배차 ON 기사가 있음 → HOLD 상태로 시작
            dispatch.setStatus(StatusType.HOLD);
            Dispatch savedDispatch = dispatchRepository.save(dispatch);

            log.info("[Office] 배차 등록 완료 (HOLD) - dispatchId: {}, 주변 적격 기사 있음", savedDispatch.getId());

            // 자동 배차 알림 시작 (비동기)
            autoDispatchService.startSequentialNotification(savedDispatch.getId());

            log.info("[Office] 자동 배차 알림 트리거 완료 - dispatchId: {}", savedDispatch.getId());
        } else {
            // 주변에 자동배차 ON 기사가 없음 → 바로 OPEN 상태
            dispatch.setStatus(StatusType.OPEN);
            Dispatch savedDispatch = dispatchRepository.save(dispatch);

            log.info("[Office] 배차 등록 완료 (OPEN) - dispatchId: {}, 주변 적격 기사 없음", savedDispatch.getId());
        }
    }

    @Transactional
    public void updateDispatch(Long dispatchId,  UpdateDispatchReq req) {

        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.DISPATCH_NOT_FOUND));

        // TODO setter 삭제 후 updateStartLocation 등 메서드 생성하는 것이 좋다고 함 (캡슐화)
        // TODO 전체 update 메서드도 Dispatch 내부에 생성하면 좋을듯
        if (req.startLocation() != null) dispatch.setStartLocation(req.startLocation());
        if (req.destinationLocation() != null) dispatch.setDestinationLocation(req.destinationLocation());
        if (req.charge() != null) dispatch.setCharge(req.charge());
        if (req.clientPhoneNumber() != null) dispatch.setClientPhoneNumber(req.clientPhoneNumber());
        if (req.status() != null) dispatch.setStatus(req.status());
        if (req.call() != null) dispatch.setCall(req.call());
        if (req.active() != null) dispatch.setActive(req.active());
        if (req.service() != null) dispatch.setService(req.service());

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
        dispatch.setCanceledAt(java.time.LocalDateTime.now());

        // @Transactional이 변경 감지(Dirty Checking)로 UPDATE
    }

    @Transactional(readOnly = true)
    public GetDispatchDetailRes getDispatchDetail(Long dispatchId) {
        Dispatch dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new BusinessException(ApiResponseCode.DISPATCH_NOT_FOUND));

        return GetDispatchDetailRes.from(dispatch);
    }

    @Transactional(readOnly = true)
    public DispatchSummaryRes getDispatchSummary() {
        List<Object[]> results = dispatchRepository.countByStatus();

        // 모든 상태를 0으로 초기화
        Map<StatusType, Long> statusCounts = new EnumMap<>(StatusType.class);
        for (StatusType status : StatusType.values()) {
            statusCounts.put(status, 0L);
        }

        // 조회 결과로 업데이트
        for (Object[] row : results) {
            StatusType status = (StatusType) row[0];
            Long count = (Long) row[1];
            statusCounts.put(status, count);
        }

        return DispatchSummaryRes.from(statusCounts);
    }

    /**
     * 기사 등록 (사장님이 호출)
     * @param req 기사 정보
     * @param manager 로그인한 직원 (토큰에서 추출)
     */
    @Transactional
    public void createTransporter(TransporterCreateReq req, Manager manager) {

        // 1. 현재 로그인한 사장님 조회

        // 2. 사장님의 소속 사무실 가져오기
        Office office = manager.getOffice();

        // 3. 기사 전화번호 중복 검사
        if (transporterRepository.existsByPhone(req.phone())) {
            throw new GlobalException(ResultCode.FIXME_FAIL); // 이미 등록된 기사
        }

        // 4. 기사 저장 (사무실 정보 자동 주입)
        Transporter transporter = Transporter.builder()
                .name(req.name())
                .phone(req.phone())
                .isAutoDispatch(req.isAutoDispatch())
                .office(office) // 직원의 사무실을 자동으로 넣어줌
                .build();

        transporterRepository.save(transporter);
    }

    /**
     * 내 사무실 기사 목록 조회
     * @param manager 로그인한 직원
     */
    @Transactional(readOnly = true) // 조회 전용이므로 readOnly 권장 (성능 향상)
    public List<TransporterRes> getMyTransporters(Manager manager) {

        // 1. 관리자(사장님) 찾기

        // 2. 소속 사무실 확인
        Office office = manager.getOffice();
        if (office == null) {
            throw new GlobalException(ResultCode.FIXME_FAIL);
        }

        // 3. 해당 사무실의 기사 리스트 조회
        List<Transporter> transporters = transporterRepository.findAllByOffice(office);

        // 4. Entity List -> DTO List 변환하여 반환
        return transporters.stream()
                .map(TransporterRes::from)
                .toList();
    }

}
