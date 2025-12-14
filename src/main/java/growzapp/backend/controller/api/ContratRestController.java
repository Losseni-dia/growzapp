// src/main/java/growzapp/backend/controller/api/ContratRestController.java → VERSION FINALE 2025

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // Adapte selon ton port frontend
public class ContratRestController {

    private final ContratService contratService;

    // ========================================================================
    // 1. VOIR LE CONTRAT (NAVIGATEUR) - MULTILINGUE
    // ========================================================================
    @GetMapping("/{numero}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> voirContrat(
            @PathVariable String numero,
            @RequestParam(name = "lang", defaultValue = "fr") String lang // ← Paramètre Langue
    ) throws Exception {

        Contrat contrat = contratService.trouverParNumero(numero);

        // Vérification sécurité
        if (!contratService.utilisateurPeutVoirContrat(contrat)) {
            throw new AccessDeniedException("Vous n'avez pas l'autorisation de voir ce contrat.");
        }

        // Génération (ou récupération) du PDF dans la langue demandée
        byte[] pdf = contratService.genererPdf(contrat, lang);

        return ResponseEntity.ok()
                // "inline" permet l'affichage dans le navigateur
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"Contrat_" + numero + "_" + lang + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    // ========================================================================
    // 2. TÉLÉCHARGER LE CONTRAT - MULTILINGUE
    // ========================================================================
    @GetMapping("/{numero}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadContrat(
            @PathVariable String numero,
            @RequestParam(defaultValue = "false") boolean view,
            @RequestParam(name = "lang", defaultValue = "fr") String lang // ← Paramètre Langue
    ) throws Exception {

        Contrat contrat = contratService.trouverParNumero(numero);

        if (!contratService.utilisateurPeutVoirContrat(contrat)) {
            throw new AccessDeniedException("Accès refusé.");
        }

        byte[] pdf = contratService.genererPdf(contrat, lang);

        // "attachment" force le téléchargement, "inline" l'affiche
        String disposition = view ? "inline" : "attachment";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"Contrat_" + numero + "_" + lang + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(pdf));
    }

    // ========================================================================
    // 3. VÉRIFICATION PUBLIQUE (QR CODE) - SANS AUTH
    // ========================================================================
    @GetMapping("/public/verifier/{numero}")
    public ResponseEntity<?> verifierContratPublic(@PathVariable String numero) {
        try {
            Contrat contrat = contratService.trouverParNumero(numero);
            Investissement inv = contrat.getInvestissement();

            // On renvoie un objet JSON léger et anonymisé
            Map<String, Object> response = Map.of(
                    "valide", true,
                    "numeroContrat", contrat.getNumeroContrat(),
                    "projet", inv.getProjet().getLibelle(),
                    "investisseur", masquerNomInvestisseur(inv.getInvestisseur()),
                    "montant", inv.getMontantInvesti(),
                    "date", contrat.getDateGeneration().toLocalDate());

            return ResponseEntity.ok(response);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("valide", false, "message", "Contrat introuvable"));
        }
    }

    // Utilitaire pour cacher le nom complet (RGPD / Privacy)
    private String masquerNomInvestisseur(User user) {
        if (user == null || user.getPrenom() == null || user.getNom() == null)
            return "Investisseur Anonyme";
        // Exemple : "Jean D."
        return user.getPrenom() + " " + user.getNom().charAt(0) + ".";
    }

    // ========================================================================
    // 4. DÉTAILS JSON (POUR LA PAGE "MON CONTRAT")
    // ========================================================================
    @GetMapping("/details/{numero}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getContratDetails(@PathVariable String numero) {
        Contrat contrat = contratService.trouverParNumero(numero);

        if (!contratService.utilisateurPeutVoirContrat(contrat)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Accès refusé");
        }

        Investissement inv = contrat.getInvestissement();
        Projet projet = inv.getProjet();
        User user = inv.getInvestisseur();

        // Construction du JSON structuré pour le Frontend React
        Map<String, Object> response = new HashMap<>();
        response.put("numeroContrat", contrat.getNumeroContrat());
        response.put("dateGeneration", contrat.getDateGeneration());
        response.put("lienVerification", contrat.getLienVerification());

        response.put("investisseur", Map.of(
                "prenom", user.getPrenom(),
                "nom", user.getNom(),
                "email", user.getEmail(), // Utile pour l'affichage
                "telephone", user.getContact() != null ? user.getContact() : ""));

        response.put("projet", Map.of(
                "libelle", projet.getLibelle(),
                "prixUnePart", projet.getPrixUnePart(),
                "dureeMois", projet.getDureeMois(),
                "porteurNom", projet.getPorteur() != null ? projet.getPorteur().getNom() : "Inconnu"));

        response.put("investissement", Map.of(
                "nombrePartsPris", inv.getNombrePartsPris(),
                "montantInvesti", inv.getMontantInvesti(),
                "valeurPartsPrisEnPourcent", inv.getValeurPartsPrisEnPourcent(),
                "statutInvestissement", inv.getStatutPartInvestissement()));

        // URL pour télécharger le PDF par défaut
        response.put("lienPdf", "/api/contrats/" + contrat.getNumeroContrat() + "/download");

        return ResponseEntity.ok(response);
    }

    // ========================================================================
    // 5. LISTE ADMIN (TABLEAU DE BORD)
    // ========================================================================
    @GetMapping("/admin/liste")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> listeContratsAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Integer montantMin,
            @RequestParam(required = false) Integer montantMax,
            @RequestParam(defaultValue = "dateGeneration,desc") String sort) {

        try {
            // Gestion du tri (ex: "montantInvesti,asc")
            Sort sortBy = sort.contains(",")
                    ? Sort.by(Sort.Direction.fromString(sort.split(",")[1]), sort.split(",")[0])
                    : Sort.by(Sort.Direction.DESC, "dateGeneration");

            Pageable pageable = PageRequest.of(page, size, sortBy);

            // Appel Service
            Page<Contrat> resultats = contratService.rechercherAvecFiltres(
                    search, dateDebut, dateFin, statut, montantMin, montantMax, pageable);

            // Transformation en JSON plat pour le tableau React
            List<Map<String, Object>> contratsFormates = resultats.getContent().stream()
                    .map(contrat -> {
                        Investissement inv = contrat.getInvestissement();
                        Projet projet = inv.getProjet();
                        User investisseur = inv.getInvestisseur();

                        Map<String, Object> map = new HashMap<>();
                        map.put("id", contrat.getId());
                        map.put("numeroContrat", contrat.getNumeroContrat());
                        map.put("dateGeneration", contrat.getDateGeneration());
                        map.put("projet", projet.getLibelle());
                        map.put("investisseur", investisseur.getPrenom() + " " + investisseur.getNom());
                        map.put("emailInvestisseur", investisseur.getEmail());
                        map.put("telephone", investisseur.getContact() != null ? investisseur.getContact() : "");
                        map.put("montantInvesti", inv.getMontantInvesti());
                        map.put("nombreParts", inv.getNombrePartsPris());
                        map.put("pourcentage", inv.getValeurPartsPrisEnPourcent());
                        map.put("statutInvestissement", inv.getStatutPartInvestissement().name());
                        map.put("fichierUrl", contrat.getFichierUrl());
                        map.put("lienVerification", contrat.getLienVerification());
                        map.put("lienPdf", "/api/contrats/" + contrat.getNumeroContrat() + "/download");

                        return map;
                    })
                    .toList();

            // Construction de la réponse paginée
            Map<String, Object> response = Map.of(
                    "contrats", contratsFormates,
                    "pageActuelle", resultats.getNumber(),
                    "totalPages", resultats.getTotalPages(),
                    "totalContrats", resultats.getTotalElements(), // IMPORTANT pour le Dashboard
                    "hasNext", resultats.hasNext(),
                    "hasPrevious", resultats.hasPrevious());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur serveur : " + e.getMessage());
        }
    }
}