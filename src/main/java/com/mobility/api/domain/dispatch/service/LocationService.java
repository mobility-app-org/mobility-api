package com.mobility.api.domain.dispatch.service;

import com.mobility.api.domain.transporter.dto.request.LocationUpdateReq;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;

@Service
public class LocationService {
    public void processLocationUpdate(Long transporterId, @Valid LocationUpdateReq locationUpdateReq) {

    }
}
