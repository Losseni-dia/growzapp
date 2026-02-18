// src/main/java/growzapp/backend/service/InvestissementService.java
package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.investisementDTO.InvestissementCreateDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.Transaction;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.KycStatus;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.model.enumeration.StatutTransaction;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.notification.service.NotificationService;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.TransactionRepository;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.WalletRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class InvestissementService {

        private final InvestissementRepository repository;
        private final InvestissementRepository investissementRepository;
        private final DtoConverter converter;
        private final ProjetRepository projetRepository;
        private final UserRepository userRepository;
        private final WalletRepository walletRepository;
        private final TransactionRepository transactionRepository;
        private final NotificationService notificationService;

        public List<InvestissementDTO> getAll() {
                return investissementRepository.findAll().stream()
                                .map(converter::toInvestissementDto)
                                .toList();
        }

        public Page<InvestissementDTO> getAllAdmin(Pageable pageable) {
                Page<Investissement> page = investissementRepository.findAll(pageable);
                return page.map(converter::toInvestissementDto);
        }

        // Tu peux aussi ajouter une version avec recherche si tu veux
        public List<InvestissementDTO> getAllAdmin(String search) {
                if (search != null && !search.isBlank()) {
                        String like = "%" + search.toLowerCase() + "%";
                        return investissementRepository.findBySearchTerm(like)
                                        .stream()
                                        .map(converter::toInvestissementDto)
                                        .toList();
                }
                return investissementRepository.findAll()
                                .stream()
                                .map(converter::toInvestissementDto)
                                .toList();
        }

        @Transactional
        public void annulerInvestissement(Long id) {
                Investissement inv = repository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Investissement non trouvé : " + id));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Impossible d’annuler un investissement déjà validé ou annulé");
                }

                inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
        }

        // Dans InvestissementService.java → CHANGE void → InvestissementDTO
        @Transactional
        public InvestissementDTO save(InvestissementCreateDTO dto, Long id) {
                Investissement entity = id != null
                                ? investissementRepository.findById(id)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Investissement non trouvé"))
                                : new Investissement();

                // Ton mapping existant
                entity.setNombrePartsPris(dto.getNombrePartsPris());
                entity.setFrais(dto.getFrais() != null ? dto.getFrais() : 0.0);

                Projet projet = projetRepository.findById(dto.getProjetId())
                                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));
                entity.setProjet(projet);

                User investisseur = userRepository.findById(dto.getInvestisseurId())
                                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
                entity.setInvestisseur(investisseur);

                // Statut par défaut en création
                if (id == null) {
                        entity.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
                        entity.setDate(LocalDateTime.now());
                }

                entity = investissementRepository.save(entity);

                // ← RETOURNE LE DTO COMPLET
                return converter.toInvestissementDto(entity);
        }

        /// InvestissementService.java → VERSION FINALE 2025 – TOUT EST CORRIGÉ

        // =====================================================================
        // MÉTHODE 1 : investir (Crée la transaction en statut EN_ATTENTE)
        // =====================================================================
        @Transactional
        public InvestissementDTO investir(Long projetId, int nombrePartsPris, User investisseur) {

                // === AJOUT DE LA CONTRAINTE KYC ===
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

                // 1. BLOQUE LES FONDS DE L'UTILISATEUR
                walletUser.bloquerFonds(montantTotal);
                walletRepository.save(walletUser);

                // 2. CRÉATION DE L'INVESTISSEMENT
                Investissement investissement = new Investissement();
                investissement.setNombrePartsPris(nombrePartsPris);
                investissement.setMontantInvesti(montantTotal);
                investissement.setInvestisseur(investisseur); // <--- CORRECTION D'AFFECTATION
                investissement.setProjet(projet);
                investissement.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
                investissement.setDate(LocalDateTime.now());
                investissement.calculerTout();

                investissement = repository.save(investissement); // Sauvegarde pour obtenir l'ID

                // 3. CRÉATION DE LA TRANSACTION INITIALE (EN ATTENTE)
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

                return converter.toInvestissementDto(investissement);
        }

        // =====================================================================
        // MÉTHODE 2 : validerInvestissement (Met la transaction à SUCCESS)
        // =====================================================================
        @Transactional
        public Investissement validerInvestissement(Long id) throws Exception {
                Investissement inv = repository.findByIdWithLock(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Cet investissement a déjà été traité");
                }

                BigDecimal montant = inv.getMontantInvesti();
                Projet projet = inv.getProjet();
                User investisseur = inv.getInvestisseur();

                // 1. TRANSFERT DE FONDS (Débloque chez l'utilisateur -> Crédite le projet)
                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet investisseur non trouvé"));

                Wallet walletProjet = walletRepository
                                .findByProjetIdAndWalletTypeWithLock(projet.getId(), WalletType.PROJET)
                                .orElseGet(() -> {
                                        // ... (votre logique de création du wallet projet) ...
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

                // 2. MISE À JOUR DE LA TRANSACTION ASSOCIÉE (Statut: SUCCESS)
                Transaction tx = transactionRepository.findByReferenceTypeAndReferenceId("INVESTISSEMENT", id)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Transaction d'investissement associée introuvable."));

                tx.markAsSuccess();
                transactionRepository.save(tx);

                // 3. MISE À JOUR DU PROJET
                projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());
                projet.setMontantCollecte(projet.getMontantCollecte().add(montant));
                projetRepository.save(projet);

                // 4. MISE À JOUR DE L'INVESTISSEMENT
                inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);
                walletRepository.save(walletUser);
                walletRepository.save(walletProjet);

                // ON SAUVEGARDE MAIS ON NE FAIT PAS ENCORE LE RETURN
                Investissement savedInv = repository.save(inv);

                // 5. NOTIFICATIONS (Maintenant elles sont bien exécutées !)
                notificationService.notifyProjectOwner(
                                projet.getPorteur(),
                                "Nouvel investissement !",
                                "Félicitations ! Un montant de " + montant + " FCFA a été investi dans votre projet "
                                                + projet.getLibelle());

                // B. Notifier les autres investisseurs du projet
                notificationService.notifyExistingInvestors(
                                projet,
                                montant,
                                investisseur);

                // ENFIN, ON SORT DE LA MÉTHODE
                return savedInv;
        }

        // =====================================================================
        // MÉTHODE 3 : refuserInvestissement (Met la transaction à FAILED)
        // =====================================================================
        @Transactional
        public Investissement refuserInvestissement(Long id) {
                Investissement inv = repository.findByIdWithLock(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

                if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                        throw new IllegalStateException("Impossible de refuser un investissement déjà traité");
                }

                BigDecimal montant = inv.getMontantInvesti();
                Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(inv.getInvestisseur().getId())
                                .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));

                // 1. REMBOURSEMENT/DÉBLOCAGE : Libère les fonds bloqués et les remet en
                // Disponible
                walletUser.debloquerFonds(montant);
                walletRepository.save(walletUser);

                // 2. MISE À JOUR DE LA TRANSACTION ASSOCIÉE (Statut: FAILED)
                Transaction tx = transactionRepository.findByReferenceTypeAndReferenceId("INVESTISSEMENT", id)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Transaction d'investissement associée introuvable."));

                tx.markAsFailed();
                transactionRepository.save(tx);

                // 3. MISE À JOUR DU PROJET (Remboursement logique de la collecte affichée)
                Projet projet = inv.getProjet();
                projet.setPartsPrises(projet.getPartsPrises() - inv.getNombrePartsPris());
                if (projet.getMontantCollecte() != null) {
                        projet.setMontantCollecte(projet.getMontantCollecte().subtract(montant));
                }
                projetRepository.save(projet);

                // 4. MISE À JOUR DE L'INVESTISSEMENT
                inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
                return repository.save(inv);
        }

        public InvestissementDTO getInvestissementWithDividendes(Long id) {
                Investissement inv = investissementRepository.findById(id).orElseThrow();
                return converter.toInvestissementDto(inv); // DTO enrichi
        }

        // Méthode qui retourne l'entité
        public Investissement findEntityById(Long id) {
                return investissementRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));
        }

        // Méthode qui retourne le DTO
        public InvestissementDTO getInvestissementDtoById(Long id) {
                Investissement inv = findEntityById(id);
                return converter.toInvestissementDto(inv);
        }

        public List<InvestissementDTO> getAllInvestissements() {
                return investissementRepository.findAll().stream()
                                .map(converter::toInvestissementDto)
                                .toList();
        }

        public InvestissementDTO getInvestissementWithAllDividendes(Long projetId) {
                Investissement investissement = investissementRepository.findById(projetId)
                                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

                return converter.toInvestissementDto(investissement);
        }

        public List<InvestissementDTO> getByInvestisseurId(Long investisseurId) {
                return investissementRepository.findByInvestisseurId(investisseurId).stream()
                                .map(converter::toInvestissementDto)
                                .toList();
        }

        public List<InvestissementDTO> getInvestissementsByProjetId(Long projetId) {
                return investissementRepository.findByProjetId(projetId)
                                .stream()
                                .map(converter::toInvestissementDto)
                                .toList();
        }

        // === AJOUT POUR LES STATISTIQUES GLOBALES ===

        public List<Map<String, Object>> getInvestmentEvolution() {
                // 1. Récupérer et trier par date chronologique
                List<Investissement> investissements = investissementRepository.findAll().stream()
                                .filter(inv -> inv.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                                .sorted(java.util.Comparator.comparing(Investissement::getDate))
                                .toList();

                // 2. Utilisation de LinkedHashMap pour conserver l'ordre des jours
                java.util.Map<String, BigDecimal> statsMap = new java.util.LinkedHashMap<>();
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM");

                BigDecimal cumulProgressif = BigDecimal.ZERO;

                for (Investissement inv : investissements) {
                        String jour = inv.getDate().format(formatter);
                        BigDecimal montantDeLInvestissement = inv.getMontantInvesti() != null ? inv.getMontantInvesti()
                                        : BigDecimal.ZERO;

                        // On ajoute le montant de cet investissement au total cumulé
                        cumulProgressif = cumulProgressif.add(montantDeLInvestissement);

                        // On met à jour (ou écrase) la valeur du jour avec le nouveau cumul
                        // Si plusieurs investissements le même jour, le dernier passera avec le cumul
                        // le plus récent
                        statsMap.put(jour, cumulProgressif);
                }

                // 3. Conversion pour le Frontend
                return statsMap.entrySet().stream()
                                .map(entry -> {
                                        Map<String, Object> dataPoint = new java.util.HashMap<>();
                                        dataPoint.put("date", entry.getKey());
                                        dataPoint.put("montant", entry.getValue());
                                        return dataPoint;
                                })
                                .toList();
        }

}
