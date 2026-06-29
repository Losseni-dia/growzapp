package growzapp.backend.module.investissement.service;

import growzapp.backend.module.investissement.dto.InvestissementCreateDTO;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.mapper.InvestissementMapper;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
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

                walletUser.bloquerFonds(montantTotal);
                walletRepository.save(walletUser);

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
                                .walletId(walletUser.getId())
                                .walletType(WalletType.USER)
                                .montant(montantTotal)
                                .type(TypeTransaction.INVESTISSEMENT)
                                .statut(StatutTransaction.EN_ATTENTE_VALIDATION)
                                .description("Investissement en attente dans le projet: " + projet.getLibelle())
                                .createdAt(LocalDateTime.now())
                                .referenceType("INVESTISSEMENT")
                                .referenceId(investissement.getId())
                                .build();
                transactionRepository.save(tx);

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
                                                        .soldeRetirable(BigDecimal.ZERO)
                                                        .build();
                                        return walletRepository.save(w);
                                });

                walletUser.validerInvestissement(montant);
                walletProjet.crediterDisponible(montant);

                Transaction tx = transactionRepository.findByReferenceTypeAndReferenceId("INVESTISSEMENT", id)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Transaction d'investissement introuvable."));

                tx.markAsSuccess();
                transactionRepository.save(tx);

                projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());
                projet.setMontantCollecte(projet.getMontantCollecte().add(montant));
                projetRepository.save(projet);

                inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);
                walletRepository.save(walletUser);
                walletRepository.save(walletProjet);

                Investissement savedInv = investissementRepository.save(inv);

                notificationService.notifyUser(
                                projet.getPorteur(),
                                "Nouvel investissement !",
                                "Félicitations ! Un montant de " + montant + " FCFA a été investi dans votre projet "
                                                + projet.getLibelle(),
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