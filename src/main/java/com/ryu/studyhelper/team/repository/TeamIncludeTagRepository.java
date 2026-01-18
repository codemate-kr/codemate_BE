package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.team.domain.TeamIncludeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamIncludeTagRepository extends JpaRepository<TeamIncludeTag, Long> {

    List<TeamIncludeTag> findByTeamId(Long teamId);

    @Query("SELECT tit.tag FROM TeamIncludeTag tit WHERE tit.team.id = :teamId")
    List<Tag> findTagsByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT tit.tag.key FROM TeamIncludeTag tit WHERE tit.team.id = :teamId")
    List<String> findTagKeysByTeamId(@Param("teamId") Long teamId);

    @Modifying
    @Query("DELETE FROM TeamIncludeTag tit WHERE tit.team.id = :teamId")
    void deleteAllByTeamId(@Param("teamId") Long teamId);

    boolean existsByTeamIdAndTagKey(Long teamId, String tagKey);
}
