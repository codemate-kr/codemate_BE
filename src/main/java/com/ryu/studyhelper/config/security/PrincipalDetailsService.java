package com.ryu.studyhelper.config.security;

import com.ryu.studyhelper.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String id) throws UsernameNotFoundException {
        return memberRepository
                .findById(Long.parseLong(id))
                .map(PrincipalDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));
    }
}
