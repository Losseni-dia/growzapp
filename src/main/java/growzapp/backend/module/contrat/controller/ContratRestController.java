package growzapp.backend.module.contrat.controller;

import growzapp.backend.module.contrat.dto.ContratPublicDTO;
import growzapp.backend.module.contrat.model.Contrat;
import growzapp.backend.module.contrat.service.ContratService;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
@Tag(name = "Contrats", description = "Vérification publique et consultation des contrats d'investissement au format PDF")
public class ContratRestController {

    private final PasswordEncoder passwordEncoder;
    private final ContratService contratService;

    private final Map<String, long[]> failureTracker = new ConcurrentHashMap<>();

    @PostMapping("/public/verifier-securise")
    @Operation(
        summary = "Vérifier l'authenticité d'un contrat",
        description = "Endpoint public sécurisé permettant de vérifier qu'un contrat est authentique. " +
            "Exige le numéro de contrat, l'email et le mot de passe de l'investisseur. " +
            "Protégé contre le brute-force : blocage de 24h après 5 tentatives échouées.",
        tags = {"Contrats"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Identifiants de vérification du contrat",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"numero\": \"CONTRAT-2025-00015\", \"email\": \"john.doe@example.com\", \"password\": \"motDePasse123!\"}"))
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Contrat valide — détails retournés",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ContratPublicDTO.class))),
        @ApiResponse(responseCode = "401", description = "Identifiants invalides",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"valide\": false, \"message\": \"Identifiants invalides. Tentatives restantes : 4\"}"))),
        @ApiResponse(responseCode = "403", description = "Compte bloqué pour 24h suite à trop de tentatives",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"valide\": false, \"message\": \"Trop de tentatives. Bloqué pour 24h.\"}"))),
        @ApiResponse(responseCode = "404", description = "Numéro de contrat introuvable",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"valide\": false, \"message\": \"Contrat introuvable\"}")))
    })
    public ResponseEntity<?> verifierContratSecurise(@RequestBody Map<String, String> payload) {
        String numero = payload.get("numero");
        String email = payload.get("email").toLowerCase().trim();
        String password = payload.get("password");

        long[] stats = failureTracker.get(email);
        if (stats != null && stats[1] > System.currentTimeMillis()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("valide", false, "message", "Trop de tentatives. Bloqué pour 24h."));
        }

        try {
            Contrat contrat = contratService.trouverParNumero(numero);
            User user = contrat.getInvestissement().getInvestisseur();

            if (!user.getEmail().equalsIgnoreCase(email) || !passwordEncoder.matches(password, user.getPassword())) {
                return handleFailedAttempt(email);
            }

            failureTracker.remove(email);

            return ResponseEntity.ok(Map.of(
                    "valide", true,
                    "numeroContrat", contrat.getNumeroContrat(),
                    "projet", contrat.getInvestissement().getProjet().getLibelle(),
                    "investisseur", user.getPrenom() + " " + user.getNom(),
                    "montant", contrat.getInvestissement().getMontantInvesti(),
                    "date", contrat.getDateGeneration().toLocalDate()));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("valide", false, "message", "Contrat introuvable"));
        }
    }

    @GetMapping("/{numero}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Visualiser un contrat PDF en ligne",
        description = "Sert le contrat au format PDF pour affichage inline dans le navigateur. L'utilisateur doit être le propriétaire du contrat ou un administrateur.",
        tags = {"Contrats"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF du contrat retourné",
            content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — l'utilisateur n'est pas propriétaire du contrat",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Contrat introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<Resource> voirContrat(
            @Parameter(description = "Numéro officiel du contrat", example = "CONTRAT-2025-00015", required = true)
            @PathVariable String numero,
            @Parameter(description = "Langue du contrat généré", example = "fr", schema = @Schema(allowableValues = {"fr", "en", "es"}))
            @RequestParam(defaultValue = "fr") String lang) throws Exception {
        Contrat contrat = contratService.trouverParNumero(numero);
        if (!contratService.utilisateurPeutVoirContrat(contrat))
            throw new AccessDeniedException("Accès refusé.");
        byte[] pdf = contratService.genererPdf(contrat, lang);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Contrat_" + numero + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/{numero}/download")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Télécharger un contrat PDF",
        description = "Télécharge le contrat au format PDF. Le paramètre 'view' permet de basculer entre affichage inline et téléchargement forcé.",
        tags = {"Contrats"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "PDF du contrat téléchargé",
            content = @Content(mediaType = "application/pdf")),
        @ApiResponse(responseCode = "403", description = "Accès refusé",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Contrat introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<Resource> downloadContrat(
            @Parameter(description = "Numéro officiel du contrat", example = "CONTRAT-2025-00015", required = true)
            @PathVariable String numero,
            @Parameter(description = "true = affichage inline, false = téléchargement forcé", example = "false")
            @RequestParam(defaultValue = "false") boolean view,
            @Parameter(description = "Langue du document", example = "fr", schema = @Schema(allowableValues = {"fr", "en", "es"}))
            @RequestParam(defaultValue = "fr") String lang) throws Exception {
        Contrat contrat = contratService.trouverParNumero(numero);
        if (!contratService.utilisateurPeutVoirContrat(contrat))
            throw new AccessDeniedException("Accès refusé.");
        byte[] pdf = contratService.genererPdf(contrat, lang);
        String disp = view ? "inline" : "attachment";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disp + "; filename=\"Contrat_" + numero + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    @GetMapping("/details/{numero}")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Détails d'un contrat (JSON)",
        description = "Retourne les informations clés d'un contrat au format JSON : numéro, investisseur et montant investi.",
        tags = {"Contrats"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Détails du contrat",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"numeroContrat\": \"CONTRAT-2025-00015\", \"investisseur\": {\"prenom\": \"John\", \"nom\": \"Doe\", \"email\": \"john.doe@example.com\"}, \"montantInvesti\": 2500.00}"))),
        @ApiResponse(responseCode = "403", description = "Accès refusé",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Contrat introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> getContratDetails(
            @Parameter(description = "Numéro officiel du contrat", example = "CONTRAT-2025-00015", required = true)
            @PathVariable String numero) {
        Contrat contrat = contratService.trouverParNumero(numero);
        if (!contratService.utilisateurPeutVoirContrat(contrat))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");

        Investissement inv = contrat.getInvestissement();
        User user = inv.getInvestisseur();

        Map<String, Object> response = new HashMap<>();
        response.put("numeroContrat", contrat.getNumeroContrat());
        response.put("investisseur",
                Map.of("prenom", user.getPrenom(), "nom", user.getNom(), "email", user.getEmail()));
        response.put("montantInvesti", inv.getMontantInvesti());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/liste")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "[Admin] Lister tous les contrats",
        description = "Retourne la liste paginée de tous les contrats générés sur la plateforme, triée par date de génération décroissante.",
        tags = {"Contrats"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page de contrats retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<?> listeContratsAdmin(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateGeneration").descending());
        Page<Contrat> resultats = contratService.rechercherAvecFiltres(null, null, null, null, null, null, pageable);
        return ResponseEntity.ok(resultats);
    }

    private ResponseEntity<?> handleFailedAttempt(String email) {
        long[] stats = failureTracker.getOrDefault(email, new long[]{0, 0});
        stats[0]++;

        if (stats[0] >= 5) {
            stats[1] = System.currentTimeMillis() + (24 * 60 * 60 * 1000);
            failureTracker.put(email, stats);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("valide", false, "message", "Compte bloqué pour 24h suite à 5 échecs."));
        } else {
            failureTracker.put(email, stats);
            long restant = 5 - stats[0];
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valide", false, "message", "Identifiants invalides. Tentatives restantes : " + restant));
        }
    }
}
