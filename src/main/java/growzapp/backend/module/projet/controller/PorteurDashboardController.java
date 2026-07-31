package growzapp.backend.module.projet.controller;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.projet.dto.PorteurDashboardDTO;
import growzapp.backend.module.projet.dto.PorteurProjetLigneDTO;
import growzapp.backend.module.projet.dto.ValorisationSnapshotDTO;
import growzapp.backend.module.projet.dto.VelocitePointDTO;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetValorisation;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.projet.repository.ProjetValorisationRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.traduction.DeepL.model.ProjetTraductionProjection;
import growzapp.backend.module.traduction.DeepL.repository.ProjetTraductionRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.WalletRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/projets/mes-projets", "/api/projets/mes-projets"})
@RequiredArgsConstructor
@Tag(name = "Porteur - Tableau de bord", description = "Statistiques agrégées pour les porteurs de projet. Aucune identité d'investisseur n'est jamais exposée ici.")
public class PorteurDashboardController {

    private final ProjetRepository projetRepository;
    private final InvestissementRepository investissementRepository;
    private final DividendeRepository dividendeRepository;
    private final WalletRepository walletRepository;
    private final ProjetValorisationRepository projetValorisationRepository;
    private final ProjetTraductionRepository traductionRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter MOIS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Operation(summary = "Mon tableau de bord porteur", description = "Retourne les statistiques agrégées de tous les projets du porteur connecté. Aucune identité d'investisseur individuelle n'est jamais retournée.")
    @GetMapping("/dashboard-porteur")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    public ApiResponseDTO<PorteurDashboardDTO> getMonDashboardPorteur(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "fr") String langue) {

        User porteur = userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Projet> mesProjets = projetRepository.findByPorteurId(porteur.getId());

        List<PorteurProjetLigneDTO> lignes = new ArrayList<>();
        BigDecimal totalCollecte = BigDecimal.ZERO;
        int totalInvestisseurs = 0;
        BigDecimal totalDividendesVerses = BigDecimal.ZERO;

        for (Projet projet : mesProjets) {
            List<Investissement> investissementsValides = investissementRepository
                    .findByProjetId(projet.getId())
                    .stream()
                    .filter(i -> i.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                    .toList();

            // ── Agrégats investisseurs — jamais d'identité, uniquement des compteurs ──
            Set<Long> investisseursDistincts = new HashSet<>();
            BigDecimal montantTotalInvesti = BigDecimal.ZERO;
            for (Investissement inv : investissementsValides) {
                if (inv.getInvestisseur() != null) {
                    investisseursDistincts.add(inv.getInvestisseur().getId());
                }
                if (inv.getMontantInvesti() != null) {
                    montantTotalInvesti = montantTotalInvesti.add(inv.getMontantInvesti());
                }
            }
            int nbInvestisseurs = investisseursDistincts.size();
            BigDecimal montantMoyen = nbInvestisseurs > 0
                    ? montantTotalInvesti.divide(BigDecimal.valueOf(nbInvestisseurs), MathContext.DECIMAL128)
                    : BigDecimal.ZERO;

            // ── Vitesse de levée — agrégat mensuel ──
            Map<String, List<Investissement>> parMois = investissementsValides.stream()
                    .filter(i -> i.getDate() != null)
                    .collect(Collectors.groupingBy(i -> i.getDate().format(MOIS_FORMAT)));

            List<VelocitePointDTO> vitesseLevee = parMois.entrySet().stream()
                    .map(entry -> {
                        BigDecimal montantMois = entry.getValue().stream()
                                .map(Investissement::getMontantInvesti)
                                .filter(java.util.Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        return new VelocitePointDTO(entry.getKey(), montantMois, entry.getValue().size());
                    })
                    .sorted((a, b) -> a.periode().compareTo(b.periode()))
                    .toList();

            // ── Wallet projet ──
            Optional<Wallet> walletOpt = walletRepository.findByProjetIdAndWalletType(projet.getId(),
                    WalletType.PROJET);
            BigDecimal soldeDisponible = walletOpt.map(Wallet::getSoldeDisponible).orElse(BigDecimal.ZERO);
            BigDecimal soldeBloque = walletOpt.map(Wallet::getSoldeBloque).orElse(BigDecimal.ZERO);

            // ── Dividendes versés (montant agrégé uniquement) ──
            List<Dividende> dividendesProjet = dividendeRepository
                            .findByInvestissement_Projet_Id(projet.getId());
            BigDecimal dividendesVerses = dividendesProjet.stream()
                            .map(Dividende::getMontantTotal)
                            .filter(java.util.Objects::nonNull)
                            .map(BigDecimal::valueOf)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            // ── Historique de collecte ──
            List<ProjetValorisation> historique = projetValorisationRepository
                    .findByProjetIdOrderByDateSnapshotAsc(projet.getId());
            List<ValorisationSnapshotDTO> historiqueDto = historique.stream()
                    .map(v -> new ValorisationSnapshotDTO(
                            v.getDateSnapshot(),
                            v.getMontantValorisation(),
                            v.getMontantCollecte(),
                            v.getTypeEvenement(),
                            v.getMontantEvenement()))
                    .toList();

            double progression = projet.getObjectifFinancement() != null
                    && projet.getObjectifFinancement().compareTo(BigDecimal.ZERO) > 0
                            ? projet.getMontantCollecte()
                                    .divide(projet.getObjectifFinancement(), MathContext.DECIMAL128)
                                    .multiply(BigDecimal.valueOf(100))
                                    .doubleValue()
                            : 0.0;

            String libelleTradu = null;
            if (langue != null && !langue.isBlank() && !langue.equals("fr")) {
                Optional<ProjetTraductionProjection> traduction = traductionRepository
                        .findProjectionByProjetIdAndLangue(projet.getId(), langue);
                if (traduction.isPresent() && traduction.get().getLibelle() != null) {
                    libelleTradu = traduction.get().getLibelle();
                }
            }

            PorteurProjetLigneDTO ligne = PorteurProjetLigneDTO.builder()
                    .projetId(projet.getId())
                    .projetLibelle(projet.getLibelle())
                    .projetLibelleTradu(libelleTradu)
                    .projetPoster(projet.getPoster())
                    .statutProjet(projet.getStatutProjet() != null ? projet.getStatutProjet().name() : null)
                    .objectifFinancement(projet.getObjectifFinancement())
                    .montantCollecte(projet.getMontantCollecte())
                    .progressionPourcent(progression)
                    .nombreInvestisseurs(nbInvestisseurs)
                    .montantMoyenParInvestisseur(montantMoyen)
                    .soldeDisponibleWallet(soldeDisponible)
                    .soldeBloqueWallet(soldeBloque)
                    .totalDividendesVerses(dividendesVerses)
                    .historiqueCollecte(historiqueDto)
                    .vitesseLevee(vitesseLevee)
                    .build();

            lignes.add(ligne);

            totalCollecte = totalCollecte.add(projet.getMontantCollecte() != null ? projet.getMontantCollecte()
                    : BigDecimal.ZERO);
            totalInvestisseurs += nbInvestisseurs;
            totalDividendesVerses = totalDividendesVerses.add(dividendesVerses);
        }

        PorteurDashboardDTO dashboard = new PorteurDashboardDTO(
                lignes.size(),
                totalCollecte,
                totalInvestisseurs,
                totalDividendesVerses,
                lignes);

        return ApiResponseDTO.success(dashboard);
    }
}