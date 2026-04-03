package com.ll.content.service;

import com.ll.common.exception.BizException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_TYPES = {"application/pdf"};

    private final Path storageDir;

    public FileStorageService() {
        this.storageDir = Path.of(System.getProperty("user.home"), ".lingualeap", "uploads");
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建上传目录: " + storageDir, e);
        }
    }

    public String store(MultipartFile file) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        String storedName = UUID.randomUUID() + ext;
        Path target = storageDir.resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }

        log.info("文件已保存: {} -> {}", originalName, storedName);
        return storedName;
    }

    public Path getFilePath(String storedName) {
        Path path = storageDir.resolve(storedName).normalize();
        if (!path.startsWith(storageDir)) {
            throw new BizException("非法文件路径");
        }
        return path;
    }

    public void delete(String storedName) {
        if (storedName == null || storedName.isEmpty()) return;
        try {
            Path path = getFilePath(storedName);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", storedName);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择文件");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException("文件大小不能超过 10MB");
        }
        String contentType = file.getContentType();
        boolean allowed = false;
        for (String type : ALLOWED_TYPES) {
            if (type.equals(contentType)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new BizException("仅支持 PDF 文件");
        }
    }
}
