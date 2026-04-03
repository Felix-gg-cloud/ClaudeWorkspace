package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.common.util.UserContext;
import com.ll.content.entity.StudySet;
import com.ll.content.service.StudySetService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/content/study-sets")
public class StudySetController {

    private final StudySetService studySetService;

    public StudySetController(StudySetService studySetService) {
        this.studySetService = studySetService;
    }

    /**
     * 从文本创建学习集
     */
    @PostMapping
    public ApiResponse<StudySet> createFromText(@RequestBody Map<String, String> body,
                                                 HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String title = body.getOrDefault("title", "我的学习集");
        String text = body.get("text");
        String sourceType = body.getOrDefault("sourceType", "text");
        String userNote = body.get("userNote");
        String grade = body.get("grade");

        String token = request.getHeader("Authorization");
        StudySet set = studySetService.createFromText(userId, title, text, sourceType, userNote, grade, token);
        return ApiResponse.ok(set);
    }

    /**
     * 从 PDF 文件创建学习集
     */
    @PostMapping("/upload")
    public ApiResponse<StudySet> uploadFile(@RequestParam("file") MultipartFile file,
                                            @RequestParam(value = "title", defaultValue = "PDF 学习集") String title,
                                            @RequestParam(value = "userNote", required = false) String userNote,
                                            @RequestParam(value = "grade", required = false) String grade,
                                            HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        String token = request.getHeader("Authorization");
        StudySet set = studySetService.createFromFile(userId, title, file, userNote, grade, token);
        return ApiResponse.ok(set);
    }

    /**
     * 我的学习集列表
     */
    @GetMapping
    public ApiResponse<List<StudySet>> list() {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(studySetService.listByUser(userId));
    }

    /**
     * 学习集详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        return ApiResponse.ok(studySetService.getDetail(id, userId));
    }

    /**
     * 删除学习集
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        studySetService.delete(id, userId);
        return ApiResponse.ok(null);
    }
}
