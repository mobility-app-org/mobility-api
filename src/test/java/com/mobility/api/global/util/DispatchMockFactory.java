package com.mobility.api.global.util;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.transporter.dto.TransporterDistanceProjection;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.Mockito;

public class DispatchMockFactory {

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    /**
     * 가짜 배차(Dispatch) 엔티티 생성
     * 예: 서울역 (37.5547, 126.9707)
     */
    public static Dispatch createDispatch(Long id) {
        return Dispatch.builder()
                .id(id)
                .startLocation("서울역")
                .startLatitude(37.5547)  // 서울역 위도
                .startLongitude(126.9707) // 서울역 경도
                .build();
    }

    /**
     * 가짜 기사 위치 정보(Projection) 생성 - Interface Mocking
     * 예: 남산타워 기사 (서울역에서 약 1.5km 거리)
     */
    public static TransporterDistanceProjection createTransporterProjection(
            Long id, String name, double distanceMeters) {

        // Interface는 new로 생성할 수 없으므로 Mockito.mock() 사용
        TransporterDistanceProjection mock = Mockito.mock(TransporterDistanceProjection.class);

        Mockito.when(mock.getId()).thenReturn(id);
        Mockito.when(mock.getName()).thenReturn(name);
        Mockito.when(mock.getPhone()).thenReturn("010-1234-5678");
        Mockito.when(mock.getDistanceInMeters()).thenReturn(distanceMeters);

        return mock;
    }

    /**
     * (참고용) 실제 Point 객체 생성이 필요할 경우 사용
     */
    public static Point createPoint(double lat, double lon) {
        return geometryFactory.createPoint(new Coordinate(lon, lat));
    }
}