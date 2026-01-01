package growzapp.backend.controller.api;

import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.KycStatus;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.KycStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
public class KycController {

    private final KycStorageService kycStorageService;
    private final UserRepository userRepository;

    /**
     * SOUMISSION DU KYC (Utilisateur connecté)
     * Contrainte : Un utilisateur ne peut soumettre que pour son propre ID
     */
    @PostMapping("/soumettre")
    @PreAuthorize("#userId == authentication.principal.id")
    public ResponseEntity<?> soumettreKyc(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dateNaissance") String dateNaissance,
            @RequestParam("adresse") String adresse,
            @RequestParam("userId") Long userId) {

        // 1. Validation de sécurité sur le fichier
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) { // Max 5Mo
            return ResponseEntity.badRequest().body("Fichier invalide ou trop volumineux (Max 5Mo)");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png")
                && !contentType.equals("application/pdf"))) {
            return ResponseEntity.badRequest().body("Format autorisé : JPG, PNG ou PDF uniquement");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Sauvegarde sécurisée
        String fileName = kycStorageService.save(file);

        // 3. Mise à jour de l'entité
        user.setKycDocumentUrl(fileName);
        user.setAdresseResidencielle(adresse);
        user.setDateNaissance(LocalDate.parse(dateNaissance));
        user.setKycStatus(KycStatus.EN_ATTENTE);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Document KYC soumis. En attente de validation."));
    }

    /**
     * LISTE DES DEMANDES (Admin uniquement)
     */
    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getDemandesEnAttente() {
        return ResponseEntity.ok(userRepository.findByKycStatus(KycStatus.EN_ATTENTE));
    }

    /**
     * VISIONNEUSE DE DOCUMENT (Admin uniquement)
     * Sert le fichier depuis le stockage privé
     */
    @GetMapping("/admin/document/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getKycDocument(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        try {
            Path filePath = Paths.get("uploads/kyc-documents").resolve(user.getKycDocumentUrl());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG) // Ou dynamique selon l'extension
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * VALIDATION OU REJET (Admin uniquement)
     */
    @PostMapping("/admin/decider")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deciderKyc(
            @RequestParam("userId") Long userId,
            @RequestParam("approuve") boolean approuve,
            @RequestParam(value = "commentaire", required = false) String commentaire) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (approuve) {
            user.setKycStatus(KycStatus.VALIDE);
            user.setKycDateValidation(LocalDateTime.now());
            user.setKycCommentaireRejet(null);
        } else {
            user.setKycStatus(KycStatus.REJETE);
            user.setKycCommentaireRejet(commentaire);
        }

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("status", user.getKycStatus()));
    }
}