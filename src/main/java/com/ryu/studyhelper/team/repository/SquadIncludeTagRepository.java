package com.ryu.studyhelper.team.repository;

import com.ryu.studyhelper.problem.domain.Tag;
import com.ryu.studyhelper.team.domain.SquadIncludeTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SquadIncludeTagRepository extends JpaRepository<SquadIncludeTag, Long> {

    @Query("SELECT sit.tag FROM SquadIncludeTag sit WHERE sit.squad.id = :squadId")
    List<Tag> findTagsBySquadId(@Param("squadId") Long squadId);

    @Query("SELECT sit.tag.key FROM SquadIncludeTag sit WHERE sit.squad.id = :squadId")
    List<String> findTagKeysBySquadId(@Param("squadId") Long squadId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SquadIncludeTag sit WHERE sit.squad.id = :squadId")
    void deleteAllBySquadId(@Param("squadId") Long squadId);
}
