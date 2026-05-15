package com.zaytoun.aiprediction.service;

import com.zaytoun.aiprediction.validation.BatchIdRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class StorageService {

    private final Path rootDirectory;

    public StorageService(@Value("${ai.storage.root:./storage/ai-prediction}") String rootDirectory) throws IOException {
        this.rootDirectory = Paths.get(rootDirectory).toAbsolutePath().normalize();
        Files.createDirectories(this.rootDirectory.resolve("images"));
        Files.createDirectories(this.rootDirectory.resolve("models"));
    }

    public Path storeImage(MultipartFile file, String batchId) {
        try {
            String normalizedBatchId = BatchIdRules.requireValid(batchId);
            String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image.png" : file.getOriginalFilename());
            String extension = original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".png";
            Path folder = rootDirectory.resolve("images").resolve(LocalDate.now().toString()).resolve(normalizedBatchId);
            Files.createDirectories(folder);
            Path destination = folder.resolve(UUID.randomUUID() + extension);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store image file", e);
        }
    }

    public Path modelPath(String version) {
        return rootDirectory.resolve("models").resolve(version + ".json");
    }

    public Path modelsDirectory() {
        return rootDirectory.resolve("models");
    }
}
