package com.ryu.studyhelper.problem.repository;

import com.ryu.studyhelper.problem.domain.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, String> {

    // findById(key)로 대체 가능하지만 명시적으로 유지
    Optional<Tag> findByKey(String key);

    // findAllById(keys)로 대체 가능하지만 명시적으로 유지
    List<Tag> findByKeyIn(List<String> keys);
}