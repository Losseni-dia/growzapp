package growzapp.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class KycStorageService {

    // On utilise un sous-dossier 'private' pour être sûr qu'il ne soit jamais
    // exposé
    // par StaticResourceConfig même par erreur.
    private final Path root = Paths.get("uploads/private/kyc-documents").toAbsolutePath().normalize();

    public KycStorageService() {
        try {
            // Création récursive du dossier privé
            Files.createDirectories(root);
            System.out.println("Storage KYC initialisé dans : " + root);
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

            // Extraction sécurisée de l'extension
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();
            }

            // Génération d'un nom unique (UUID) pour anonymiser le fichier sur le disque
            String fileName = UUID.randomUUID().toString() + extension;

            // Résolution sécurisée du chemin pour éviter les attaques "Path Traversal"
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