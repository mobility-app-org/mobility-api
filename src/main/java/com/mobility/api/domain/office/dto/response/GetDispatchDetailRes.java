package com.mobility.api.domain.office.dto.response;

import com.mobility.api.domain.dispatch.entity.Dispatch;
import com.mobility.api.domain.dispatch.enums.*;
import com.mobility.api.domain.transporter.entity.Transporter;

import java.time.LocalDateTime;

public record GetDispatchDetailRes(
        Long id,
        String dispatchNumber,
        StatusType status,
        Integer charge,
        String startLocation,
        double startLatitude,
        double startLongitude,
        String destinationLocation,
        double destinationLatitude,
        double destinationLongitude,
        String clientPhoneNumber,
        String memo,
        CallType call,
        ServiceType service,
        PaymentType paymentMethod,
        TollType tollType,
        Long transporterId,
        String transporterName,
        String transporterPhone,
        Long officeId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime assignedAt,
        LocalDateTime completedAt,
        LocalDateTime canceledAt,
        String cancelReason
) {
    public static GetDispatchDetailRes from(Dispatch dispatch) {
        Transporter transporter = dispatch.getTransporter();

        return new GetDispatchDetailRes(
                dispatch.getId(),
                null, // dispatchNumber - 엔티티에 없음
                dispatch.getStatus(),
                dispatch.getCharge(),
                dispatch.getStartLocation(),
                dispatch.getStartLatitude(),
                dispatch.getStartLongitude(),
                dispatch.getDestinationLocation(),
                dispatch.getDestinationLatitude(),
                dispatch.getDestinationLongitude(),
                maskPhoneNumber(dispatch.getClientPhoneNumber()),
                dispatch.getMemo(),
                dispatch.getCall(),
                dispatch.getService(),
                dispatch.getPaymentType(),
                dispatch.getTollType(),
                transporter != null ? transporter.getId() : null,
                transporter != null ? transporter.getName() : null,
                transporter != null ? transporter.getPhone() : null,
                dispatch.getOfficeId(),
                dispatch.getCreatedAt(),
                dispatch.getUpdatedAt(),
                dispatch.getAssignedAt(),
                dispatch.getCompletedAt(),
                dispatch.getCanceledAt(),
                null // cancelReason - 엔티티에 없음
        );
    }

    private static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return phoneNumber;
        }
        // 010-1234-5678 -> 010-****-5678
        // 01012345678 -> 010****5678
        if (phoneNumber.contains("-")) {
            String[] parts = phoneNumber.split("-");
            if (parts.length == 3) {
                return parts[0] + "-****-" + parts[2];
            }
        }
        // 하이픈 없는 경우 (01012345678)
        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "****" + phoneNumber.substring(7);
        }
        return phoneNumber;
    }
}
