package com.eqa.repository;

import com.eqa.entity.ContentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ContentItemRepository extends JpaRepository<ContentItem, String> {
    List<ContentItem> findByChapterCode(String chapterCode);
    List<ContentItem> findByChapterCodeAndType(String chapterCode, String type);
}
