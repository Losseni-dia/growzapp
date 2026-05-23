package growzapp.backend.module.kyc.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class KycStorageService {

    private final Path root = Paths.get("uploads/private/kyc-documents").toAbsolutePath().normalize();

    public KycStorageService() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new RuntimeException("Erreur d'initialisation du stockage KYC confidentiel", e);
        }
    }

    public String save(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Impossible de sauvegarder un fichier vide.");
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";

            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            String fileName = UUID.randomUUID().toString() + extension;

            Path targetLocation = this.root.resolve(fileName).normalize();

            if (!targetLocation.startsWith(this.root)) {
                throw new RuntimeException("Tentative d'accès hors du dossier de stockage autorisé.");
            }

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du fichier KYC : " + e.getMessage());
        }
    }
}
