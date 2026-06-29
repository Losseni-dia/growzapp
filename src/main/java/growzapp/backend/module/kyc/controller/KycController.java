package growzapp.backend.module.kyc.controller;

import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.kyc.service.KycStorageService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.dto.UserDTO;
import growzapp.backend.module.user.mapper.UserMapper;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kyc")
@RequiredArgsConstructor
@Tag(name = "KYC", description = "Vérification d'identité : soumission de documents et décision admin")
public class KycController {

    private final KycStorageService kycStorageService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @PostMapping(value = "/soumettre", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Soumettre son dossier KYC", tags = { "KYC" })
    public ResponseEntity<?> soumettreKyc(
            @RequestParam("fileRecto") MultipartFile fileRecto,
            @RequestParam(value = "fileVerso", required = false) MultipartFile fileVerso,
            @RequestParam("fileSelfie") MultipartFile fileSelfie,
            @RequestParam("dateNaissance") String dateNaissance,
            @RequestParam("adresse") String adresse,
            @RequestParam("numeroPiece") String numeroPiece,
            @RequestParam("dateDelivrance") String dateDelivrance,
            @RequestParam("dateExpiration") String dateExpiration,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getCurrentUserId(userDetails);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setKycRectoUrl(kycStorageService.save(fileRecto));
        if (fileVerso != null && !fileVerso.isEmpty()) {
            user.setKycVersoUrl(kycStorageService.save(fileVerso));
        }
        user.setKycSelfieUrl(kycStorageService.save(fileSelfie));
        user.setKycNumeroPiece(numeroPiece);
        user.setKycDateDelivrance(LocalDate.parse(dateDelivrance));
        user.setKycDateExpiration(LocalDate.parse(dateExpiration));
        user.setDateNaissance(LocalDate.parse(dateNaissance));
        user.setAdresseResidencielle(adresse);
        user.setKycStatus(KycStatus.EN_ATTENTE);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message",
                "Votre dossier KYC a été soumis avec succès et est en cours de révision."));
    }

    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Lister les dossiers KYC en attente", tags = { "KYC" })
    public ResponseEntity<List<UserDTO>> getDemandesEnAttente() {
        List<User> users = userRepository.findByKycStatus(KycStatus.EN_ATTENTE);
        return ResponseEntity.ok(users.stream().map(userMapper::toDto).toList());
    }

    @GetMapping("/admin/document/{userId}/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Visualiser un document KYC", tags = { "KYC" })
    public ResponseEntity<Resource> getKycDocument(
            @PathVariable Long userId,
            @PathVariable String type) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String fileName = switch (type.toLowerCase()) {
            case "recto" -> user.getKycRectoUrl();
            case "verso" -> user.getKycVersoUrl();
            case "selfie" -> user.getKycSelfieUrl();
            default -> null;
        };

        if (fileName == null)
            return ResponseEntity.notFound().build();

        try {
            Path rootPath = Paths.get("uploads", "private", "kyc-documents").toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null)
                    contentType = "image/jpeg";
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/admin/decider")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "[Admin] Approuver ou rejeter un dossier KYC", description = "En cas d'approbation → statut VALIDE + notification + email. En cas de rejet → statut REJETE + motif + notification + email.", tags = {
            "KYC" })
    public ResponseEntity<?> deciderKyc(
            @RequestParam("userId") Long userId,
            @RequestParam("approuve") boolean approuve,
            @RequestParam(value = "commentaire", required = false) String commentaire) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String nomComplet = user.getPrenom() + " " + user.getNom();

        if (approuve) {
            user.setKycStatus(KycStatus.VALIDE);
            user.setKycDateValidation(LocalDateTime.now());
            user.setKycCommentaireRejet(null);
            userRepository.save(user);

            // Notification in-app
            notificationService.notifyUser(
                    user,
                    "✅ KYC validé !",
                    "Félicitations ! Votre identité a été vérifiée et validée. " +
                            "Vous pouvez maintenant investir sur GrowzApp.",
                    null, null, null);

            // Email
            emailService.envoyerKycValide(user.getEmail(), nomComplet);

        } else {
            String motif = (commentaire != null && !commentaire.isBlank())
                    ? commentaire
                    : "Document non conforme";

            user.setKycStatus(KycStatus.REJETE);
            user.setKycCommentaireRejet(motif);
            userRepository.save(user);

            // Notification in-app avec motif
            notificationService.notifyUser(
                    user,
                    "❌ KYC refusé",
                    "Votre dossier KYC a été refusé. Motif : " + motif +
                            ". Vous pouvez soumettre un nouveau dossier depuis votre profil.",
                    null, null, motif);

            // Email avec motif
            emailService.envoyerKycRefuse(user.getEmail(), nomComplet, motif);
        }

        return ResponseEntity.ok(Map.of("status", user.getKycStatus()));
    }

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + userDetails.getUsername()));
    }
}