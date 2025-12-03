// src/main/java/growzapp/backend/service/FileStorageService.java

package growzapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // =============== CHEMINS ===============

    private Path getRootPath() {
        Path path = Paths.get(uploadDir).toAbsolutePath().normalize();
        createDirectoriesIfNotExists(path);
        return path;
    }

    public Path getUploadPath(String subfolder) {
        Path path = getRootPath().resolve(subfolder).normalize();
        createDirectoriesIfNotExists(path);
        return path;
    }

    private void createDirectoriesIfNotExists(Path path) {
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            throw new RuntimeException("Impossible de créer le dossier : " + path, e);
        }
    }

    // =============== SAUVEGARDE ===============

    private String storeFile(MultipartFile file, String subfolder, String urlPrefix) throws IOException {
        String original = StringUtils.cleanPath(file.getOriginalFilename());
        String filename = UUID.randomUUID() + "_" + original;

        Path target = getUploadPath(subfolder).resolve(filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        return urlPrefix + filename;
    }

    // → Utilisé pour posters projets et avatars utilisateurs
    public String savePosterOrAvatar(MultipartFile file) throws IOException {
        return storeFile(file, "posters", "/uploads/posters/");
    }

    // → LA MÉTHODE QUE TU CHERCHES DANS DocumentController
    public String saveProjectDocument(MultipartFile file) throws IOException {
        return storeFile(file, "documents", "/files/documents/");
    }

    public String saveContrat(MultipartFile file) throws IOException {
        return storeFile(file, "contrats", "/uploads/contrats/");
    }

    // =============== CHARGEMENT ===============

    public byte[] loadDocumentAsBytes(String filenameOnly) throws IOException {
        Path filePath = getUploadPath("documents").resolve(filenameOnly).normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("Document non trouvé : " + filenameOnly);
        }
        return Files.readAllBytes(filePath);
    }

    public byte[] loadAsBytes(String subfolder, String filename) throws IOException {
        Path filePath = getUploadPath(subfolder).resolve(filename).normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("Fichier non trouvé : " + filename);
        }
        return Files.readAllBytes(filePath);
    }

    // AJOUTE CETTE MÉTHODE (compatible avec tous tes anciens appels)
    public byte[] loadAsBytes(String fullUrl) throws IOException {
        // fullUrl = "/uploads/contrats/uuid_123.pdf" ou
        // "/files/documents/uuid_bilan.pdf"
        String filename = fullUrl.substring(fullUrl.lastIndexOf("/") + 1);

        // On détecte automatiquement le sous-dossier
        if (fullUrl.contains("/uploads/posters/") || fullUrl.contains("/uploads/contrats/")
                || fullUrl.contains("/uploads/avatars/")) {
            String subfolder = fullUrl.contains("/posters/") ? "posters"
                    : fullUrl.contains("/contrats/") ? "contrats" : "avatars";
            return loadAsBytes(subfolder, filename);
        }

        if (fullUrl.contains("/files/documents/")) {
            return loadDocumentAsBytes(filename);
        }

        throw new IOException("URL de fichier non reconnue : " + fullUrl);
    }


    // ===================================================================
    // COMPATIBILITÉ ASCENDANTE – POUR LES CONTRATS (et tout ancien code)
    // ===================================================================
    public String save(byte[] content, String originalFilename, String contentType) throws IOException {
        String filename = UUID.randomUUID() + "_" + StringUtils.cleanPath(originalFilename);
        Path target = getUploadPath("contrats").resolve(filename);
        Files.write(target, content);
        return "/uploads/contrats/" + filename;
    }


    /**
     * Compatibilité totale avec l'ancien système
     * Accepte juste le nom du fichier et devine le dossier selon les conventions
     * actuelles
     */
    public Path load(String filename) {
        // On cherche dans les dossiers publics dans l'ordre de priorité
        Path[] candidates = {
                getUploadPath("posters").resolve(filename),
                getUploadPath("contrats").resolve(filename),
                getUploadPath("avatars").resolve(filename)
        };

        for (Path path : candidates) {
            if (Files.exists(path)) {
                return path.normalize();
            }
        }

        // Si rien trouvé → on retourne posters par défaut (comme avant)
        return getUploadPath("posters").resolve(filename).normalize();
    }
}