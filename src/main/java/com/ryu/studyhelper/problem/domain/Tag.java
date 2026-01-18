package com.ryu.studyhelper.problem.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "tag")
public class Tag {

    @Id
    @Column(name = "tag_key", length = 64)
    private String key;  // solved.ac 태그 키 (예: "dp", "greedy")

    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    @Column(name = "name_en", nullable = false)
    private String nameEn;

    public static Tag create(String key, String nameKo, String nameEn) {
        return Tag.builder()
                .key(key)
                .nameKo(nameKo)
                .nameEn(nameEn)
                .build();
    }

    public void update(String nameKo, String nameEn) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
    }
}