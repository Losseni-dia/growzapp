package growzapp.backend.module.dividende.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.dividende.dto.DividendeHistoriqueAdminDTO;
import growzapp.backend.module.dividende.enums.StatutDividende;
import growzapp.backend.module.dividende.mapper.DividendeMapper;
import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.facture.service.FactureService;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.paiement.enums.MoyenPaiement;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DividendeService {

    private final DividendeRepository dividendeRepository;
    private final InvestissementRepository investissementRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FactureService factureService;
    private final EmailService emailService;
    private final DividendeMapper dividendeMapper;

    private static final Logger log = LoggerFactory.getLogger(DividendeService.class);

    @Transactional
    public void payerDividendesProjetProrata(
            Long projetId,
            BigDecimal montantTotal,
            String motif,
            String periode) {

        log.info("Début de la distribution des dividendes - projetId={}, montantTotal={}, motif={}, periode={}",
                projetId, montantTotal, motif, periode);

        if (montantTotal == null || montantTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Montant invalide lors de la distribution pour le projet {} : {}", projetId, montantTotal);
            throw new IllegalStateException("Le montant total doit être strictement positif");
        }

        List<Investissement> investissements = investissementRepository.findByProjetId(projetId)
                .stream()
                .filter(i -> i.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                .toList();

        if (investissements.isEmpty()) {
            log.warn("Aucun investissement avec statut VALIDE pour le projet {}", projetId);
            throw new IllegalStateException(
                    "Aucun investissement valide trouvé pour ce projet. Impossible de distribuer des dividendes.");
        }

        BigDecimal totalParts = investissements.stream()
                .map(inv -> BigDecimal.valueOf(inv.getNombrePartsPris()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalParts.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Aucune part investie (totalParts = 0) pour le projet {}", projetId);
            throw new IllegalStateException("Aucune part investie dans ce projet");
        }

        Wallet walletProjet = walletRepository
                .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> {
                    log.error("Wallet projet introuvable pour le projet {}", projetId);
                    return new IllegalStateException("Wallet du projet introuvable");
                });

        if (walletProjet.getSoldeDisponible().compareTo(montantTotal) < 0) {
            log.warn("Solde insuffisant pour le projet {} : disponible = {}, requis = {}",
                    projetId, walletProjet.getSoldeDisponible(), montantTotal);
            throw new IllegalStateException("Solde insuffisant dans le wallet du projet");
        }

        for (Investissement inv : investissements) {
            BigDecimal parts = BigDecimal.valueOf(inv.getNombrePartsPris());

            BigDecimal montantAPayer = parts
                    .divide(totalParts, 10, RoundingMode.HALF_UP)
                    .multiply(montantTotal);

            BigDecimal montantParPart = montantAPayer.divide(parts, 10, RoundingMode.HALF_UP);

            Dividende dividende = new Dividende();
            dividende.setInvestissement(inv);
            dividende.setMontantParPart(montantParPart);
            dividende.setStatutDividende(StatutDividende.PAYE);
            dividende.setMoyenPaiement(MoyenPaiement.WALLET);
            dividende.setDatePaiement(LocalDateTime.now());

            dividende = dividendeRepository.save(dividende);

            final Long dividendeId = dividende.getId();

            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager
                        .registerSynchronization(new TransactionSynchronization() {
                            @Override
                            public void afterCommit() {
                                log.info("Transaction commited. Lancement async facture pour dividende {}",
                                        dividendeId);
                                factureService.genererFactureEtEnvoyerEmailAsync(dividendeId);
                            }
                        });
            } else {
                factureService.genererFactureEtEnvoyerEmailAsync(dividendeId);
            }

            Wallet walletInvestisseur = inv.getInvestisseur().getWallet();
            if (walletInvestisseur == null) {
                log.error("Wallet manquant pour l'investisseur ID {}", inv.getInvestisseur().getId());
                throw new IllegalStateException("Wallet manquant pour l'investisseur");
            }

            walletInvestisseur.crediterDisponible(montantAPayer);
            walletRepository.save(walletInvestisseur);

            Transaction txCredit = Transaction.builder()
                    .walletId(walletInvestisseur.getId())
                    .walletType(WalletType.USER)
                    .montant(montantAPayer)
                    .type(TypeTransaction.DIVIDENDE)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Dividende " + periode + " – " + motif)
                    .referenceType("DIVIDENDE")
                    .referenceId(dividende.getId())
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(txCredit);
        }

        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(montantTotal));
        walletRepository.save(walletProjet);

        Transaction txDebit = Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(montantTotal)
                .type(TypeTransaction.DIVIDENDE_SORTANT)
                .statut(StatutTransaction.SUCCESS)
                .description("Paiement dividende global " + periode + " – " + motif)
                .referenceType("PROJET")
                .referenceId(projetId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(txDebit);

        log.info("Distribution des dividendes terminée avec succès - projetId={}, montant distribué={}",
                projetId, montantTotal);
    }

    @Transactional(readOnly = true)
    public List<DividendeDTO> getAll() {
        return dividendeRepository.findAll().stream()
                .map(dividendeMapper::toDto)
                .toList();
    }

    public Page<DividendeDTO> getAllAdmin(Pageable pageable) {
        return dividendeRepository.findAll(pageable)
                .map(dividendeMapper::toDto);
    }

    public DividendeDTO getById(Long id) {
        return dividendeRepository.findById(id)
                .map(dividendeMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Dividende non trouvé"));
    }

    @Transactional
    public DividendeDTO save(DividendeDTO dto) {
        if (dto.investissementId() == null) {
            throw new IllegalArgumentException("Investissement obligatoire");
        }
        Investissement inv = investissementRepository.findById(dto.investissementId())
                .orElseThrow(() -> new RuntimeException("Investissement introuvable"));

        Dividende dividende = dividendeMapper.toEntity(dto);
        dividende.setInvestissement(inv);
        dividende = dividendeRepository.save(dividende);

        return dividendeMapper.toDto(dividende);
    }

    @Transactional
    public void deleteById(Long id) {
        dividendeRepository.deleteById(id);
    }

    public List<DividendeDTO> getByInvestisseurId(Long investisseurId) {
        return dividendeRepository.findByInvestissement_Investisseur_Id(investisseurId)
                .stream()
                .map(dividendeMapper::toDto)
                .toList();
    }

    public double getTotalPercuPayeByInvestisseur(Long investisseurId) {
        return dividendeRepository.findByInvestissement_Investisseur_Id(investisseurId)
                .stream()
                .filter(d -> d.getStatutDividende() == StatutDividende.PAYE)
                .mapToDouble(Dividende::getMontantTotal)
                .sum();
    }

    public Dividende findEntityById(Long id) {
        return dividendeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Dividende non trouvé"));
    }

    public List<DividendeDTO> getDividendesByProjetId(Long projetId) {
        return dividendeRepository.findByInvestissement_Projet_Id(projetId)
                .stream()
                .map(dividendeMapper::toDto)
                .toList();
    }

    public List<DividendeHistoriqueAdminDTO> getHistoriqueDividendesAvecDetails(Long projetId) {
        return dividendeRepository.findByInvestissement_Projet_Id(projetId)
                .stream()
                .map(d -> {
                    Facture facture = d.getFacture();
                    String factureUrl = null;
                    if (facture != null) {
                        Hibernate.initialize(facture);
                        factureUrl = facture.getFichierUrl();
                    }

                    return new DividendeHistoriqueAdminDTO(
                            d.getId(),
                            d.getMontantTotal(),
                            d.getDatePaiement() != null ? d.getDatePaiement().toString() : null,
                            "Dividende prorata",
                            d.getInvestissement().getInvestisseur().getPrenom() + " " +
                                    d.getInvestissement().getInvestisseur().getNom(),
                            factureUrl);
                })
                .sorted((a, b) -> b.datePaiement() != null && a.datePaiement() != null
                        ? b.datePaiement().compareTo(a.datePaiement())
                        : b.datePaiement() != null ? -1 : 1)
                .toList();
    }
}
