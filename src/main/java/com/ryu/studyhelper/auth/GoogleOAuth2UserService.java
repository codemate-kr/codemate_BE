package com.ryu.studyhelper.auth;

import com.ryu.studyhelper.auth.dto.OAuthInfo;
import com.ryu.studyhelper.config.security.PrincipalDetails;
import com.ryu.studyhelper.infrastructure.discord.DiscordMessage;
import com.ryu.studyhelper.infrastructure.discord.DiscordNotifier;
import com.ryu.studyhelper.member.repository.MemberRepository;
import com.ryu.studyhelper.member.domain.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final DiscordNotifier discordNotifier;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.info("Google OAuth2 사용자 정보 로드 시작");

        // 1. Google로부터 사용자 정보 받아오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. Google 사용자 정보 추출
        OAuthInfo oAuthInfo = OAuthInfo.ofGoogle(oAuth2User);
        log.info("Google 사용자 정보 추출 완료: {}", oAuthInfo.getEmail());

        // 3. 사용자 정보 저장 또는 업데이트
        Member member = saveOrUpdateMember(oAuthInfo);

        // 4. PrincipalDetails 객체 반환
        return new PrincipalDetails(member, oAuth2User.getAttributes());
    }

    private Member saveOrUpdateMember(OAuthInfo oAuthInfo) {
        Optional<Member> existingMember = memberRepository.findByProviderId(oAuthInfo.getProviderId());
        if (existingMember.isPresent()) {
            // 기존 사용자 반환
            Member member = existingMember.get();
            log.info("기존 사용자 로그인: {}", member.getEmail());
            return member;
        } else {
            // 신규 사용자 생성
            Member newMember = Member.create("google", oAuthInfo.getProviderId(), oAuthInfo.getEmail());
            Member savedMember = memberRepository.save(newMember);
            log.info("신규 사용자 생성 완료: {}", savedMember.getEmail());

            discordNotifier.sendEvent(DiscordMessage.event("신규 회원 가입",
                    "회원 ID", String.valueOf(savedMember.getId()),
                    "이메일", maskEmail(savedMember.getEmail())
            ));

            return savedMember;
        }
    }

    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 3) {
            return email.charAt(0) + "***" + email.substring(atIndex);
        }
        return email.substring(0, 3) + "***" + email.substring(atIndex);
    }
}