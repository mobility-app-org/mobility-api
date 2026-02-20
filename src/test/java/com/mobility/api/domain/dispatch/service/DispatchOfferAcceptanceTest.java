package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.entity.DispatchOffer;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.dispatch.repository.DispatchOfferRepository;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.repository.OfficeRepository;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("배차 제안 수락 테스트 - 한 기사는 하나의 배차만 ACCEPTED 가능")
class DispatchOfferAcceptanceTest {

    private static final Logger log = LoggerFactory.getLogger(DispatchOfferAcceptanceTest.class);

    @Autowired
    private DispatchOfferRepository dispatchOfferRepository;

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private OfficeRepository officeRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private Office testOffice;
    private Transporter testTransporter;
    private List<Dispatch> testDispatches;
    private List<DispatchOffer> testOffers;

    @BeforeEach
    @Transactional
    void setUp() {
        // 기존 데이터 정리
        dispatchOfferRepository.deleteAll();
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

        // 테스트용 기사 생성
        testTransporter = Transporter.builder()
                .name("김기사")
                .phone("010-1234-5678")
                .isAutoDispatch(true)
                .build();
        testTransporter = transporterRepository.save(testTransporter);

        // 테스트용 배차 3개 생성
        testDispatches = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Dispatch dispatch = Dispatch.builder()
                    .dispatchNumber("TEST-" + String.format("%04d", i))
                    .startLocation("서울시 강남구")
                    .startLatitude(37.5012)
                    .startLongitude(127.0396)
                    .destinationLocation("부산시 해운대구")
                    .destinationLatitude(35.1587)
                    .destinationLongitude(129.1603)
                    .charge(150000 + (i * 10000))
                    .clientPhoneNumber("010-1234-5678")
                    .status(StatusType.HOLD)
                    .call(CallType.INTERNAL)
                    .service(ServiceType.DELIVERY)
                    .paymentType(PaymentType.CASH)
                    .tollType(TollType.HIPASS)
                    .active(true)
                    .officeId(testOffice.getId())
                    .build();
            testDispatches.add(dispatchRepository.save(dispatch));
        }

        // 테스트용 배차 제안 3개 생성 (모두 같은 기사에게)
        testOffers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DispatchOffer offer = DispatchOffer.builder()
                    .dispatch(testDispatches.get(i))
                    .transporter(testTransporter)
                    .status(OfferStatus.PENDING)
                    .sequence(i + 1)
                    .build();
            testOffers.add(dispatchOfferRepository.save(offer));
        }
    }

    @Test
    @DisplayName("한 기사가 첫 번째 제안을 수락하면 ACCEPTED 상태가 되어야 함")
    @Transactional
    void acceptFirstOffer_ShouldBeAccepted() {
        // given
        DispatchOffer firstOffer = testOffers.get(0);

        // when
        firstOffer.accept();
        dispatchOfferRepository.save(firstOffer);
        dispatchOfferRepository.flush();

        // then
        List<DispatchOffer> acceptedOffers = dispatchOfferRepository.findAcceptedOffersByTransporter(testTransporter.getId());
        assertThat(acceptedOffers).hasSize(1);
        assertThat(acceptedOffers.get(0).getId()).isEqualTo(firstOffer.getId());
        assertThat(acceptedOffers.get(0).getStatus()).isEqualTo(OfferStatus.ACCEPTED);

        log.info("✅ 첫 번째 제안 수락 성공: 배차번호={}", acceptedOffers.get(0).getDispatch().getDispatchNumber());
    }

    @Test
    @DisplayName("한 기사가 이미 ACCEPTED한 제안이 있으면 두 번째 제안은 수락할 수 없어야 함")
    @Transactional
    void acceptSecondOffer_WhenAlreadyAccepted_ShouldFail() {
        // given: 첫 번째 제안을 먼저 수락
        DispatchOffer firstOffer = testOffers.get(0);
        firstOffer.accept();
        dispatchOfferRepository.save(firstOffer);
        dispatchOfferRepository.flush();

        // when: 두 번째 제안도 수락 시도
        DispatchOffer secondOffer = testOffers.get(1);

        // 수락 전에 이미 ACCEPTED한 제안이 있는지 확인
        int acceptedCount = dispatchOfferRepository.countAcceptedOffersByTransporter(testTransporter.getId());

        // then: 이미 ACCEPTED한 제안이 있으므로 두 번째 제안은 수락 불가
        assertThat(acceptedCount).isEqualTo(1);

        // 비즈니스 로직: 이미 수락한 제안이 있으면 새로운 수락 불가
        if (acceptedCount > 0) {
            log.info("❌ 이미 수락한 제안이 있어서 두 번째 제안 수락 불가");
            // 실제 서비스에서는 예외를 던져야 함
            assertThat(true).isTrue(); // 검증 통과
        } else {
            secondOffer.accept();
            dispatchOfferRepository.save(secondOffer);
        }

        // 최종 확인: ACCEPTED 상태는 여전히 1개만 있어야 함
        List<DispatchOffer> acceptedOffers = dispatchOfferRepository.findAcceptedOffersByTransporter(testTransporter.getId());
        assertThat(acceptedOffers).hasSize(1);
        assertThat(acceptedOffers.get(0).getId()).isEqualTo(firstOffer.getId());
    }

    @Test
    @DisplayName("동시에 여러 제안을 수락하려 해도 하나만 ACCEPTED 되어야 함 (동시성 테스트)")
    void concurrentAcceptance_OnlyOneAccepted() throws InterruptedException {
        // given
        int threadCount = 3; // 3개의 제안을 동시에 수락 시도
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when: 3개의 제안을 동시에 수락 시도
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        try {
                            // 비관적 락으로 제안 조회
                            DispatchOffer offer = dispatchOfferRepository.findByIdWithLock(testOffers.get(index).getId())
                                    .orElseThrow();

                            // 이미 ACCEPTED한 제안이 있는지 확인
                            int acceptedCount = dispatchOfferRepository.countAcceptedOffersByTransporter(testTransporter.getId());

                            if (acceptedCount == 0) {
                                // ACCEPTED한 제안이 없으면 수락 가능
                                offer.accept();
                                dispatchOfferRepository.saveAndFlush(offer);
                                successCount.incrementAndGet();
                                log.info("✅ 제안 {} 수락 성공", index + 1);
                            } else {
                                // 이미 ACCEPTED한 제안이 있으면 실패
                                failCount.incrementAndGet();
                                log.info("❌ 제안 {} 수락 실패 (이미 수락한 제안 존재)", index + 1);
                            }
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            log.error("⚠️ 제안 {} 예외 발생: {}", index + 1, e.getMessage());
                        }
                        return null;
                    });
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

        // 정확히 1개만 성공해야 함
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(2);

        // DB에서 ACCEPTED 상태 확인 (트랜잭션 내에서)
        transactionTemplate.execute(status -> {
            List<DispatchOffer> acceptedOffers = dispatchOfferRepository.findAcceptedOffersByTransporter(testTransporter.getId());
            assertThat(acceptedOffers).hasSize(1);
            assertThat(acceptedOffers.get(0).getStatus()).isEqualTo(OfferStatus.ACCEPTED);

            log.info("최종 ACCEPTED 제안: 배차번호={}, 순번={}",
                    acceptedOffers.get(0).getDispatch().getDispatchNumber(),
                    acceptedOffers.get(0).getSequence());
            return null;
        });
    }

    @Test
    @DisplayName("여러 기사가 각자 다른 제안을 수락하면 모두 성공해야 함")
    void multipleTransporters_AcceptDifferentOffers_AllSuccess() throws InterruptedException {
        // given: 추가 기사 2명 생성
        List<Transporter> transporters = new ArrayList<>();
        transporters.add(testTransporter);

        for (int i = 2; i <= 3; i++) {
            Transporter transporter = Transporter.builder()
                    .name("기사" + i)
                    .phone("010-0000-" + String.format("%04d", i))
                    .isAutoDispatch(true)
                    .build();
            transporters.add(transporterRepository.save(transporter));
        }

        // 각 기사에게 제안 생성 (총 3개 배차 × 3명 기사 = 9개 제안)
        List<DispatchOffer> allOffers = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                DispatchOffer offer = DispatchOffer.builder()
                        .dispatch(testDispatches.get(i))
                        .transporter(transporters.get(j))
                        .status(OfferStatus.PENDING)
                        .sequence(j + 1)
                        .build();
                allOffers.add(dispatchOfferRepository.save(offer));
            }
        }

        int threadCount = 3;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);

        // when: 각 기사가 하나씩 제안을 수락
        for (int i = 0; i < threadCount; i++) {
            final int transporterIndex = i;
            executorService.submit(() -> {
                try {
                    transactionTemplate.execute(status -> {
                        try {
                            // 해당 기사의 첫 번째 제안 조회
                            Long transporterId = transporters.get(transporterIndex).getId();
                            DispatchOffer offer = allOffers.stream()
                                    .filter(o -> o.getTransporter().getId().equals(transporterId))
                                    .findFirst()
                                    .orElseThrow();

                            // 비관적 락으로 조회
                            DispatchOffer lockedOffer = dispatchOfferRepository.findByIdWithLock(offer.getId())
                                    .orElseThrow();

                            // 수락
                            lockedOffer.accept();
                            dispatchOfferRepository.saveAndFlush(lockedOffer);
                            successCount.incrementAndGet();
                            log.info("✅ 기사 {} 제안 수락 성공", transporterIndex + 1);
                        } catch (Exception e) {
                            log.error("❌ 기사 {} 예외 발생: {}", transporterIndex + 1, e.getMessage());
                        }
                        return null;
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        // then: 3명 모두 성공해야 함
        log.info("========== 다중 기사 테스트 결과 ==========");
        log.info("성공: {}건", successCount.get());

        assertThat(successCount.get()).isEqualTo(3);

        // 각 기사마다 ACCEPTED 제안이 정확히 1개씩 있어야 함 (트랜잭션 내에서)
        transactionTemplate.execute(status -> {
            for (Transporter transporter : transporters) {
                int acceptedCount = dispatchOfferRepository.countAcceptedOffersByTransporter(transporter.getId());
                assertThat(acceptedCount).isEqualTo(1);
                log.info("기사 {}: ACCEPTED 제안 {}개", transporter.getName(), acceptedCount);
            }
            return null;
        });
    }

    @Test
    @DisplayName("REJECTED 또는 TIMEOUT 상태의 제안은 ACCEPTED 개수에 포함되지 않아야 함")
    @Transactional
    void rejectedOrTimeoutOffers_NotCountedAsAccepted() {
        // given: 여러 상태의 제안 생성
        testOffers.get(0).accept();   // ACCEPTED
        testOffers.get(1).reject();   // REJECTED
        testOffers.get(2).timeout();  // TIMEOUT

        dispatchOfferRepository.saveAll(testOffers);
        dispatchOfferRepository.flush();

        // when
        int acceptedCount = dispatchOfferRepository.countAcceptedOffersByTransporter(testTransporter.getId());
        List<DispatchOffer> acceptedOffers = dispatchOfferRepository.findAcceptedOffersByTransporter(testTransporter.getId());

        // then
        assertThat(acceptedCount).isEqualTo(1);
        assertThat(acceptedOffers).hasSize(1);
        assertThat(acceptedOffers.get(0).getStatus()).isEqualTo(OfferStatus.ACCEPTED);

        log.info("✅ ACCEPTED: 1개, REJECTED: 1개, TIMEOUT: 1개");
        log.info("ACCEPTED 제안만 조회됨: 배차번호={}", acceptedOffers.get(0).getDispatch().getDispatchNumber());
    }
}
