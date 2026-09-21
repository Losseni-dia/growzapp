package growzapp.backend.module.files.validation;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

/**
 * Valide le VRAI contenu d'un fichier uploadé (signature binaire, "magic
 * bytes"), pas juste son extension ou son Content-Type déclaré — les deux
 * sont facilement falsifiables par un client malveillant (HIGH-04 de
 * l'audit).
 */
@Service
public class FileValidationService {

    private final Tika tika = new Tika();

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "application/pdf");

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 Mo

    public void validateImage(MultipartFile file) throws IOException {
        validate(file, IMAGE_TYPES);
    }

    /**
     * Comme {@link #validateImage}, avec en plus un contrôle de résolution
     * minimale — sans ce garde-fou, une photo déjà minuscule (capture
     * d'écran recadrée, image compressée par WhatsApp...) passait sans
     * problème puis apparaissait floue/pixelisée une fois affichée, même
     * dans un cadre de petite taille (l'agrandissement d'une image trop
     * petite est visible dès qu'on dépasse sa résolution native).
     */
    public void validateImageMinDimensions(MultipartFile file, int minWidth, int minHeight) throws IOException {
        validate(file, IMAGE_TYPES);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
        if (image == null) {
            throw new IllegalArgumentException("Impossible de lire cette image — fichier corrompu ?");
        }
        if (image.getWidth() < minWidth || image.getHeight() < minHeight) {
            throw new IllegalArgumentException(
                    "Image trop petite (" + image.getWidth() + "x" + image.getHeight()
                            + " px) — minimum requis : " + minWidth + "x" + minHeight + " px pour un rendu net.");
        }
    }

    public void validateDocument(MultipartFile file) throws IOException {
        validate(file, DOCUMENT_TYPES);
    }

    private void validate(MultipartFile file, Set<String> allowedTypes) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier vide ou manquant");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Fichier trop volumineux (max 10 Mo)");
        }

        // Détecte le vrai type MIME à partir du contenu binaire réel du
        // fichier, indépendamment de son extension ou du Content-Type
        // déclaré par le client.
        String detectedType = tika.detect(file.getBytes());

        if (!allowedTypes.contains(detectedType)) {
            throw new IllegalArgumentException(
                    "Type de fichier non autorisé : " + detectedType
                            + " (types acceptés : " + allowedTypes + ")");
        }
    }
}