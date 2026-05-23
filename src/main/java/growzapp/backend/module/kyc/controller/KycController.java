package growzapp.backend.module.kyc.controller;

import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.kyc.service.KycStorageService;
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
@Tag(name = "KYC", description = "Vérification d'identité (Know Your Customer) : soumission de documents par l'utilisateur et décision par l'administrateur")
public class KycController {

    private final KycStorageService kycStorageService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @PostMapping(value = "/soumettre", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Soumettre son dossier KYC",
        description = "L'utilisateur connecté soumet ses documents d'identité (recto, verso optionnel, selfie) ainsi que ses informations personnelles. Le statut passe automatiquement à EN_ATTENTE.",
        tags = {"KYC"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dossier KYC soumis avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"message\": \"Votre dossier KYC a été soumis avec succès et est en cours de révision.\"}"))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> soumettreKyc(
            @Parameter(description = "Photo du recto de la pièce d'identité", required = true,
                schema = @Schema(type = "string", format = "binary"))
            @RequestParam("fileRecto") MultipartFile fileRecto,

            @Parameter(description = "Photo du verso de la pièce d'identité (optionnel)",
                schema = @Schema(type = "string", format = "binary"))
            @RequestParam(value = "fileVerso", required = false) MultipartFile fileVerso,

            @Parameter(description = "Selfie de l'utilisateur tenant sa pièce d'identité", required = true,
                schema = @Schema(type = "string", format = "binary"))
            @RequestParam("fileSelfie") MultipartFile fileSelfie,

            @Parameter(description = "Date de naissance au format ISO (YYYY-MM-DD)", required = true, example = "1990-05-20")
            @RequestParam("dateNaissance") String dateNaissance,

            @Parameter(description = "Adresse de résidence actuelle", required = true, example = "12 Rue des Fleurs, Ouagadougou")
            @RequestParam("adresse") String adresse,

            @Parameter(description = "Numéro de la pièce d'identité", required = true, example = "BF123456789")
            @RequestParam("numeroPiece") String numeroPiece,

            @Parameter(description = "Date de délivrance de la pièce d'identité au format ISO (YYYY-MM-DD)", required = true, example = "2020-01-15")
            @RequestParam("dateDelivrance") String dateDelivrance,

            @Parameter(description = "Date d'expiration de la pièce d'identité au format ISO (YYYY-MM-DD)", required = true, example = "2030-01-14")
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

        return ResponseEntity.ok(Map.of("message", "Votre dossier KYC a été soumis avec succès et est en cours de révision."));
    }

    @GetMapping("/admin/en-attente")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Lister les dossiers KYC en attente",
        description = "Retourne la liste de tous les utilisateurs dont le statut KYC est EN_ATTENTE. Réservé aux administrateurs.",
        tags = {"KYC"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des dossiers en attente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<UserDTO>> getDemandesEnAttente() {
        List<User> users = userRepository.findByKycStatus(KycStatus.EN_ATTENTE);

        List<UserDTO> dtos = users.stream()
                .map(userMapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/admin/document/{userId}/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Visualiser un document KYC",
        description = "Sert le fichier image d'un document KYC depuis le stockage privé (recto, verso ou selfie). Retourne le fichier inline pour visualisation directe. Réservé aux administrateurs.",
        tags = {"KYC"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Fichier image retourné",
            content = @Content(mediaType = "image/jpeg")),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Document ou utilisateur introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Erreur de lecture du fichier",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<Resource> getKycDocument(
            @Parameter(description = "Identifiant de l'utilisateur", example = "42", required = true)
            @PathVariable Long userId,

            @Parameter(description = "Type de document à récupérer", example = "recto",
                schema = @Schema(allowableValues = {"recto", "verso", "selfie"}), required = true)
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
    @Operation(
        summary = "[Admin] Approuver ou rejeter un dossier KYC",
        description = "L'administrateur statue sur un dossier KYC soumis. En cas d'approbation, le statut passe à VALIDE. En cas de rejet, le statut passe à REJETE et un commentaire est enregistré.",
        tags = {"KYC"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Décision enregistrée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"status\": \"VALIDE\"}"))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Utilisateur non trouvé",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> deciderKyc(
            @Parameter(description = "Identifiant de l'utilisateur dont le dossier est en cours de révision", example = "42", required = true)
            @RequestParam("userId") Long userId,

            @Parameter(description = "true pour approuver, false pour rejeter", example = "true", required = true)
            @RequestParam("approuve") boolean approuve,

            @Parameter(description = "Motif du rejet (obligatoire si approuve = false)", example = "Document expiré ou illisible.")
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

    private Long getCurrentUserId(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .map(User::getId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec le login : " + userDetails.getUsername()));
    }
}
