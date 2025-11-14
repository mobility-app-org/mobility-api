package com.mobility.api.domain.transporter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point; // jts 라이브러리
import org.locationtech.jts.geom.PrecisionModel;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory {

    // GeometryFactory는 스레드 안전하며, SRID 4326용으로 하나만 만들어 재사용하는 것이 효율적
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;

    // 정적 팩토리 메서드
    public static LocationHistory of(Transporter transporter, Double latitude, Double longitude) {

        // JTS Coordinate 객체 생성 (x, y) -> (경도, 위도) 순서
        Coordinate coordinate = new Coordinate(latitude, longitude);

        // GeometryFactory를 사용하여 Point 객체 생성
        Point point = geometryFactory.createPoint(coordinate);

        return LocationHistory.builder()
                .transporter(transporter)
                .location(point)
                .build();
    }
}
