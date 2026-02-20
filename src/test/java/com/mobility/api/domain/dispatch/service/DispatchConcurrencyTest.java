package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.repository.OfficeRepository;
import com.mobility.api.domain.transporter.DispatchStatus;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("배차 할당 동시성 테스트")
class DispatchConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(DispatchConcurrencyTest.class);

    @Autowired
    private DispatcherService dispatcherService;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private OfficeRepository officeRepository;

    private Office testOffice;
    private Dispatch testDispatch;
    private List<Transporter> testTransporters;

    @BeforeEach
    @Transactional
    void setUp() {
        // 기존 데이터 정리
        dispatchRepository.deleteAll();
        transporterRepository.deleteAll();
        officeRepository.deleteAll();

        // 테스트용 사무실 생성
        testOffice = Office.builder()
                .officeName("테스트 사무실")
                .officeRegistrationNumber("123-45-67890")
                .officeAddress("서울시 강남구")
                .officeTelNumber("02-1234-5678")
                .build();
        testOffice = officeRepository.save(testOffice);

        // 테스트용 배차 생성 (OPEN 상태)
        testDispatch = Dispatch.builder()
                .dispatchNumber("TEST-0001")
                .startLocation("서울시 강남구")
                .startLatitude(37.5012)
                .startLongitude(127.0396)
                .destinationLocation("부산시 해운대구")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(150000)
                .clientPhoneNumber("010-1234-5678")
                .status(StatusType.OPEN)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .paymentType(PaymentType.CASH)
                .tollType(TollType.HIPASS)
                .active(true)
                .officeId(testOffice.getId())
                .build();
        testDispatch = dispatchRepository.save(testDispatch);

        // 테스트용 기사 10명 생성
        testTransporters = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            Transporter transporter = Transporter.builder()
                    .name("기사" + i)
                    .phone("010-0000-" + String.format("%04d", i))
                    .isAutoDispatch(true)
                    .build();
            testTransporters.add(transporterRepository.save(transporter));
        }
    }

    @Test
    @DisplayName("동시에 10명의 기사가 같은 배차를 할당받으려 할 때 1명만 성공해야 함")
    void concurrentDispatchAssignment_OnlyOneSuccess() throws InterruptedException {
        // given
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<Exception> exceptions = new CopyOnWriteArrayList<>();

        // when: 10명의 기사가 동시에 배차 할당 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Long transporterId = testTransporters.get(index).getId();
                    dispatcherService.assignDispatch(testDispatch.getId(), transporterId);
                    successCount.incrementAndGet();
                    log.info("✅ 기사 {} 배차 할당 성공", index + 1);
                } catch (GlobalException e) {
                    failCount.incrementAndGet();
                    exceptions.add(e);
                    log.warn("❌ 기사 {} 배차 할당 실패: {}", index + 1, e.getMessage());
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    exceptions.add(e);
                    log.error("⚠️ 기사 {} 예외 발생: {}", index + 1, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then
        log.info("========== 동시성 테스트 결과 ==========");
        log.info("성공: {}건, 실패: {}건", successCount.get(), failCount.get());
        log.info("예외 목록: {}", exceptions.stream().map(e -> e.getClass().getSimpleName() + ": " + e.getMessage()).toList());

        // 정확히 1명만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);

        // DB에서 배차 상태 확인 (Transporter와 함께 조회)
        Dispatch updatedDispatch = dispatchRepository.findByIdWithTransporter(testDispatch.getId()).orElseThrow();
        assertThat(updatedDispatch.getStatus()).isEqualTo(StatusType.ASSIGNED);
        assertThat(updatedDispatch.getTransporter()).isNotNull();

        log.info("배차 ID: {}, 할당된 기사: {}, 상태: {}",
                updatedDispatch.getId(),
                updatedDispatch.getTransporter() != null ? updatedDispatch.getTransporter().getName() : "없음",
                updatedDispatch.getStatus());
    }

    @Test
    @DisplayName("동시에 여러 기사가 다른 배차를 할당받으면 모두 성공해야 함")
    void concurrentDispatchAssignment_DifferentDispatches_AllSuccess() throws InterruptedException {
        // given: 추가 배차 9개 생성 (총 10개)
        List<Dispatch> dispatches = new ArrayList<>();
        dispatches.add(testDispatch);
        for (int i = 2; i <= 10; i++) {
            Dispatch dispatch = Dispatch.builder()
                    .dispatchNumber("TEST-" + String.format("%04d", i))
                    .startLocation("서울시 강남구")
                    .startLatitude(37.5012)
                    .startLongitude(127.0396)
                    .destinationLocation("부산시 해운대구")
                    .destinationLatitude(35.1587)
                    .destinationLongitude(129.1603)
                    .charge(150000)
                    .clientPhoneNumber("010-1234-5678")
                    .status(StatusType.OPEN)
                    .call(CallType.INTERNAL)
                    .service(ServiceType.DELIVERY)
                    .paymentType(PaymentType.CASH)
                    .tollType(TollType.HIPASS)
                    .active(true)
                    .officeId(testOffice.getId())
                    .build();
            dispatches.add(dispatchRepository.save(dispatch));
        }

        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 10명의 기사가 각각 다른 배차를 동시에 할당받으려 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Long transporterId = testTransporters.get(index).getId();
                    Long dispatchId = dispatches.get(index).getId();
                    dispatcherService.assignDispatch(dispatchId, transporterId);
                    successCount.incrementAndGet();
                    log.info("✅ 기사 {} 배차 {} 할당 성공", index + 1, index + 1);
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    log.error("❌ 기사 {} 배차 할당 실패: {}", index + 1, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 모두 성공해야 함
        log.info("========== 다중 배차 동시성 테스트 결과 ==========");
        log.info("성공: {}건, 실패: {}건", successCount.get(), failCount.get());

        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(0);

        // 모든 배차가 ASSIGNED 상태인지 확인
        long assignedCount = dispatchRepository.findAll().stream()
                .filter(d -> d.getStatus() == StatusType.ASSIGNED)
                .count();
        assertThat(assignedCount).isEqualTo(10);
    }

    @Test
    @DisplayName("이미 배차중인 기사가 다른 배차를 할당받으려 하면 실패해야 함")
    void assignDispatch_TransporterAlreadyDispatched_ShouldFail() throws InterruptedException {
        // given: 첫 번째 배차를 기사 1에게 할당
        Transporter transporter1 = testTransporters.get(0);
        dispatcherService.assignDispatch(testDispatch.getId(), transporter1.getId());

        // 두 번째 배차 생성
        Dispatch secondDispatch = Dispatch.builder()
                .dispatchNumber("TEST-0002")
                .startLocation("서울시 강남구")
                .startLatitude(37.5012)
                .startLongitude(127.0396)
                .destinationLocation("부산시 해운대구")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(180000)
                .clientPhoneNumber("010-1234-5678")
                .status(StatusType.OPEN)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .paymentType(PaymentType.CASH)
                .tollType(TollType.HIPASS)
                .active(true)
                .officeId(testOffice.getId())
                .build();
        secondDispatch = dispatchRepository.save(secondDispatch);

        // when & then: 이미 배차중인 기사가 두 번째 배차를 할당받으려 하면 실패
        Long secondDispatchId = secondDispatch.getId();
        try {
            dispatcherService.assignDispatch(secondDispatchId, transporter1.getId());
            assertThat(false).isTrue(); // 여기 도달하면 안 됨
        } catch (GlobalException e) {
            log.info("✅ 예상대로 실패: {}", e.getMessage());
            assertThat(e.getResultCode().getCode()).isEqualTo(3004); // TRANSPORTER_ALREADY_DISPATCHED
        }

        // 두 번째 배차는 여전히 OPEN 상태여야 함
        Dispatch updatedSecondDispatch = dispatchRepository.findById(secondDispatchId).orElseThrow();
        assertThat(updatedSecondDispatch.getStatus()).isEqualTo(StatusType.OPEN);
        assertThat(updatedSecondDispatch.getTransporter()).isNull();
    }

    @Test
    @DisplayName("비관적 락 타임아웃 테스트 - 3초 이상 대기 시 예외 발생")
    void pessimisticLock_Timeout_ShouldThrowException() throws InterruptedException {
        // given
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch firstThreadLatch = new CountDownLatch(1);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // when: 첫 번째 스레드가 락을 획득하고 5초간 대기
        executorService.submit(() -> {
            try {
                startLatch.await();
                dispatcherService.assignDispatch(testDispatch.getId(), testTransporters.get(0).getId());
                Thread.sleep(5000); // 5초간 트랜잭션 유지
                firstThreadLatch.countDown();
            } catch (Exception e) {
                log.error("첫 번째 스레드 예외: {}", e.getMessage());
            }
        });

        // 두 번째 스레드가 같은 배차에 대해 락 획득 시도 (타임아웃 예상)
        executorService.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(100); // 첫 번째 스레드가 락을 먼저 획득하도록 약간 대기
                dispatcherService.assignDispatch(testDispatch.getId(), testTransporters.get(1).getId());
            } catch (Exception e) {
                exceptionCount.incrementAndGet();
                log.info("⏱️ 예상대로 타임아웃 예외 발생: {}", e.getMessage());
            }
        });

        startLatch.countDown(); // 두 스레드 동시 시작
        firstThreadLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();
        executorService.awaitTermination(15, TimeUnit.SECONDS);

        // then: 두 번째 스레드는 타임아웃으로 실패해야 함
        log.info("타임아웃 예외 발생 횟수: {}", exceptionCount.get());
        assertThat(exceptionCount.get()).isGreaterThan(0);
    }
}
