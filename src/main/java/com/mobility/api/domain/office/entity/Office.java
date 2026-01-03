package com.mobility.api.domain.office.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "office")
public class Office {

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
}
