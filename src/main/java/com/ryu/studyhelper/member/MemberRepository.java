package com.ryu.studyhelper.member;

import com.ryu.studyhelper.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    @Query("SELECT m.handle FROM Member m WHERE m.handle IS NOT NULL AND m.isVerified = true")
    List<String> findAllVerifiedHandles();

    // 핸들로 멤버 찾기 (중복 허용)
    List<Member> findAllByHandle(String handle);

    // providerId로 멤버 찾기
    Optional<Member> findByProviderId(String providerId);

    // 이메일로 멤버 찾기
    Optional<Member> findByEmail(String email);
}