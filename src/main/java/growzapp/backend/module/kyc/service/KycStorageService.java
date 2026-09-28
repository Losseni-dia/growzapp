package growzapp.backend.module.kyc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import growzapp.backend.module.files.validation.FileValidationService;
import java.io.IOException;
import java.nio.file.*;
import java.util.Base64;
import java.util.UUID;

@Service
public class KycStorageService {
    private final Path root = Paths.get("uploads/private/kyc-documents").toAbsolutePath().normalize();

    @Autowired
    private FileValidationService fileValidationService;

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
            // Vérifie le VRAI contenu du fichier (image ou PDF) avant tout
            // traitement — documents d'identité, particulièrement sensibles (HIGH-04)
            fileValidationService.validateDocument(file);

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

    // Sauvegarde une image reçue en base64 (VOVE ID renvoie ainsi les
    // documents du dossier) — pas de MultipartFile ici, donc pas de
    // validateDocument() : le contenu vient d'un fournisseur KYC de
    // confiance déjà validé côté VOVE ID, pas d'un upload utilisateur brut.
    public String saveBase64(String base64Content, String extension) {
        if (base64Content == null || base64Content.isBlank()) {
            throw new RuntimeException("Impossible de sauvegarder une image vide.");
        }
        try {
            // Certains fournisseurs préfixent en data URL ("data:image/...;base64,")
            String cleaned = base64Content.contains(",")
                    ? base64Content.substring(base64Content.indexOf(",") + 1)
                    : base64Content;
            byte[] data = Base64.getDecoder().decode(cleaned);

            String fileName = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.root.resolve(fileName).normalize();

            if (!targetLocation.startsWith(this.root)) {
                throw new RuntimeException("Tentative d'accès hors du dossier de stockage autorisé.");
            }

            Files.write(targetLocation, data);
            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde de l'image KYC : " + e.getMessage());
        }
    }
}
