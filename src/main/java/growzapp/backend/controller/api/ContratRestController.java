package growzapp.backend.controller.api;

import growzapp.backend.model.entite.*;
import growzapp.backend.service.ContratService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ContratRestController {

    private final PasswordEncoder passwordEncoder;
    private final ContratService contratService;

    // ========================================================================
    // 1. VÉRIFICATION SÉCURISÉE (REMPLACE L'ANCIEN GET)
    // ========================================================================
   // Dans ContratRestController.java

// Stockage : Email -> [Nombre échecs, Timestamp fin de blocage]
private final Map<String, long[]> failureTracker = new ConcurrentHashMap<>();

@PostMapping("/public/verifier-securise")
public ResponseEntity<?> verifierContratSecurise(@RequestBody Map<String, String> payload) {
    String numero = payload.get("numero");
    String email = payload.get("email").toLowerCase().trim();
    String password = payload.get("password");

    // 1. Vérifier si bloqué
    long[] stats = failureTracker.get(email);
    if (stats != null && stats[1] > System.currentTimeMillis()) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("valide", false, "message", "Trop de tentatives. Bloqué pour 24h."));
    }

    try {
        Contrat contrat = contratService.trouverParNumero(numero);
        User user = contrat.getInvestissement().getInvestisseur();

        // 2. Vérification Mail + Password
        if (!user.getEmail().equalsIgnoreCase(email) || !passwordEncoder.matches(password, user.getPassword())) {
            return handleFailedAttempt(email);
        }

        // 3. Succès : Réinitialiser le tracker pour cet email
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

private ResponseEntity<?> handleFailedAttempt(String email) {
    long[] stats = failureTracker.getOrDefault(email, new long[]{0, 0});
    stats[0]++; // +1 échec

    if (stats[0] >= 5) {
        stats[1] = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // Bloque 24h
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

    // ========================================================================
    // 2. VOIR ET TÉLÉCHARGER (Gardés pour les autres besoins)
    // ========================================================================
    @GetMapping("/{numero}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> voirContrat(@PathVariable String numero,
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
    public ResponseEntity<Resource> downloadContrat(@PathVariable String numero,
            @RequestParam(defaultValue = "false") boolean view, @RequestParam(defaultValue = "fr") String lang)
            throws Exception {
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
    public ResponseEntity<?> getContratDetails(@PathVariable String numero) {
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
    public ResponseEntity<?> listeContratsAdmin(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateGeneration").descending());
        Page<Contrat> resultats = contratService.rechercherAvecFiltres(null, null, null, null, null, null, pageable);
        return ResponseEntity.ok(resultats);
    }
}