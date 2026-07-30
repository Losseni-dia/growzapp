package growzapp.backend.module.files;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.files.validation.FileValidationService;
import lombok.RequiredArgsConstructor;

// FileUploadService.java
@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileValidationService fileValidationService;

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
            // Vérifie le VRAI contenu du fichier avant tout traitement
            // (HIGH-04) — lève une exception si ce n'est pas une vraie image
            fileValidationService.validateImage(file);

            String original = file.getOriginalFilename();
            String safeName = projetId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/posters/" + safeName;
        } catch (IllegalArgumentException e) {
            // Message de validation précis (type non autorisé, trop volumineux...)
            // propagé tel quel pour l'utilisateur
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload poster", e);
        }
    }
}