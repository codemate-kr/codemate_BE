package com.ryu.studyhelper.member.domain;

import com.ryu.studyhelper.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@SQLRestriction("deleted_at IS NULL")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Role role;

    // OAuth2 제공자 (예: "google", "kakao" 등)
    private String provider;

    // OAuth2에서 제공하는 고유 식별자
    @Column(nullable = false, unique = true)
    private String providerId;

    // 이메일 주소
    @Column(nullable = false, unique = true)
    private String email;

    // 백준 핸들 (처음에는 null, 이후 인증 과정에서 입력), 중복 허용
    @Column
    private String handle;

    // 백준 핸들 본인 인증 여부
    @Column(nullable = false)
    private boolean isVerified;

    // 마지막 접속 시간
    private LocalDateTime lastLoginAt;


    //팩토리 메소드
    public static Member create(String provider, String providerId, String email) {
        return Member.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .role(Role.ROLE_USER) // 기본 역할 설정
                .isVerified(false) // 기본 인증 상태 설정
                .build();
    }

    public void changeHandle(String handle) {
        this.handle = handle;
    }

    public void verifyWithHandle(String handle) {
        this.handle = handle;
        this.isVerified = true;
    }

    public void changeEmail(String email) {
        this.email = email;
    }

    public void updateLastLoginAt(LocalDateTime loginTime) {
        this.lastLoginAt = loginTime;
    }

    /**
     * 회원 탈퇴 처리
     * 민감정보를 마스킹하고 소프트 딜리트 수행
     */
    public void withdraw() {
        // 민감정보 마스킹 (UNIQUE 제약 충돌 방지를 위해 ID 기반 더미값 사용)
        this.email = "WITHDRAWN_" + this.id + "@deleted.local";
        this.providerId = "WITHDRAWN_" + this.id;
        this.handle = null;
        this.isVerified = false;

        // 소프트 딜리트
        softDelete();
    }
}
