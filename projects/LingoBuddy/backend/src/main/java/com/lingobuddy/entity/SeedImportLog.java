package com.lingobuddy.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "seed_import_log")
public class SeedImportLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String fileName;

    private LocalDateTime importedAt = LocalDateTime.now();

    public SeedImportLog() {}

    public SeedImportLog(String fileName) {
        this.fileName = fileName;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public LocalDateTime getImportedAt() { return importedAt; }
}
