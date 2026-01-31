package com.mobility.api.domain.office.entity;

import com.mobility.api.domain.office.dto.request.OfficeUpdateReq;
import com.mobility.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "office")
public class Office extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 1. 사업장 이름 (예: mobi 탁송)
    @Column(nullable = false, length = 50)
    private String officeName;

    // 사업자 등록 번호
    @Column(name = "office_registration_number")
    private String officeRegistrationNumber;

    // 3. 사업장 주소
    @Column(nullable = false)
    private String officeAddress;

    // 사업장 전화번호
    @Column(name = "office_tel_number")
    private String officeTelNumber;

    @Builder
    public Office(String officeName, String officeRegistrationNumber, String officeAddress, String officeTelNumber) {
        this.officeName = officeName;
        this.officeRegistrationNumber = officeRegistrationNumber;
        this.officeAddress = officeAddress;
        this.officeTelNumber = officeTelNumber;
    }

    public void updateProfile(OfficeUpdateReq req) {
        // 값이 null이 아니고, (필요시) 비어있지 않은 경우에만 업데이트
        if (req.name() != null) {
            this.officeName = req.name();
        }
        if (req.registrationNumber() != null) {
            this.officeRegistrationNumber = req.registrationNumber();
        }
        if (req.address() != null) {
            this.officeAddress = req.address();
        }
        if (req.telNumber() != null) {
            this.officeTelNumber = req.telNumber();
        }
    }
}
