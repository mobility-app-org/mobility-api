package com.mobility.api.domain.transporter.entity;

import com.mobility.api.domain.office.entity.Office;
import com.mobility.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SuperBuilder
@Table(name = "transporters")
public class Transporter extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transporter_id")
    private Long id;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "phone")
    private String phone;

    // 실시간 위치 검색용 필드 (PostGIS Point)
    // columnDefinition을 통해 SRID 4326(위경도)임을 명시
    @Column(name = "current_location", columnDefinition = "geometry(Point, 4326)")
    private Point currentLocation;

    @Column(name = "is_auto_dispatch")
    private boolean isAutoDispatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")    // DB 컬럼명: office_id
    private Office office;
}
