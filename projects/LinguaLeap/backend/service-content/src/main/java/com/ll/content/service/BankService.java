package com.ll.content.service;

import com.ll.common.exception.BizException;
import com.ll.content.entity.QuestionBank;
import com.ll.content.repository.QuestionBankRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {

    private final QuestionBankRepository bankRepo;

    public BankService(QuestionBankRepository bankRepo) {
        this.bankRepo = bankRepo;
    }

    public Page<QuestionBank> list(String grade, String type, String keyword, Long userId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return bankRepo.findByFilters(grade, type, keyword, userId, pageable);
    }

    public QuestionBank getById(Long id) {
        return bankRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "题库不存在"));
    }

    @Transactional
    public QuestionBank create(QuestionBank bank) {
        return bankRepo.save(bank);
    }

    @Transactional
    public QuestionBank update(Long id, QuestionBank updates, Long userId) {
        QuestionBank bank = getById(id);
        checkOwnership(bank, userId);
        if (updates.getName() != null) bank.setName(updates.getName());
        if (updates.getDescription() != null) bank.setDescription(updates.getDescription());
        if (updates.getGrade() != null) bank.setGrade(updates.getGrade());
        return bankRepo.save(bank);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        QuestionBank bank = getById(id);
        if ("preset".equals(bank.getType())) {
            throw new BizException("预制题库不可删除");
        }
        checkOwnership(bank, userId);
        bankRepo.delete(bank);
    }

    private void checkOwnership(QuestionBank bank, Long userId) {
        if (bank.getUserId() != null && !bank.getUserId().equals(userId)) {
            throw new BizException(403, "无权操作此题库");
        }
    }
}
