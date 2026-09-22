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

    private static final Path ARTICLE_PHOTOS_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("article-photos");

    // Stockage des documents projet — servi exclusivement par
    // ProjetDocumentFileController (accès réservé porteur/investisseurs/admin,
    // jamais exposé en statique public comme les autres dossiers ci-dessus).
    private static final Path DOCUMENTS_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("documents");

    private static final Path FOURNISSEUR_LOGOS_UPLOAD_ROOT = Paths.get(System.getProperty("user.dir"))
            .resolve("uploads").resolve("fournisseur-logos");

    static {
        try {
            Files.createDirectories(UPLOAD_ROOT);
            Files.createDirectories(FICHE_PORTEUR_UPLOAD_ROOT);
            Files.createDirectories(PROJET_PHOTOS_UPLOAD_ROOT);
            Files.createDirectories(COMMANDE_FACTURES_UPLOAD_ROOT);
            Files.createDirectories(ARTICLE_PHOTOS_UPLOAD_ROOT);
            Files.createDirectories(DOCUMENTS_UPLOAD_ROOT);
            Files.createDirectories(FOURNISSEUR_LOGOS_UPLOAD_ROOT);
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

    // Facture générée automatiquement par le serveur (PDF) à l'expédition
    // d'une commande — même dossier que l'upload manuel, pour rester
    // compatible avec copierFactureVersDocuments() et le contrôleur de
    // téléchargement de facture.
    public String enregistrerFactureGeneree(byte[] pdfBytes, Long commandeId) {
        try {
            String safeName = commandeId + "_" + System.currentTimeMillis() + "_facture.pdf";
            Path destination = COMMANDE_FACTURES_UPLOAD_ROOT.resolve(safeName);
            Files.write(destination, pdfBytes);
            return "/uploads/commande-factures/" + safeName;
        } catch (Exception e) {
            throw new RuntimeException("Échec de l'enregistrement de la facture générée", e);
        }
    }

    public String uploadArticlePhoto(MultipartFile file, Long articleId) {
        try {
            fileValidationService.validateImage(file);

            String original = file.getOriginalFilename();
            String safeName = articleId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = ARTICLE_PHOTOS_UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/article-photos/" + safeName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload photo de l'article", e);
        }
    }

    public String uploadFournisseurLogo(MultipartFile file, Long fournisseurId) {
        try {
            fileValidationService.validateImage(file);

            String original = file.getOriginalFilename();
            String safeName = fournisseurId + "_" + System.currentTimeMillis() + "_" +
                    original.replaceAll("[^a-zA-Z0-9.-]", "_");

            Path destination = FOURNISSEUR_LOGOS_UPLOAD_ROOT.resolve(safeName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            return "/uploads/fournisseur-logos/" + safeName;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Échec upload logo fournisseur", e);
        }
    }

    /**
     * Copie une facture déjà stockée sous uploads/commande-factures vers
     * uploads/documents, pour qu'elle rejoigne l'onglet Documents du projet
     * (servi par ProjetDocumentFileController, avec son propre contrôle
     * d'accès investisseurs/porteur/admin). Retourne le nom de fichier dans
     * son nouvel emplacement, à stocker tel quel sur Document.filename.
     */
    public String copierFactureVersDocuments(String factureUrl) {
        try {
            String filename = factureUrl.substring(factureUrl.lastIndexOf('/') + 1);
            Path source = COMMANDE_FACTURES_UPLOAD_ROOT.resolve(filename);
            Path destination = DOCUMENTS_UPLOAD_ROOT.resolve(filename);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (Exception e) {
            throw new RuntimeException("Échec de la copie de la facture vers les documents du projet", e);
        }
    }
}