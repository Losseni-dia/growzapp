// src/main/java/growzapp/backend/service/FileStorageService.java

package growzapp.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
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
        if (filenameOnly == null || filenameOnly.isBlank()) {
            throw new IOException("Nom de fichier vide");
        }

        // Nettoie les chemins d'URL genre "/files/documents/monfichier.pdf" →
        // "monfichier.pdf"
        String cleanFilename = filenameOnly
                .substring(filenameOnly.lastIndexOf("/") + 1) // enlève tout avant le dernier /
                .substring(filenameOnly.lastIndexOf("\\") + 1) // au cas où y'aurait des \
                .trim();

        if (cleanFilename.isBlank() || cleanFilename.contains("..") || cleanFilename.contains("/")
                || cleanFilename.contains("\\")) {
            throw new IOException("Nom de fichier invalide après nettoyage : " + cleanFilename);
        }

        Path root = getUploadPath("documents");
        Path filePath = root.resolve(cleanFilename).normalize();

        // Sécurité anti-traversal
        if (!filePath.startsWith(root)) {
            throw new IOException("Tentative d'accès hors du dossier documents : " + cleanFilename);
        }

        if (!Files.exists(filePath)) {
            throw new IOException("Fichier introuvable sur le disque : " + cleanFilename);
        }

        if (!Files.isReadable(filePath)) {
            throw new IOException("Fichier non lisible (ouvert dans un autre programme ?) : " + filePath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (AccessDeniedException e) {
            throw new IOException("Fichier verrouillé – ferme-le dans Acrobat/Excel !", e);
        }
    }

    // SUPPRIME ÇA (l’ancienne) :
    // public byte[] loadAsBytes(String subfolder, String filename) throws
    // IOException { ... }

    // GARDE UNIQUEMENT ÇA (la nouvelle, qui remplace tout) :
    public byte[] loadAsBytes(String pathOrFilename) throws IOException {
        if (pathOrFilename == null || pathOrFilename.isBlank()) {
            throw new IOException("Chemin vide");
        }

        String filename = pathOrFilename
                .substring(pathOrFilename.lastIndexOf("/") + 1)
                .substring(pathOrFilename.lastIndexOf("\\") + 1)
                .trim();

        if (filename.isBlank() || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IOException("Nom invalide : " + filename);
        }

        Path filePath;

        // 1. URL complète → on détecte le dossier
        if (pathOrFilename.contains("/uploads/contrats/")) {
            filePath = getUploadPath("contrats").resolve(filename);
        } else if (pathOrFilename.contains("/uploads/posters/")) {
            filePath = getUploadPath("posters").resolve(filename);
        } else if (pathOrFilename.contains("/files/documents/")) {
            filePath = getUploadPath("documents").resolve(filename);
        }
        // 2. Sinon → c’est un filename pur → on suppose contrat (cas le plus courant
        // maintenant)
        else {
            filePath = getUploadPath("contrats").resolve(filename);
        }

        filePath = filePath.normalize();

        // Sécurité anti-traversal
        if (!filePath.startsWith(getRootPath())) {
            throw new IOException("Accès refusé hors du dossier uploads : " + filename);
        }

        if (!Files.exists(filePath)) {
            throw new IOException("Fichier non trouvé : " + filePath);
        }

        return Files.readAllBytes(filePath);
    }


    // ===================================================================
    // COMPATIBILITÉ ASCENDANTE – POUR LES CONTRATS (et tout ancien code)
    // ===================================================================
    // ==================================================================================
    // ✅ MÉTHODE SPÉCIALE POUR SAUVEGARDER LES CONTRATS PDF GÉNÉRÉS (Investissement)
    // ==================================================================================
    public String saveContrat(byte[] content, String originalFilename) {
        try {
            // 1. Nettoyage du nom de fichier par sécurité
            String filename = StringUtils.cleanPath(originalFilename);

            // 2. Récupération du chemin physique : .../uploads/contrats/
            // La méthode getUploadPath("contrats") crée le dossier s'il n'existe pas
            Path target = getUploadPath("contrats").resolve(filename);

            // 3. Écriture physique du fichier (écrase si existe déjà avec le même nom)
            Files.write(target, content);

            // 4. RETOUR DE L'URL WEB (C'est la clé pour que le front puisse télécharger)
            // On ne retourne PAS le chemin C:\Users..., mais le chemin relatif /uploads/...
            return "/uploads/contrats/" + filename;

        } catch (IOException e) {
            // On lance une RuntimeException pour que la transaction @Transactional rollbak
            // si besoin
            throw new RuntimeException("Erreur critique lors de la sauvegarde du fichier contrat : " + originalFilename,
                    e);
        }
    }


    // Ajoute cette méthode dans FileStorageService.java
    public String saveFacture(byte[] content, String originalFilename) throws IOException {
        String filename = "facture-dividende-" + System.currentTimeMillis() + ".pdf"; // ou garde ton format
        Path target = getUploadPath("factures").resolve(filename); // ← nouveau dossier "factures"
        Files.write(target, content);
        return "/uploads/factures/" + filename; // ← URL cohérente
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

    // Ajoute cette méthode dans FileStorageService.java
    public String getUploadDir() {
        return uploadDir;
    }
}