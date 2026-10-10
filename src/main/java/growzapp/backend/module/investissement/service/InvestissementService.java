package growzapp.backend.module.investissement.service;

import growzapp.backend.module.investissement.dto.InvestissementCreateDTO;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.dto.PortefeuilleDTO;
import growzapp.backend.module.investissement.dto.PortefeuilleLigneDTO;
import growzapp.backend.module.investissement.enums.ChoixEcheance;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.mapper.InvestissementMapper;
import growzapp.backend.module.investissement.model.DecisionEcheanceInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.DecisionEcheanceInvestissementRepository;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.contrat.service.ContratService;
import growzapp.backend.module.wallet.service.WalletService;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.dividende.dto.DividendeSnapshotDTO;
import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.projet.dto.ValorisationSnapshotDTO;
import growzapp.backend.module.projet.enums.StatutProjet;
import growzapp.backend.module.projet.enums.TypeEvenementValorisation;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetValorisation;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.projet.repository.ProjetValorisationRepository;
import growzapp.backend.module.projet.service.ProjetValorisationService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.user.service.UserService;
import growzapp.backend.module.wallet.enums.SourcePaiement;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvestissementService {

        private final InvestissementRepository investissementRepository;
        private final InvestissementMapper investissementMapper;
        private final ProjetRepository projetRepository;
        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final TransactionRepository transactionRepository;
        private final NotificationService notificationService;
        private final EmailService emailService;
        private final ProjetValorisationService projetValorisationService;
        private final ProjetValorisationRepository projetValorisationRepository;
        private final DividendeRepository dividendeRepository;
        private final UserService userService;
        private final WalletService walletService;
        private final ContratService contratService;
        private final DecisionEcheanceInvestissementRepository decisionEcheanceInvestissementRepository;



        public PortefeuilleDTO getPortefeuille(Long investisseurId) {
                List<Investissement> positions = investissementRepository.findByInvestisseurId(investisseurId).stream()
                                .filter(inv -> inv.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                                .toList();

                BigDecimal totalInvesti = BigDecimal.ZERO;
                BigDecimal valeurActuelleTotale = BigDecimal.ZERO;
                BigDecimal totalDividendesPercus = BigDecimal.ZERO;

                List<PortefeuilleLigneDTO> lignes = new java.util.ArrayList<>();

                for (Investissement inv : positions) {
                        Projet projet = inv.getProjet();
                        InvestissementDTO invDto = investissementMapper.toDto(inv);

                        BigDecimal valorisationActuelle = projet.getValuation() != null ? projet.getValuation()
                                        : BigDecimal.ZERO;
                        double pourcentageDetenu = inv.getValeurPartsPrisEnPourcent();

                        BigDecimal valeurPosition = valorisationActuelle
                                        .multiply(BigDecimal.valueOf(pourcentageDetenu))
                                        .divide(BigDecimal.valueOf(100), java.math.MathContext.DECIMAL128);

                        BigDecimal montantInvesti = inv.getMontantInvesti() != null ? inv.getMontantInvesti()
                                        : BigDecimal.ZERO;

                        double performance = montantInvesti.compareTo(BigDecimal.ZERO) > 0
                                        ? valeurPosition.subtract(montantInvesti)
                                                        .divide(montantInvesti, java.math.MathContext.DECIMAL128)
                                                        .multiply(BigDecimal.valueOf(100))
                                                        .doubleValue()
                                        : 0.0;

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

                      List<Dividende> dividendes = dividendeRepository
                                      .findByInvestissementId(inv.getId())
                                      .stream()
                                      .sorted(java.util.Comparator.comparing(
                                                      Dividende::getDatePaiement,
                                                      java.util.Comparator
                                                                      .nullsLast(java.util.Comparator.naturalOrder())))
                                      .toList();

                        List<DividendeSnapshotDTO> dividendesDetail = dividendes.stream()
                                        .map(d -> new DividendeSnapshotDTO(
                                                        d.getId(),
                                                        d.getDatePaiement() != null
                                                                        ? d.getDatePaiement().toLocalDate()
                                                                        : null,
                                                        d.getMontantParPart() != null
                                                                        ? d.getMontantParPart().multiply(
                                                                                        java.math.BigDecimal.valueOf(
                                                                                                        inv.getNombrePartsPris()))
                                                                        : java.math.BigDecimal.ZERO,
                                                        d.getStatutDividende() != null
                                                                        ? d.getStatutDividende().name()
                                                                        : null,
                                                        d.getMotif()))
                                        .toList();

                        PortefeuilleLigneDTO ligne = PortefeuilleLigneDTO
                                        .builder()
                                        .investissementId(inv.getId())
                                        .projetId(projet.getId())
                                        .projetLibelle(projet.getLibelle())
                                        .projetPoster(projet.getPoster())
                                        .statutProjet(projet.getStatutProjet() != null
                                                        ? projet.getStatutProjet().name()
                                                        : null)
                                        .dateInvestissement(inv.getDate())
                                        .nombrePartsPris(inv.getNombrePartsPris())
                                        .pourcentageDetenu(pourcentageDetenu)
                                        .montantInvesti(montantInvesti)
                                        .valorisationActuelle(valorisationActuelle)
                                        .valeurPositionActuelle(valeurPosition)
                                        .performancePourcent(performance)
                                        .dividendesPercus(invDto.montantTotalPercu())
                                        .historiqueValorisation(historiqueDto)
                                        .dividendesDetail(dividendesDetail)
                                        .build();

                        lignes.add(ligne);

                        totalInvesti = totalInvesti.add(montantInvesti);
                        valeurActuelleTotale = valeurActuelleTotale.add(valeurPosition);
                        totalDividendesPercus = totalDividendesPercus.add(
                                        invDto.montantTotalPercu() != null ? invDto.montantTotalPercu()
                                                        : BigDecimal.ZERO);
                }

                double performanceGlobale = totalInvesti.compareTo(BigDecimal.ZERO) > 0
                                ? valeurActuelleTotale.subtract(totalInvesti)
                                                .divide(totalInvesti, java.math.MathContext.DECIMAL128)
                                                .multiply(BigDecimal.valueOf(100))
                                                .doubleValue()
                                : 0.0;

                return new growzapp.backend.module.investissement.dto.PortefeuilleDTO(
                                totalInvesti,
                                valeurActuelleTotale,
                                totalDividendesPercus,
                                performanceGlobale,
                                lignes.size(),
                                lignes);
        }

        public List<InvestissementDTO> getAll() {
                return investissementRepository.findAll().stream()
                                .map(investissementMapper::toDto).toList();
        }

        public Page<InvestissementDTO> getAllAdmin(Pageable pageable) {
                return investissementRepository.findAll(pageable).map(investissementMapper::toDto);
        }

        public List<InvestissementDTO> getAllAdmin(String search) {
                if (search != null && !search.isBlank()) {
                        String like = "%" + search.toLowerCase() + "%";
                        return investissementRepository.findBySearchTerm(like).stream()
                                        .map(investissementMapper::toDto).toList();
                }
                return investissementRepository.findAll().stream()
                                .map(investissementMapper::toDto).toList();
        }

        public Page<InvestissementDTO> getAllAdmin(String search, StatutPartInvestissement statut, Pageable pageable) {
                return investissementRepository.rechercherAdmin(search, statut, pageable)
                                .map(investissementMapper::toDto);
        }

        public java.util.Map<String, Long> getStatutCounts() {
                java.util.Map<String, Long> counts = new java.util.LinkedHashMap<>();
                for (StatutPartInvestissement statut : StatutPartInvestissement.values()) {
                        counts.put(statut.name(), investissementRepository.countByStatutPartInvestissement(statut));
                }
                counts.put("TOUS", investissementRepository.count());
                return counts;
        }

        // ── ANNULER (avec motif) ──────────────────────────────────────────────────
        @Transactional
        public void annulerInvestissement(Long id, String motif) {
                Investissement inv = investissementRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new RuntimeException("Investissement non trouvé : " + id));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Impossible d'annuler un investissement déjà traité");
                }

                BigDecimal montant = inv.getMontantInvesti();
                User investisseur = inv.getInvestisseur();
                Projet projet = inv.getProjet();

                // 1. Restituer les fonds bloqués → solde disponible
                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet investisseur introuvable"));
                walletUser.debloquerFonds(montant);
                walletRepository.save(walletUser);

                // 2. NE PAS modifier le projet
                // investir() ne modifie pas partsPrises/montantCollecte.
                // C'est validerInvestissement() qui le fait.

                // 3. Transaction de remboursement
                Transaction tx = Transaction.builder()
                                .walletId(walletUser.getId())
                                .walletType(WalletType.USER)
                                .montant(montant)
                                .type(TypeTransaction.REMBOURSEMENT)
                                .statut(StatutTransaction.SUCCESS)
                                .description("Investissement refusé — " + projet.getLibelle() + " — " + motif)
                                .createdAt(LocalDateTime.now())
                                .referenceType("INVESTISSEMENT")
                                .referenceId(id)
                                .build();
                transactionRepository.save(tx);

                // 4. Notifier l'investisseur (notification + email)
                String messageNotif = "Votre investissement de " + montant.toPlainString()
                                + " FCFA dans le projet \"" + projet.getLibelle()
                                + "\" a été refusé. Motif : " + motif
                                + ". Les fonds ont été restitués dans votre portefeuille GrowzApp.";

                notificationService.notifyUser(
                                investisseur,
                                "Investissement refusé — " + projet.getLibelle(),
                                messageNotif,
                                projet.getId(),
                                projet.getSlug(),
                                motif);

                // Email avec le motif détaillé
                emailService.envoyerRefusInvestissement(
                                investisseur.getEmail(),
                                investisseur.getPrenom() + " " + investisseur.getNom(),
                                projet.getLibelle(),
                                montant.toPlainString(),
                                motif);

                // 5. Marquer annulé
                inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
                investissementRepository.save(inv);

                log.info("Investissement {} refusé (motif: {}) — {} FCFA restitués à user={}",
                                id, motif, montant, investisseur.getId());
        }

        // Surcharge sans motif pour compatibilité
        @Transactional
        public void annulerInvestissement(Long id) {
                annulerInvestissement(id, "Refusé par l'administration");
        }

        // ── Textes de consentement légal affichés à l'investisseur avant de
        // valider son choix à l'échéance de financement d'un projet — copiés
        // intégralement dans DecisionEcheanceInvestissement.consentementTexte au
        // moment de la décision, pour rester lisibles même si ces textes changent
        // plus tard. Pas un avis juridique final — à faire relire avant mise en
        // production.
        public static final String CONSENTEMENT_CONTINUER =
                        "En choisissant de continuer, je confirme vouloir maintenir mon investissement dans ce "
                        + "projet malgré le dépassement de sa date limite de financement, dans l'hypothèse où "
                        + "GrowzApp déciderait de la prolonger. Ce choix est définitif pour la période en cours : "
                        + "je ne pourrai plus demander le remboursement de cet investissement tant qu'une nouvelle "
                        + "date limite n'est pas elle-même dépassée sans objectif atteint. Si GrowzApp décide "
                        + "malgré tout de clôturer ce projet en échec, mon investissement me sera intégralement "
                        + "remboursé sans action de ma part. Aucune garantie n'est donnée que le projet atteindra "
                        + "son objectif même après prolongation.";

        public static final String CONSENTEMENT_RECUPERER =
                        "En choisissant de récupérer mon investissement, je confirme vouloir être intégralement "
                        + "remboursé du montant investi dans ce projet, qui n'a pas atteint son objectif de "
                        + "financement à sa date limite. Ce remboursement est immédiat et irrévocable. Mon contrat "
                        + "d'investissement pour ce projet est annulé et archivé à compter de cette décision — il "
                        + "reste consultable comme justificatif historique mais ne représente plus une "
                        + "participation active. Je renonce à toute part de capital, tout dividende futur ou autre "
                        + "droit lié à cet investissement dans ce projet. Toute utilisation frauduleuse de ce "
                        + "contrat annulé après remboursement constitue une infraction pouvant faire l'objet de "
                        + "poursuites conformément aux CGU et CGV de GrowzApp.";

        private static final String CONSENTEMENT_CLOTURE_ADMIN =
                        "Remboursement automatique suite à la clôture administrative du projet par l'équipe GrowzApp.";

        // Rembourse un investissement VALIDE dont le projet n'a pas atteint son
        // objectif de financement à sa date limite — point d'entrée unique
        // partagé par la clôture admin en masse (ProjetService.cloturerEnEchec)
        // et le choix individuel self-service de l'investisseur
        // (/{id}/echeance/recuperer), pour que la logique wallet + statut +
        // archivage de contrat + audit + email ne soit jamais dupliquée.
        @Transactional
        public void rembourserPourEchecFinancement(Investissement inv, String motif, boolean declencheParAdmin) {
                Long projetId = inv.getProjet().getId();
                Long investisseurId = inv.getInvestisseur().getId();

                walletService.rembourserEchecFinancement(projetId, investisseurId, inv.getMontantInvesti(), motif);

                inv.setStatutPartInvestissement(StatutPartInvestissement.REMBOURSE);
                // Un remboursement forcé par la clôture admin écrase un éventuel choix
                // "CONTINUER" fait avant que l'admin ne décide malgré tout de ne pas
                // prolonger — ce champ ne représente que le cycle courant.
                inv.setChoixEcheanceActuel(ChoixEcheance.RECUPERER);
                investissementRepository.save(inv);

                if (inv.getContrat() != null) {
                        try {
                                contratService.archiver(
                                                inv.getContrat().getId(),
                                                declencheParAdmin
                                                                ? "SYSTEM (clôture échec financement)"
                                                                : "Investisseur (choix à l'échéance)");
                        } catch (Exception e) {
                                log.error("rembourserPourEchecFinancement : échec de l'archivage du contrat {} "
                                                + "(investissement {}) : {}",
                                                inv.getContrat().getId(), inv.getId(), e.getMessage());
                        }
                }

                DecisionEcheanceInvestissement decision = DecisionEcheanceInvestissement.builder()
                                .investissementId(inv.getId())
                                .projetId(projetId)
                                .investisseurId(investisseurId)
                                .choix(ChoixEcheance.RECUPERER)
                                .dateDecision(LocalDateTime.now())
                                .dateFinProjetAuMoment(inv.getProjet().getDateFin())
                                .consentementTexte(declencheParAdmin ? CONSENTEMENT_CLOTURE_ADMIN : CONSENTEMENT_RECUPERER)
                                .declencheParAdmin(declencheParAdmin)
                                .build();
                decisionEcheanceInvestissementRepository.save(decision);

                User investisseur = inv.getInvestisseur();
                Projet projet = inv.getProjet();
                emailService.envoyerRemboursementEcheance(
                                investisseur.getEmail(),
                                investisseur.getPrenom() + " " + investisseur.getNom(),
                                projet.getLibelle(),
                                inv.getMontantInvesti().toPlainString(),
                                motif,
                                declencheParAdmin);

                log.info("rembourserPourEchecFinancement : investissement {} remboursé ({} FCFA) — déclenché par {}",
                                inv.getId(), inv.getMontantInvesti(), declencheParAdmin ? "admin" : "investisseur");
        }

        // Choix de l'investisseur de maintenir son investissement malgré la date
        // limite dépassée, dans l'espoir d'une prolongation. Aucun effet
        // financier — engagement enregistré uniquement.
        @Transactional
        public void continuerMalgreEcheance(Long investissementId, Long investisseurConnecteId) {
                Investissement inv = chargerPourChoixEcheance(investissementId, investisseurConnecteId);

                inv.setChoixEcheanceActuel(ChoixEcheance.CONTINUER);
                investissementRepository.save(inv);

                DecisionEcheanceInvestissement decision = DecisionEcheanceInvestissement.builder()
                                .investissementId(inv.getId())
                                .projetId(inv.getProjet().getId())
                                .investisseurId(investisseurConnecteId)
                                .choix(ChoixEcheance.CONTINUER)
                                .dateDecision(LocalDateTime.now())
                                .dateFinProjetAuMoment(inv.getProjet().getDateFin())
                                .consentementTexte(CONSENTEMENT_CONTINUER)
                                .declencheParAdmin(false)
                                .build();
                decisionEcheanceInvestissementRepository.save(decision);

                log.info("continuerMalgreEcheance : investissement {} — investisseur {} choisit de continuer",
                                investissementId, investisseurConnecteId);
        }

        // Choix de l'investisseur de récupérer immédiatement son argent plutôt
        // que d'attendre une éventuelle prolongation.
        @Transactional
        public void recupererAEcheance(Long investissementId, Long investisseurConnecteId) {
                Investissement inv = chargerPourChoixEcheance(investissementId, investisseurConnecteId);
                rembourserPourEchecFinancement(inv, null, false);
        }

        // Vérifications communes aux deux choix d'échéance : l'investissement
        // appartient bien à l'utilisateur connecté, est encore VALIDE, le projet
        // a dépassé sa date limite sans atteindre son objectif, et aucune
        // décision n'a encore été prise pour le cycle d'échéance en cours.
        private Investissement chargerPourChoixEcheance(Long investissementId, Long investisseurConnecteId) {
                Investissement inv = investissementRepository.findByIdWithLock(investissementId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Investissement non trouvé : " + investissementId));

                if (!inv.getInvestisseur().getId().equals(investisseurConnecteId)) {
                        throw new org.springframework.security.access.AccessDeniedException(
                                        "Cet investissement ne vous appartient pas.");
                }
                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.VALIDE) {
                        throw new IllegalStateException("Cet investissement n'est pas dans un état permettant ce choix.");
                }

                Projet projet = inv.getProjet();
                boolean dateDepassee = projet.getDateFin() != null
                                && projet.getDateFin().isBefore(java.time.LocalDate.now());
                boolean objectifNonAtteint = projet.getObjectifFinancement() != null
                                && (projet.getMontantCollecte() == null
                                                || projet.getMontantCollecte().compareTo(projet.getObjectifFinancement()) < 0);
                if (!dateDepassee || !objectifNonAtteint) {
                        throw new IllegalStateException(
                                        "La date limite de financement de ce projet n'est pas dépassée sans objectif atteint.");
                }
                if (inv.getChoixEcheanceActuel() != null) {
                        throw new IllegalStateException("Une décision a déjà été prise pour ce cycle d'échéance.");
                }

                return inv;
        }

        @Transactional
        public InvestissementDTO save(InvestissementCreateDTO dto, Long id) {
                Investissement entity = id != null
                                ? investissementRepository.findById(id)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Investissement non trouvé"))
                                : new Investissement();

                entity.setNombrePartsPris(dto.getNombrePartsPris());
                entity.setFrais(dto.getFrais() != null ? dto.getFrais() : 0.0);

                Projet projet = projetRepository.findById(dto.getProjetId())
                                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));
                entity.setProjet(projet);

                User investisseur = userRepository.findById(dto.getInvestisseurId())
                                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
                entity.setInvestisseur(investisseur);

                if (id == null) {
                        entity.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
                        entity.setDate(LocalDateTime.now());
                }

                entity = investissementRepository.save(entity);
                return investissementMapper.toDto(entity);
        }

        @Transactional
        public InvestissementDTO investir(Long projetId, int nombrePartsPris, User investisseur) {
                if (investisseur.getKycStatus() != KycStatus.VALIDE) {
                        throw new IllegalStateException(
                                        "Votre profil KYC doit être validé par un administrateur avant de pouvoir investir.");
                }

                Projet projet = projetRepository.findByIdWithLock(projetId)
                                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

                BigDecimal prixPart = projet.getPrixUnePart();
                BigDecimal montantTotal = prixPart.multiply(BigDecimal.valueOf(nombrePartsPris));

                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet utilisateur non trouvé"));

                if (walletUser.getSoldeDisponible().compareTo(montantTotal) < 0) {
                        throw new IllegalStateException("Solde insuffisant");
                }

                // Investissement financé par le wallet interne : les fonds proviennent
                // bien du soldeDisponible existant, donc bloquerFonds() (qui
                // débite disponible pour créditer bloqué) est le mécanisme correct ici.
                walletUser.bloquerFonds(montantTotal);
                walletRepository.save(walletUser);

                return finaliserCreationInvestissement(projet, nombrePartsPris, montantTotal, investisseur,
                                walletUser.getId(), SourcePaiement.WALLET_GROWZAPP);
        }

        /**
         * Investissement financé par un paiement EXTERNE déjà confirmé (Stripe,
         * FedaPay, PayDunya) — l'argent n'a jamais transité par le wallet
         * interne du porteur, il doit donc créditer directement soldeBloque,
         * sans jamais passer (même transitoirement) par soldeDisponible.
         */
        @Transactional
        public InvestissementDTO investirDepuisPaiementExterne(Long projetId, int nombrePartsPris, User investisseur,
                        SourcePaiement sourcePaiement) {
                if (investisseur.getKycStatus() != KycStatus.VALIDE) {
                        throw new IllegalStateException(
                                        "Votre profil KYC doit être validé par un administrateur avant de pouvoir investir.");
                }

                Projet projet = projetRepository.findByIdWithLock(projetId)
                                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

                BigDecimal montantTotal = projet.getPrixUnePart().multiply(BigDecimal.valueOf(nombrePartsPris));

                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet utilisateur non trouvé"));

                walletUser.crediterDirectementBloque(montantTotal);
                walletRepository.save(walletUser);

                return finaliserCreationInvestissement(projet, nombrePartsPris, montantTotal, investisseur,
                                walletUser.getId(), sourcePaiement);
        }

        private InvestissementDTO finaliserCreationInvestissement(Projet projet, int nombrePartsPris,
                        BigDecimal montantTotal, User investisseur, Long walletUserId, SourcePaiement sourcePaiement) {
                Investissement investissement = new Investissement();
                investissement.setNombrePartsPris(nombrePartsPris);
                investissement.setMontantInvesti(montantTotal);
                investissement.setInvestisseur(investisseur);
                investissement.setProjet(projet);
                investissement.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
                investissement.setDate(LocalDateTime.now());
                investissement.calculerTout();
                investissement = investissementRepository.save(investissement);

                Transaction tx = Transaction.builder()
                                .walletId(walletUserId)
                                .walletType(WalletType.USER)
                                .montant(montantTotal)
                                .type(TypeTransaction.INVESTISSEMENT)
                                .statut(StatutTransaction.EN_ATTENTE_VALIDATION)
                                .description("Investissement en attente dans le projet: " + projet.getLibelle())
                                .createdAt(LocalDateTime.now())
                                .referenceType("INVESTISSEMENT")
                                .referenceId(investissement.getId())
                                .sourcePaiement(sourcePaiement)
                                .build();
                transactionRepository.save(tx);

                notificationService.notifyAdmins(
                                "💰 Nouvel investissement en attente",
                                investisseur.getPrenom() + " " + investisseur.getNom() + " a investi " + montantTotal
                                                + " FCFA dans « " + projet.getLibelle()
                                                + " » — validation requise.",
                                "/admin/investissements");

                return investissementMapper.toDto(investissement);
        }

        @Transactional
        public Investissement validerInvestissement(Long id) throws Exception {
                Investissement inv = investissementRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Cet investissement a déjà été traité");
                }

                BigDecimal montant = inv.getMontantInvesti();
                Projet projet = inv.getProjet();
                User investisseur = inv.getInvestisseur();

                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet investisseur non trouvé"));

                Wallet walletProjet = walletRepository
                                .findByProjetIdAndWalletTypeWithLock(projet.getId(), WalletType.PROJET)
                                .orElseGet(() -> {
                                        Wallet w = Wallet.builder()
                                                        .walletType(WalletType.PROJET)
                                                        .projetId(projet.getId())
                                                        .user(null)
                                                        .soldeDisponible(BigDecimal.ZERO)
                                                        .soldeBloque(BigDecimal.ZERO)
                                                        .build();
                                        return walletRepository.save(w);
                                });

                walletUser.validerInvestissement(montant);
                // Nouveau flux : les fonds validés vont en trésorerie séquestrée du
                // projet (soldeBloque), pas directement en soldeDisponible. Ils ne
                // deviennent utilisables par le porteur qu'après un déblocage admin
                // explicite (WalletService.debloquerTresorerieProjet).
                walletProjet.crediterBloqueProjet(montant);

                Transaction tx = transactionRepository.findByReferenceTypeAndReferenceId("INVESTISSEMENT", id)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Transaction d'investissement introuvable."));

                tx.markAsSuccess();
                transactionRepository.save(tx);

                // Transaction côté wallet PROJET : jusqu'ici seule la transaction
                // côté wallet USER (ci-dessus) existait, donc l'historique du wallet
                // projet ne montrait jamais l'entrée d'argent liée aux investissements
                // (uniquement les mouvements admin ultérieurs). Corrigé pour respecter
                // la traçabilité complète attendue (qui/quand/combien/quel projet).
                Transaction txProjet = Transaction.builder()
                                .walletId(walletProjet.getId())
                                .walletType(WalletType.PROJET)
                                .montant(montant)
                                .type(TypeTransaction.CREDIT_PROJET)
                                .statut(StatutTransaction.SUCCESS)
                                .description("Investissement validé — " + projet.getLibelle())
                                .referenceType("INVESTISSEMENT")
                                .referenceId(id)
                                .createdAt(LocalDateTime.now())
                                .sourcePaiement(tx.getSourcePaiement())
                                .build();
                transactionRepository.save(txProjet);

                projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());
                projet.setMontantCollecte(projet.getMontantCollecte().add(montant));

                // Objectif de financement atteint → le projet quitte automatiquement le
                // catalogue public (qui n'affiche que le statut VALIDE) en passant FINANCE.
                if (projet.getStatutProjet() == StatutProjet.VALIDE
                        && projet.getObjectifFinancement() != null
                        && projet.getMontantCollecte().compareTo(projet.getObjectifFinancement()) >= 0) {
                    projet.setStatutProjet(StatutProjet.FINANCE);
                }

                projetRepository.save(projet);

                projetValorisationService.enregistrerSnapshot(
                                projet,
                                TypeEvenementValorisation.INVESTISSEMENT,
                                montant);

                inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);
                walletRepository.save(walletUser);
                walletRepository.save(walletProjet);

                Investissement savedInv = investissementRepository.save(inv);

                userService.attribuerRoleSiAbsent(investisseur.getId(), "INVESTISSEUR");

                notificationService.notifyUser(
                                projet.getPorteur(),
                                "Nouvel investissement !",
                                "Félicitations ! Un montant de " + montant + " FCFA a été investi dans votre projet "
                                                + projet.getLibelle(),
                                projet.getId(),
                                projet.getSlug());

                notificationService.notifyUser(
                                investisseur,
                                "✅ Investissement validé !",
                                "Votre investissement de " + montant + " FCFA dans « " + projet.getLibelle()
                                                + " » a été validé. Votre contrat est disponible.",
                                projet.getId(),
                                projet.getSlug());

                notificationService.notifyExistingInvestors(projet, montant, investisseur);

                return savedInv;
        }

        @Transactional
        public Investissement refuserInvestissement(Long id) {
                Investissement inv = investissementRepository.findByIdWithLock(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Impossible de refuser un investissement déjà traité");
                }

                BigDecimal montant = inv.getMontantInvesti();
                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(inv.getInvestisseur().getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));

                walletUser.debloquerFonds(montant);
                walletRepository.save(walletUser);

                Transaction tx = transactionRepository.findByReferenceTypeAndReferenceId("INVESTISSEMENT", id)
                                .orElseThrow(() -> new IllegalStateException("Transaction introuvable"));
                tx.markAsFailed();
                transactionRepository.save(tx);

                // NE PAS modifier le projet
                inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
                return investissementRepository.save(inv);
        }

        public InvestissementDTO getInvestissementWithDividendes(Long id) {
                return investissementMapper.toDto(investissementRepository.findById(id).orElseThrow());
        }

        public Investissement findEntityById(Long id) {
                return investissementRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));
        }

        public InvestissementDTO getInvestissementDtoById(Long id) {
                return investissementMapper.toDto(findEntityById(id));
        }

        public List<InvestissementDTO> getAllInvestissements() {
                return investissementRepository.findAll().stream()
                                .map(investissementMapper::toDto).toList();
        }

        public InvestissementDTO getInvestissementWithAllDividendes(Long projetId) {
                return investissementMapper.toDto(investissementRepository.findById(projetId)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé")));
        }

        public List<InvestissementDTO> getByInvestisseurId(Long investisseurId) {
                return investissementRepository.findByInvestisseurId(investisseurId).stream()
                                .map(investissementMapper::toDto).toList();
        }

        public List<InvestissementDTO> getInvestissementsByProjetId(Long projetId) {
                return investissementRepository.findByProjetId(projetId).stream()
                                .map(investissementMapper::toDto).toList();
        }

        public List<Map<String, Object>> getInvestmentEvolution() {
                List<Investissement> investissements = investissementRepository.findAll().stream()
                                .filter(inv -> inv.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                                .sorted(java.util.Comparator.comparing(Investissement::getDate))
                                .toList();

                java.util.Map<String, BigDecimal> statsMap = new java.util.LinkedHashMap<>();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");
                BigDecimal cumulProgressif = BigDecimal.ZERO;

                for (Investissement inv : investissements) {
                        String jour = inv.getDate().format(formatter);
                        BigDecimal m = inv.getMontantInvesti() != null ? inv.getMontantInvesti() : BigDecimal.ZERO;
                        cumulProgressif = cumulProgressif.add(m);
                        statsMap.put(jour, cumulProgressif);
                }

                return statsMap.entrySet().stream().map(entry -> {
                        Map<String, Object> dp = new java.util.HashMap<>();
                        dp.put("date", entry.getKey());
                        dp.put("montant", entry.getValue());
                        return dp;
                }).toList();
        }
}