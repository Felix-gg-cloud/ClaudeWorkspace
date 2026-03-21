package com.eqa.repository;

import com.eqa.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, String> {
    List<Chapter> findAllByOrderByOrderIndexAsc();
}
