package com.ll.content.repository;

import com.ll.content.entity.GoldenSample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoldenSampleRepository extends JpaRepository<GoldenSample, Long> {

    List<GoldenSample> findByLevelCode(String levelCode);
}
