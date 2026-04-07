package com.ll.content.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "vocab_constraint")
public class VocabConstraint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(length = 30)
    private String pos;

    @Column(name = "meaning_zh", nullable = false, length = 500)
    private String meaningZh;

    @Column(name = "level_code", nullable = false, length = 10)
    private String levelCode;

    @Column(name = "source_book", nullable = false, length = 50)
    private String sourceBook;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // --- getters & setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWord() { return word; }
    public void setWord(String word) { this.word = word; }

    public String getPos() { return pos; }
    public void setPos(String pos) { this.pos = pos; }

    public String getMeaningZh() { return meaningZh; }
    public void setMeaningZh(String meaningZh) { this.meaningZh = meaningZh; }

    public String getLevelCode() { return levelCode; }
    public void setLevelCode(String levelCode) { this.levelCode = levelCode; }

    public String getSourceBook() { return sourceBook; }
    public void setSourceBook(String sourceBook) { this.sourceBook = sourceBook; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
