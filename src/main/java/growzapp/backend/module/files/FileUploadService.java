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

    private static final Path FICHE_PORTEUR_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("fiches-porteur");

    private static final Path PROJET_PHOTOS_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("projet-photos");

    private static final Path COMMANDE_FACTURES_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("commande-factures");

    static {
        try {
            Files.createDirectories(UPLOAD_ROOT);
            Files.createDirectories(FICHE_PORTEUR_UPLOAD_ROOT);
            Files.createDirectories(PROJET_PHOTOS_UPLOAD_ROOT);
            Files.createDirectories(COMMANDE_FACTURES_UPLOAD_ROOT);
        } catch (IOException e) {
            throw new RuntimeException("Impossible de créer les dossiers d'upload", e);
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

    public String uploadFichePorteurPhoto(MultipartFile file, Long userId) {
        try {
            // Résolution minimale exigée — au-delà de sa taille d'affichage
            // actuelle (petit avatar), cette photo doit rester nette si on
            // l'agrandit un jour (page profil complète, etc.).
            fileValidationService.validateImageMinDimensions(file, 300, 300);

            String original = file.getOriginalFilename();
            String safeName = userId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = FICHE_PORTEUR_UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/fiches-porteur/" + safeName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload photo fiche porteur", e);
        }
    }

    public String uploadProjetPhoto(MultipartFile file, Long projetId) {
        try {
            fileValidationService.validateImage(file);

            String original = file.getOriginalFilename();
            String safeName = projetId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = PROJET_PHOTOS_UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/projet-photos/" + safeName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload photo du projet", e);
        }
    }

    public String uploadFactureCommande(MultipartFile file, Long commandeId) {
        try {
            // Facture = document justificatif transmis aux investisseurs pour
            // preuve de l'usage réel des fonds — accepte PDF ou image
            // (validateDocument), pas seulement image comme les autres
            // uploads de ce service.
            fileValidationService.validateDocument(file);

            String original = file.getOriginalFilename();
            String safeName = commandeId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = COMMANDE_FACTURES_UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/commande-factures/" + safeName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload facture de la commande", e);
        }
    }
}