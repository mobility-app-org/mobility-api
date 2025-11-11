package com.mobility.api.domain.transporter.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "transporters")
public class Transporter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transporter_id")
    private Long id;

    @Column(name = "name", length = 20)
    private String name;

    @Column(name = "phone")
    private String phone;

    @Column(name = "is_auto_dispatch")
    private boolean isAutoDispatch;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "update_at")
    private LocalDateTime updatedAt;

}
