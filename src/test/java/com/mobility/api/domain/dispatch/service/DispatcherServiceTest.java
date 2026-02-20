package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.dispatch.dto.DispatchDistanceProjection;
import com.mobility.api.domain.dispatch.dto.response.CurrentDispatchDetailRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchAssignCompleteRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchCancelRes;
import com.mobility.api.domain.dispatch.dto.response.DispatchListItemRes;
import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.dispatch.repository.DispatchRepository;
import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.domain.office.repository.OfficeRepository;
import com.mobility.api.domain.transporter.DispatchStatus;
import com.mobility.api.domain.transporter.entity.LocationHistory;
import com.mobility.api.domain.transporter.entity.Transporter;
import com.mobility.api.domain.transporter.repository.LocationRepository;
import com.mobility.api.domain.transporter.repository.TransporterRepository;
import com.mobility.api.global.exception.GlobalException;
import com.mobility.api.global.response.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DispatcherService 테스트")
class DispatcherServiceTest {

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private TransporterRepository transporterRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private OfficeRepository officeRepository;

    @InjectMocks
    private DispatcherService dispatcherService;

    private GeometryFactory geometryFactory;
    private Transporter testTransporter;
    private Dispatch testDispatch;
    private Office testOffice;

    @BeforeEach
    void setUp() {
        geometryFactory = new GeometryFactory();

        // 테스트용 사무실
        testOffice = Office.builder()
                .officeName("테스트 사무실")
                .officeRegistrationNumber("123-45-67890")
                .officeAddress("서울시 강남구")
                .officeTelNumber("02-1234-5678")
                .build();
        setId(testOffice, 1L);

        // 테스트용 기사
        testTransporter = Transporter.builder()
                .id(1L)
                .name("김철수")
                .phone("010-1234-5678")
                .isAutoDispatch(true)
                .dispatchStatus(DispatchStatus.EMPTY)
                .build();

        // 테스트용 배차
        testDispatch = Dispatch.builder()
                .id(100L)
                .dispatchNumber("2024-0001")
                .startLocation("서울시 강남구")
                .startLatitude(37.5000)
                .startLongitude(127.0400)
                .destinationLocation("부산시 해운대구")
                .destinationLatitude(35.1587)
                .destinationLongitude(129.1603)
                .charge(150000)
                .clientPhoneNumber("010-9876-5432")
                .status(StatusType.OPEN)
                .call(CallType.INTERNAL)
                .service(ServiceType.DELIVERY)
                .paymentType(PaymentType.CASH)
                .tollType(TollType.HIPASS)
                .active(true)
                .officeId(1L)
                .build();
    }

    @Test
    @DisplayName("배차 할당 성공")
    void assignDispatch_Success() {
        // given
        given(transporterRepository.findByIdWithPessimisticLock(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findByIdWithPessimisticLock(100L))
                .willReturn(Optional.of(testDispatch));

        // when
        DispatchAssignCompleteRes result = dispatcherService.assignDispatch(100L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(testDispatch.getStatus()).isEqualTo(StatusType.ASSIGNED);
        assertThat(testDispatch.getTransporter()).isEqualTo(testTransporter);
        assertThat(testTransporter.getDispatchStatus()).isEqualTo(DispatchStatus.DISPATCH);
    }

    @Test
    @DisplayName("배차 할당 실패 - 기사를 찾을 수 없음")
    void assignDispatch_TransporterNotFound() {
        // given
        given(transporterRepository.findByIdWithPessimisticLock(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dispatcherService.assignDispatch(100L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.NOT_FOUND_USER);
    }

    @Test
    @DisplayName("배차 할당 실패 - 이미 배차중인 기사")
    void assignDispatch_TransporterAlreadyDispatched() {
        // given
        Transporter busyTransporter = Transporter.builder()
                .id(1L)
                .name("바쁜기사")
                .phone("010-1111-1111")
                .dispatchStatus(DispatchStatus.DISPATCH) // 이미 배차중
                .build();

        given(transporterRepository.findByIdWithPessimisticLock(1L))
                .willReturn(Optional.of(busyTransporter));

        // when & then
        assertThatThrownBy(() -> dispatcherService.assignDispatch(100L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.TRANSPORTER_ALREADY_DISPATCHED);
    }

    @Test
    @DisplayName("배차 할당 실패 - 배차를 찾을 수 없음")
    void assignDispatch_DispatchNotFound() {
        // given
        given(transporterRepository.findByIdWithPessimisticLock(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findByIdWithPessimisticLock(100L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dispatcherService.assignDispatch(100L, 1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.NOT_FOUND_DISPATCH);
    }

    @Test
    @DisplayName("배차 취소 성공")
    void cancelDispatch_Success() {
        // given
        testDispatch.assignDispatch(testTransporter); // 먼저 배차 할당

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findByIdWithPessimisticLock(100L))
                .willReturn(Optional.of(testDispatch));

        // when
        DispatchCancelRes result = dispatcherService.cancelDispatch(100L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(testDispatch.getStatus()).isEqualTo(StatusType.OPEN);
        assertThat(testDispatch.getTransporter()).isNull();
        assertThat(testTransporter.getDispatchStatus()).isEqualTo(DispatchStatus.EMPTY);
    }

    @Test
    @DisplayName("배차 완료 성공")
    void completeDispatch_Success() {
        // given
        testDispatch.assignDispatch(testTransporter); // 먼저 배차 할당
        testTransporter.changeDispatchStatus(DispatchStatus.DISPATCH);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findByIdWithPessimisticLock(100L))
                .willReturn(Optional.of(testDispatch));

        // when
        DispatchAssignCompleteRes result = dispatcherService.completeDispatch(100L, 1L);

        // then
        assertThat(result).isNotNull();
        assertThat(testDispatch.getStatus()).isEqualTo(StatusType.COMPLETED);
        assertThat(testDispatch.getCompletedAt()).isNotNull();
        assertThat(testTransporter.getDispatchStatus()).isEqualTo(DispatchStatus.EMPTY);
    }

    @Test
    @DisplayName("기사용 배차 리스트 조회 - 거리순 정렬")
    void getDispatchListByDistance_Success() {
        // given
        Point location = createPoint(127.0276, 37.4979);
        LocationHistory locationHistory = LocationHistory.builder()
                .id(1L)
                .location(location)
                .build();

        testTransporter.changeDispatchStatus(DispatchStatus.EMPTY);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(locationRepository.findFirstByTransporter_IdOrderByIdDesc(1L))
                .willReturn(Optional.of(locationHistory));

        // Mock DispatchDistanceProjection
        DispatchDistanceProjection projection = mock(DispatchDistanceProjection.class);
        given(projection.getId()).willReturn(100L);
        given(projection.getServiceType()).willReturn("DELIVERY");
        given(projection.getCharge()).willReturn(150000);
        given(projection.getStartLocation()).willReturn("서울시 강남구");
        given(projection.getDestinationLocation()).willReturn("부산시 해운대구");
        given(projection.getStatus()).willReturn("OPEN");
        given(projection.getDistanceInMeters()).willReturn(500.0);

        given(dispatchRepository.findDispatchesByDistanceAndStatus(
                anyDouble(), anyDouble(), any()
        )).willReturn(List.of(projection));

        // when
        List<DispatchListItemRes> result = dispatcherService.getDispatchListByDistance(
                1L, List.of(StatusType.OPEN)
        );

        // then
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(100L);
    }

    @Test
    @DisplayName("배차 리스트 조회 실패 - 이미 배차중인 기사")
    void getDispatchListByDistance_TransporterAlreadyDispatched() {
        // given
        testTransporter.changeDispatchStatus(DispatchStatus.DISPATCH);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));

        // when & then
        assertThatThrownBy(() -> dispatcherService.getDispatchListByDistance(1L, null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.TRANSPORTER_ALREADY_DISPATCHED);
    }

    @Test
    @DisplayName("배차 리스트 조회 실패 - 기사 위치 정보 없음")
    void getDispatchListByDistance_LocationNotFound() {
        // given
        testTransporter.changeDispatchStatus(DispatchStatus.EMPTY);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(locationRepository.findFirstByTransporter_IdOrderByIdDesc(1L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dispatcherService.getDispatchListByDistance(1L, null))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.NOT_FOUND_USER);
    }

    @Test
    @DisplayName("현재 배차중인 오더 조회 성공")
    void getCurrentDispatch_Success() {
        // given
        testDispatch.assignDispatch(testTransporter);
        testTransporter.changeDispatchStatus(DispatchStatus.DISPATCH);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findFirstByTransporterIdAndStatusOrderByAssignedAtDesc(1L, StatusType.ASSIGNED))
                .willReturn(Optional.of(testDispatch));
        given(officeRepository.findById(1L))
                .willReturn(Optional.of(testOffice));

        // when
        CurrentDispatchDetailRes result = dispatcherService.getCurrentDispatch(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.status()).isEqualTo(StatusType.ASSIGNED);
        assertThat(result.officeTelNumber()).isEqualTo("02-1234-5678");
    }

    @Test
    @DisplayName("현재 배차중인 오더 조회 실패 - 배차중인 오더 없음 (EMPTY 상태)")
    void getCurrentDispatch_NoCurrentDispatch() {
        // given
        testTransporter.changeDispatchStatus(DispatchStatus.EMPTY);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));

        // when & then
        assertThatThrownBy(() -> dispatcherService.getCurrentDispatch(1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.DISPATCH_NOT_ASSIGNED);
    }

    @Test
    @DisplayName("현재 배차중인 오더 조회 실패 - ASSIGNED 배차를 찾을 수 없음")
    void getCurrentDispatch_AssignedDispatchNotFound() {
        // given
        testTransporter.changeDispatchStatus(DispatchStatus.DISPATCH);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findFirstByTransporterIdAndStatusOrderByAssignedAtDesc(1L, StatusType.ASSIGNED))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> dispatcherService.getCurrentDispatch(1L))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCode.DISPATCH_NOT_ASSIGNED);
    }

    @Test
    @DisplayName("현재 배차중인 오더 조회 - 사무실 정보 없을 때 officeTelNumber는 null")
    void getCurrentDispatch_NoOfficeInfo() {
        // given
        testDispatch.assignDispatch(testTransporter);
        testTransporter.changeDispatchStatus(DispatchStatus.DISPATCH);

        given(transporterRepository.findById(1L))
                .willReturn(Optional.of(testTransporter));
        given(dispatchRepository.findFirstByTransporterIdAndStatusOrderByAssignedAtDesc(1L, StatusType.ASSIGNED))
                .willReturn(Optional.of(testDispatch));
        given(officeRepository.findById(1L))
                .willReturn(Optional.empty()); // 사무실 정보 없음

        // when
        CurrentDispatchDetailRes result = dispatcherService.getCurrentDispatch(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.officeTelNumber()).isNull();
    }

    // === 헬퍼 메서드 ===

    private Point createPoint(double lon, double lat) {
        Point point = geometryFactory.createPoint(new Coordinate(lon, lat));
        point.setSRID(4326);
        return point;
    }

    private void setId(Object entity, Long id) {
        try {
            java.lang.reflect.Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set id", e);
        }
    }
}
