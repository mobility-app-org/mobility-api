package com.mobility.api.domain.office.entity;

import com.mobility.api.domain.office.enums.ManagerRole;
import com.mobility.api.domain.office.enums.ManagerStatus;
import com.mobility.api.global.entity.BaseSoftDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "manager") // DB 테이블명
@SQLDelete(sql = "UPDATE manager SET deleted_at = NOW() WHERE manager_id = ?")
public class Manager extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "manager_id")
    private Long id;

    // 1. 로그인 ID (새로 추가! - 예: tak123)
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    // 2. 비밀번호
    @Column(nullable = false)
    private String password;

    // 3. 사용자 이름 (예: 김담당)
    @Column(nullable = false, length = 20)
    private String name;

    // 휴대폰 번호 (예: 010-5244-4070)
    @Column(nullable = false, length = 20)
    private String phone;

    // 이메일 (로그인용 아님, 연락용)
    @Column(nullable = false, length = 100)
    private String email;

    // 4. 권한 (OWNER: 사무실 대표, ??: 일반 직원) - 선택사항
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ManagerRole role;

    // 상태 (ACTIVE, PENDING, INACTIVE)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20) default 'PENDING'")
    private ManagerStatus status;

    // 5. 소속 사무실 (어느 사무실 사람인지?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @Builder
    public Manager(String loginId, String password, String name, String phone, String email, ManagerRole role, Office office) {
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.office = office;
    }

    // [비즈니스 로직] 상태 변경 메서드
    public void updateStatus(ManagerStatus status) {
        this.status = status;
    }
}