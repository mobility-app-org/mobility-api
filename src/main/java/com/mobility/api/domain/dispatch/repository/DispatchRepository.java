package com.mobility.api.domain.dispatch.repository;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {
}
