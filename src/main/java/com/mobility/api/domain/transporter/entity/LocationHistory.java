package com.mobility.api.domain.transporter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.locationtech.jts.geom.Point; // jts 라이브러리

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_history_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transporter_id")
    private Transporter transporter;

    @Column(columnDefinition = "geometry(Point, 4326)")
    private Point location;
}
