package growzapp.backend.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

// FileUploadService.java
@Service
@RequiredArgsConstructor
public class FileUploadService {

    // Chemin ABSOLU à la racine du projet
    private static final Path UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("posters");

    static {
        try {
            Files.createDirectories(UPLOAD_ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer le dossier uploads/posters", e);
        }
    }

    public String uploadPoster(MultipartFile file, Long projetId) {
        try {
            String original = file.getOriginalFilename();
            String safeName = projetId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/posters/" + safeName;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload poster", e);
        }
    }
}