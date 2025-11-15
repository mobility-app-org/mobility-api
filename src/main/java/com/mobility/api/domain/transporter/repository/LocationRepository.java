package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.transporter.entity.LocationHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<LocationHistory, Integer> {
}
