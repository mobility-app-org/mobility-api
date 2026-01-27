package com.mobility.api.domain.office.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.StatusType;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.dto.response.DispatchFeedRes;
import com.mobility.api.domain.office.entity.Manager;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.enums.ManagerRole;
import com.mobility.api.domain.transporter.entity.Transporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OfficeServiceTest {

    @Mock
    private DispatchRepository dispatchRepository;

    @InjectMocks
    private OfficeService officeService;

    private Manager testManager;
    private Office testOffice;
    private Office otherOffice;

    @BeforeEach
    void setUp() throws Exception {
        // 테스트 사무실 1 (ID: 1)
        testOffice = Office.builder()
                .officeName("테스트 사무실")
                .officeRegistrationNumber("123-45-67890")
                .officeAddress("서울시 강남구")
                .officeTelNumber("02-1234-5678")
                .build();

        // Reflection으로 id 설정
        java.lang.reflect.Field officeIdField = Office.class.getDeclaredField("id");
        officeIdField.setAccessible(true);
        officeIdField.set(testOffice, 1L);

        // 테스트 매니저 (사무실 1 소속)
        testManager = Manager.builder()
                .loginId("testmanager")
                .password("encodedPassword")
                .name("테스트 관리자")
                .phone("010-1234-5678")
                .email("test@example.com")
                .role(ManagerRole.OWNER)
                .office(testOffice)
                .build();

        // Reflection으로 id 설정
        java.lang.reflect.Field managerIdField = Manager.class.getDeclaredField("id");
        managerIdField.setAccessible(true);
        managerIdField.set(testManager, 1L);

        // 다른 사무실 2 (ID: 2)
        otherOffice = Office.builder()
                .officeName("다른 사무실")
                .officeRegistrationNumber("987-65-43210")
                .officeAddress("서울시 서초구")
                .officeTelNumber("02-9876-5432")
                .build();

        // Reflection으로 id 설정
        officeIdField.set(otherOffice, 2L);
    }

    @Test
    @DisplayName("[Service] 배차 피드 조회 - 현재 사무실의 배차만 필터링")
    void getDispatchFeed_FilterByOfficeId() {
        // given
        Integer limit = 20;

        Transporter transporter1 = Transporter.builder()
                .id(1L)
                .name("김철수")
                .phone("010-1111-2222")
                .build();

        Transporter transporter2 = Transporter.builder()
                .id(2L)
                .name("이영희")
                .phone("010-3333-4444")
                .build();

        // Repository에서 반환될 배차 목록 (여러 사무실 혼재)
        List<Dispatch> allDispatches = List.of(
                // 사무실 1의 배차 (testOffice) - 포함되어야 함
                Dispatch.builder()
                        .id(101L)
                        .officeId(1L)  // testOffice
                        .dispatchNumber("2024-0001")
                        .status(StatusType.ASSIGNED)
                        .transporter(transporter1)
                        .assignedAt(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                        .createdAt(LocalDateTime.of(2024, 1, 15, 10, 0, 0))
                        .build(),

                // 사무실 2의 배차 (otherOffice) - 필터링되어야 함
                Dispatch.builder()
                        .id(102L)
                        .officeId(2L)  // otherOffice
                        .dispatchNumber("2024-0002")
                        .status(StatusType.OPEN)
                        .createdAt(LocalDateTime.of(2024, 1, 15, 10, 25, 0))
                        .build(),

                // 사무실 1의 배차 (testOffice) - 포함되어야 함
                Dispatch.builder()
                        .id(103L)
                        .officeId(1L)  // testOffice
                        .dispatchNumber("2024-0003")
                        .status(StatusType.COMPLETED)
                        .transporter(transporter2)
                        .completedAt(LocalDateTime.of(2024, 1, 15, 10, 20, 0))
                        .createdAt(LocalDateTime.of(2024, 1, 15, 9, 50, 0))
                        .build(),

                // HOLD 상태 배차 (사무실 1) - 필터링되어야 함
                Dispatch.builder()
                        .id(104L)
                        .officeId(1L)  // testOffice
                        .dispatchNumber("2024-0004")
                        .status(StatusType.HOLD)
                        .createdAt(LocalDateTime.of(2024, 1, 15, 10, 15, 0))
                        .build(),

                // 사무실 1의 배차 (testOffice) - 포함되어야 함
                Dispatch.builder()
                        .id(105L)
                        .officeId(1L)  // testOffice
                        .dispatchNumber("2024-0005")
                        .status(StatusType.CANCELED)
                        .canceledAt(LocalDateTime.of(2024, 1, 15, 10, 10, 0))
                        .createdAt(LocalDateTime.of(2024, 1, 15, 9, 40, 0))
                        .build()
        );

        Page<Dispatch> dispatchPage = new PageImpl<>(allDispatches);

        given(dispatchRepository.findAllWithTransporter(any(Pageable.class)))
                .willReturn(dispatchPage);

        // when
        List<DispatchFeedRes> result = officeService.getDispatchFeed(limit, testManager);

        // then
        // 1. 결과 개수 검증: 사무실 1의 배차만 포함되고 HOLD 상태는 제외
        // ID: 101(ASSIGNED), 103(COMPLETED), 105(CANCELED) = 3개
        assertThat(result).hasSize(3);

        // 2. 사무실 1의 배차만 포함되었는지 검증
        assertThat(result)
                .extracting(DispatchFeedRes::dispatchId)
                .containsExactly(101L, 103L, 105L);

        // 3. 사무실 2의 배차(ID: 102)는 제외되었는지 확인
        assertThat(result)
                .extracting(DispatchFeedRes::dispatchId)
                .doesNotContain(102L);

        // 4. HOLD 상태 배차(ID: 104)는 제외되었는지 확인
        assertThat(result)
                .extracting(DispatchFeedRes::dispatchId)
                .doesNotContain(104L);

        // 5. 각 피드의 타입 검증
        assertThat(result.get(0).type()).isEqualTo("assigned");
        assertThat(result.get(1).type()).isEqualTo("completed");
        assertThat(result.get(2).type()).isEqualTo("canceled");
    }

    @Test
    @DisplayName("[Service] 배차 피드 조회 - HOLD 상태는 항상 제외")
    void getDispatchFeed_ExcludeHoldStatus() {
        // given
        Integer limit = 20;

        List<Dispatch> allDispatches = List.of(
                Dispatch.builder()
                        .id(101L)
                        .officeId(1L)
                        .dispatchNumber("2024-0001")
                        .status(StatusType.HOLD)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Dispatch.builder()
                        .id(102L)
                        .officeId(1L)
                        .dispatchNumber("2024-0002")
                        .status(StatusType.OPEN)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Page<Dispatch> dispatchPage = new PageImpl<>(allDispatches);

        given(dispatchRepository.findAllWithTransporter(any(Pageable.class)))
                .willReturn(dispatchPage);

        // when
        List<DispatchFeedRes> result = officeService.getDispatchFeed(limit, testManager);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).dispatchId()).isEqualTo(102L);
        assertThat(result.get(0).type()).isEqualTo("open");
    }

    @Test
    @DisplayName("[Service] 배차 피드 조회 - 빈 결과 반환")
    void getDispatchFeed_EmptyResult() {
        // given
        Integer limit = 20;
        Page<Dispatch> emptyPage = new PageImpl<>(List.of());

        given(dispatchRepository.findAllWithTransporter(any(Pageable.class)))
                .willReturn(emptyPage);

        // when
        List<DispatchFeedRes> result = officeService.getDispatchFeed(limit, testManager);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[Service] 배차 피드 조회 - 다른 사무실 배차는 모두 제외")
    void getDispatchFeed_FilterOutAllOtherOfficeDispatches() {
        // given
        Integer limit = 20;

        // 모든 배차가 다른 사무실의 것
        List<Dispatch> allDispatches = List.of(
                Dispatch.builder()
                        .id(201L)
                        .officeId(2L)  // otherOffice
                        .dispatchNumber("2024-0001")
                        .status(StatusType.OPEN)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Dispatch.builder()
                        .id(202L)
                        .officeId(3L)  // another office
                        .dispatchNumber("2024-0002")
                        .status(StatusType.ASSIGNED)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Page<Dispatch> dispatchPage = new PageImpl<>(allDispatches);

        given(dispatchRepository.findAllWithTransporter(any(Pageable.class)))
                .willReturn(dispatchPage);

        // when
        List<DispatchFeedRes> result = officeService.getDispatchFeed(limit, testManager);

        // then
        // 모든 배차가 다른 사무실의 것이므로 결과는 비어있어야 함
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("[Service] 배차 피드 조회 - 피드 ID 연번 생성 검증")
    void getDispatchFeed_FeedIdSequence() {
        // given
        Integer limit = 20;

        List<Dispatch> allDispatches = List.of(
                Dispatch.builder()
                        .id(101L)
                        .officeId(1L)
                        .dispatchNumber("2024-0001")
                        .status(StatusType.OPEN)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Dispatch.builder()
                        .id(102L)
                        .officeId(1L)
                        .dispatchNumber("2024-0002")
                        .status(StatusType.ASSIGNED)
                        .createdAt(LocalDateTime.now())
                        .build(),
                Dispatch.builder()
                        .id(103L)
                        .officeId(1L)
                        .dispatchNumber("2024-0003")
                        .status(StatusType.COMPLETED)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        Page<Dispatch> dispatchPage = new PageImpl<>(allDispatches);

        given(dispatchRepository.findAllWithTransporter(any(Pageable.class)))
                .willReturn(dispatchPage);

        // when
        List<DispatchFeedRes> result = officeService.getDispatchFeed(limit, testManager);

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).id()).isEqualTo("feed-01");
        assertThat(result.get(1).id()).isEqualTo("feed-02");
        assertThat(result.get(2).id()).isEqualTo("feed-03");
    }
}
