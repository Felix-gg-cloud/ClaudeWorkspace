package com.ll.content.controller;

import com.ll.common.dto.ApiResponse;
import com.ll.common.util.UserContext;
import com.ll.content.entity.QuestionBank;
import com.ll.content.service.BankService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content/banks")
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping
    public ApiResponse<Page<QuestionBank>> list(
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId();
        return ApiResponse.ok(bankService.list(grade, type, keyword, userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionBank> getById(@PathVariable Long id) {
        return ApiResponse.ok(bankService.getById(id));
    }

    @PostMapping
    public ApiResponse<QuestionBank> create(@RequestBody QuestionBank bank) {
        bank.setUserId(getUserId());
        bank.setType("user_upload");
        return ApiResponse.ok(bankService.create(bank));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionBank> update(@PathVariable Long id, @RequestBody QuestionBank bank) {
        return ApiResponse.ok(bankService.update(id, bank, getUserId()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bankService.delete(id, getUserId());
        return ApiResponse.ok();
    }

    private Long getUserId() {
        Long userId = UserContext.getUserId();
        return userId != null ? userId : 0L;
    }
}
