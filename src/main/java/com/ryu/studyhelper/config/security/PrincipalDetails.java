package com.ryu.studyhelper.config.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.ryu.studyhelper.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
public class PrincipalDetails implements OAuth2User, UserDetails {

    private final Member member;
    private final Map<String, Object> attributes;

    // OAuth2 로그인용 생성자
    public PrincipalDetails(Member member, Map<String, Object> attributes) {
        this.member = member;
        this.attributes = attributes;
    }

    // JWT 인증용 생성자
    public PrincipalDetails(Member member) {
        this.member = member;
        this.attributes = null;
    }

    // OAuth2User 구현
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return member.getEmail();
    }

    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(member.getRole().getValue())
        );
    }

    @Override
    public String getPassword() {
        return null; // OAuth2는 비밀번호 없음
    }

    @Override
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.isVerified();
    }

    // 편의 메서드들
    public Long getMemberId() {
        return member.getId();
    }

    public String getEmail() {
        return member.getEmail();
    }

//    public boolean isBojVerified() {
//        return member.getStatus() == MemberStatus.ACTIVE;
//    }

//    public MemberStatus getMemberStatus() {
//        return member.getStatus();
//    }
}