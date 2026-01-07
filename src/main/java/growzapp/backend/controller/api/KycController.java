package growzapp.backend.controller.api;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.userDTO.UserDTO;
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
public class KycController {

    private final KycStorageService kycStorageService;
    private final UserRepository userRepository;
    private final DtoConverter converter;

    /**
     * Méthode utilitaire pour extraire l'ID de l'utilisateur à partir du login (username)
     */
    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec le login : " + userDetails.getUsername()));
    }

    /**
     * SOUMISSION DU KYC (Utilisateur connecté)
     * Utilise @AuthenticationPrincipal pour identifier l'utilisateur via le token JWT
     */
    @PostMapping(value = "/soumettre", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
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

        // 1. Identification sécurisée de l'utilisateur
        Long userId = getCurrentUserId(userDetails);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // 2. Sauvegarde des images et mise à jour des URLs
        user.setKycRectoUrl(kycStorageService.save(fileRecto));
        
        if (fileVerso != null && !fileVerso.isEmpty()) {
            user.setKycVersoUrl(kycStorageService.save(fileVerso));
        }
        
        user.setKycSelfieUrl(kycStorageService.save(fileSelfie));

        // 3. Mise à jour des informations d'identité
        user.setKycNumeroPiece(numeroPiece);
        user.setKycDateDelivrance(LocalDate.parse(dateDelivrance));
        user.setKycDateExpiration(LocalDate.parse(dateExpiration));
        user.setDateNaissance(LocalDate.parse(dateNaissance));
        user.setAdresseResidencielle(adresse);
        
        // Passage du statut en attente
        user.setKycStatus(KycStatus.EN_ATTENTE);

        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Votre dossier KYC a été soumis avec succès et est en cours de révision."));
    }

    /**
     * LISTE DES DEMANDES (Admin uniquement)
     */
    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getDemandesEnAttente() {
        List<User> users = userRepository.findByKycStatus(KycStatus.EN_ATTENTE);
        
        // On transforme la liste d'entités en liste de DTO pour couper les boucles infinies
        List<UserDTO> dtos = users.stream()
                .map(converter::toUserDto)
                .toList();
                
        return ResponseEntity.ok(dtos);
    }

    /**
     * VISIONNEUSE DE DOCUMENT (Admin uniquement)
     * Sert le fichier depuis le stockage privé
     */
    @GetMapping("/admin/document/{userId}/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> getKycDocument(@PathVariable Long userId, @PathVariable String type) {
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
            // CONSTRUCTION DU CHEMIN ABSOLU SÉCURISÉE
            Path rootPath = Paths.get("uploads", "private", "kyc-documents").toAbsolutePath().normalize();
            Path filePath = rootPath.resolve(fileName).normalize();

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                // Détection du type de fichier
                String contentType = Files.probeContentType(filePath);
                if (contentType == null)
                    contentType = "image/jpeg";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                System.err.println("Fichier introuvable au chemin : " + filePath.toString());
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