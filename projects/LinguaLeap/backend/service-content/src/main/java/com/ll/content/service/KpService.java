package com.ll.content.service;

import com.ll.common.exception.BizException;
import com.ll.content.entity.KnowledgePoint;
import com.ll.content.entity.QuestionBank;
import com.ll.content.repository.KnowledgePointRepository;
import com.ll.content.repository.QuestionBankRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class KpService {

    private final KnowledgePointRepository kpRepo;
    private final QuestionBankRepository bankRepo;

    public KpService(KnowledgePointRepository kpRepo, QuestionBankRepository bankRepo) {
        this.kpRepo = kpRepo;
        this.bankRepo = bankRepo;
    }

    public Page<KnowledgePoint> list(Long bankId, String type, String keyword, Integer difficulty, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        return kpRepo.findByBankIdAndFilters(bankId, type, keyword, difficulty, pageable);
    }

    public KnowledgePoint getById(Long id) {
        return kpRepo.findById(id)
                .orElseThrow(() -> new BizException(404, "知识点不存在"));
    }

    @Transactional
    public KnowledgePoint create(Long bankId, KnowledgePoint kp) {
        bankRepo.findById(bankId).orElseThrow(() -> new BizException(404, "题库不存在"));
        kp.setBankId(bankId);
        KnowledgePoint saved = kpRepo.save(kp);
        updateBankKpCount(bankId);
        return saved;
    }

    @Transactional
    public KnowledgePoint update(Long id, KnowledgePoint updates) {
        KnowledgePoint kp = getById(id);
        if (updates.getContent() != null) kp.setContent(updates.getContent());
        if (updates.getPhonetic() != null) kp.setPhonetic(updates.getPhonetic());
        if (updates.getMeaningZh() != null) kp.setMeaningZh(updates.getMeaningZh());
        if (updates.getExampleSentence() != null) kp.setExampleSentence(updates.getExampleSentence());
        if (updates.getExampleZh() != null) kp.setExampleZh(updates.getExampleZh());
        if (updates.getDifficulty() != null) kp.setDifficulty(updates.getDifficulty());
        if (updates.getTags() != null) kp.setTags(updates.getTags());
        if (updates.getType() != null) kp.setType(updates.getType());
        return kpRepo.save(kp);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgePoint kp = getById(id);
        Long bankId = kp.getBankId();
        kpRepo.delete(kp);
        updateBankKpCount(bankId);
    }

    @Transactional
    public List<KnowledgePoint> batchImport(Long bankId, List<KnowledgePoint> kps) {
        bankRepo.findById(bankId).orElseThrow(() -> new BizException(404, "题库不存在"));
        kps.forEach(kp -> kp.setBankId(bankId));
        List<KnowledgePoint> saved = kpRepo.saveAll(kps);
        updateBankKpCount(bankId);
        return saved;
    }

    private void updateBankKpCount(Long bankId) {
        bankRepo.findById(bankId).ifPresent(bank -> {
            bank.setKpCount((int) kpRepo.countByBankId(bankId));
            bankRepo.save(bank);
        });
    }
}
