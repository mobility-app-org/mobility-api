package com.mobility.api.domain.transporter.repository;

import com.mobility.api.domain.transporter.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {
}
