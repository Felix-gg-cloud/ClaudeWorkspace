package com.ll.ai.service;

import com.ll.ai.entity.AiAnalysisCache;
import com.ll.ai.repository.AiAnalysisCacheRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class CacheService {

    private static final int CACHE_DAYS = 7;

    private final AiAnalysisCacheRepository cacheRepo;

    public CacheService(AiAnalysisCacheRepository cacheRepo) {
        this.cacheRepo = cacheRepo;
    }

    /**
     * 查询缓存
     * @return 缓存的结果，未命中返回 null
     */
    public String get(String input, String analysisType) {
        String hash = sha256(input);
        Optional<AiAnalysisCache> cached = cacheRepo
                .findByContentHashAndAnalysisTypeAndExpiresAtAfter(hash, analysisType, LocalDateTime.now());
        return cached.map(AiAnalysisCache::getResult).orElse(null);
    }

    /**
     * 写入缓存
     */
    public void put(String input, String analysisType, String result, String model, Integer tokensUsed) {
        String hash = sha256(input);
        AiAnalysisCache cache = new AiAnalysisCache();
        cache.setContentHash(hash);
        cache.setAnalysisType(analysisType);
        cache.setInputSummary(input.length() > 200 ? input.substring(0, 200) : input);
        cache.setResult(result);
        cache.setModel(model);
        cache.setTokensUsed(tokensUsed);
        cache.setExpiresAt(LocalDateTime.now().plusDays(CACHE_DAYS));
        cacheRepo.save(cache);
    }

    private String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
