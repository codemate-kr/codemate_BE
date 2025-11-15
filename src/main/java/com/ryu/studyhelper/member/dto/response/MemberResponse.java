package com.ryu.studyhelper.member.dto.response;

import com.ryu.studyhelper.member.domain.Member;
import com.ryu.studyhelper.member.domain.Role;

public record MemberResponse(
        Long id,
        String email,
        String handle,
        boolean verified,
        Role role,
        String provider,
        String providerId
) {
    public static MemberResponse from(Member m) {
        return new MemberResponse(
                m.getId(),
                m.getEmail(),
                m.getHandle(),
                m.isVerified(),
                m.getRole(),
                m.getProvider(),
                m.getProviderId()
        );
    }
}

