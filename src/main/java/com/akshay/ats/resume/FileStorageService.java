package com.akshay.ats.resume;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.file-storage.resume-dir}")
    private String resumeDir;

    private static final List<String> ALLOWED_TYPES = List.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public String store(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF and Word documents are allowed");
        }

        Path dir = Paths.get(resumeDir);
        Files.createDirectories(dir);

        String safeName = UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
        Path target = dir.resolve(safeName);
        file.transferTo(target);

        return target.toString();
    }

    public Path resolve(String storedPath) {
        return Paths.get(storedPath);
    }

    private String sanitize(String originalName) {
        if (originalName == null) return "resume";
        return originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
