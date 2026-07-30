package growzapp.backend.module.files.validation;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
            "image/jpeg", "image/png", "application/pdf");

    private static final long MAX_SIZE_BYTES = 10 * 1024 * 1024; // 10 Mo

    public void validateImage(MultipartFile file) throws IOException {
        validate(file, IMAGE_TYPES);
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