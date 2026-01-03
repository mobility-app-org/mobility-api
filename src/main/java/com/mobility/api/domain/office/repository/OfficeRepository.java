package com.mobility.api.domain.office.repository;

import com.mobility.api.domain.office.entity.Office;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfficeRepository extends JpaRepository<Office, Long> {
}
