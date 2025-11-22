package growzapp.backend.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private static final String UPLOAD_DIR = "uploads/posters/";

    public String uploadPoster(MultipartFile file, Long projetId) {
        try {
            // Création du dossier si inexistant
            Path uploadPath = Paths.get(UPLOAD_DIR);
            Files.createDirectories(uploadPath);

            String fileName = projetId + "_" + System.currentTimeMillis() + "_"
                    + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Retourne l'URL accessible via le navigateur
            return "/uploads/posters/" + fileName;
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'uploader le poster", e);
        }
    }
}