// src/main/java/growzapp/backend/service/DividendeService.java
// VERSION ULTIME 2025 – DIVIDENDES PRORATA + HISTORIQUE + FACTURE + EMAIL

package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.*;
import growzapp.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DividendeService {

    private final DividendeRepository dividendeRepository;
    private final InvestissementRepository investissementRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final FactureService factureService;
    private final EmailService emailService;
    private final DtoConverter converter;

    // PAIEMENT DIVIDENDE PRORATA – LA FONCTION STAR
    @Transactional
    public void payerDividendesProjetProrata(
            Long projetId,
            Double montantTotal,
            String motif,
            String periode) {

        // 1. Récupérer les investissements validés
        List<Investissement> investissements = investissementRepository
                .findByProjetId(projetId)
                .stream()
                .filter(i -> i.getStatutPartInvestissement() == StatutPartInvestissement.VALIDE)
                .toList();

        if (investissements.isEmpty()) {
            throw new IllegalStateException("Aucun investissement valide pour ce projet");
        }

        // 2. Calcul total des parts
        double totalParts = investissements.stream()
                .mapToDouble(Investissement::getNombrePartsPris)
                .sum();

        if (totalParts <= 0) {
            throw new IllegalStateException("Aucune part investie");
        }

        // 3. Vérifier le solde du wallet projet
        Wallet walletProjet = walletRepository
                .findByProjetIdAndWalletType(projetId, WalletType.PROJET)
                .orElseThrow(() -> new IllegalStateException("Wallet projet introuvable"));

        if (walletProjet.getSoldeDisponible().doubleValue() < montantTotal) {
            throw new IllegalStateException("Solde insuffisant dans le wallet projet");
        }

        // 4. Distribuer au prorata + créer dividende + facture + email
        for (Investissement inv : investissements) {
            double parts = inv.getNombrePartsPris();
            double montantAPayer = (parts / totalParts) * montantTotal;

            // Créer le dividende
            Dividende dividende = new Dividende();
            dividende.setInvestissement(inv);
            dividende.setMontantParPart(montantAPayer / parts);
            dividende.setStatutDividende(StatutDividende.PAYE);
            dividende.setMoyenPaiement(MoyenPaiement.WALLET);
            dividende.setDatePaiement(LocalDateTime.now());
            dividende = dividendeRepository.save(dividende);

            // Créditer le wallet investisseur
            Wallet walletInvestisseur = walletRepository
                    .findByUserIdWithPessimisticLock(inv.getInvestisseur().getWallet().getId())
                    .orElseThrow(() -> new IllegalStateException("Wallet investisseur non trouvé"));

            walletInvestisseur.crediterDisponible(BigDecimal.valueOf(montantAPayer));
            walletRepository.save(walletInvestisseur);

            // Générer facture + envoyer email
            try {
                factureService.genererEtSauvegarderFacture(dividende.getId());
            } catch (Exception e) {
                System.err.println(
                        "Erreur génération facture pour dividende " + dividende.getId() + " : " + e.getMessage());
                // On ne bloque pas le paiement si la facture échoue
            }

            // Transaction crédit investisseur
            Transaction txCredit = Transaction.builder()
                    .walletId(walletInvestisseur.getId())
                    .walletType(WalletType.USER)
                    .montant(BigDecimal.valueOf(montantAPayer))
                    .type(TypeTransaction.DIVIDENDE)
                    .statut(StatutTransaction.SUCCESS)
                    .description("Dividende " + periode + " – " + motif)
                    .referenceType("DIVIDENDE") // NOUVEAU CHAMP
                    .referenceId(dividende.getId()) // ID du dividende
                    .referenceId(projetId)// garde aussi projetId pour compatibilité
                    .createdAt(LocalDateTime.now())
                    .build();
            transactionRepository.save(txCredit);
        }

        // 5. Débiter le wallet projet
        walletProjet.setSoldeDisponible(walletProjet.getSoldeDisponible().subtract(BigDecimal.valueOf(montantTotal)));
        walletRepository.save(walletProjet);

        // Transaction débit global
        Transaction txDebit = Transaction.builder()
                .walletId(walletProjet.getId())
                .walletType(WalletType.PROJET)
                .montant(BigDecimal.valueOf(montantTotal))
                .type(TypeTransaction.DIVIDENDE_SORTANT)
                .statut(StatutTransaction.SUCCESS)
                .description("Paiement dividende global " + periode + " – " + motif)
                .referenceType("PROJET")
                .referenceId(projetId)
                .createdAt(LocalDateTime.now())
                .build();
        transactionRepository.save(txDebit);
    }

    // AUTRES MÉTHODES (inchangées – tu les gardes telles quelles)
    public List<DividendeDTO> getAll() {
        return dividendeRepository.findAll().stream()
                .map(converter::toDividendeDto)
                .toList();
    }

    public Page<DividendeDTO> getAllAdmin(Pageable pageable) {
        return dividendeRepository.findAll(pageable)
                .map(converter::toDividendeDto);
    }

    public DividendeDTO getById(Long id) {
        return dividendeRepository.findById(id)
                .map(converter::toDividendeDto)
                .orElseThrow(() -> new RuntimeException("Dividende non trouvé"));
    }

    @Transactional
    public DividendeDTO save(DividendeDTO dto) {
        if (dto.investissementId() == null) {
            throw new IllegalArgumentException("Investissement obligatoire");
        }
        Investissement inv = investissementRepository.findById(dto.investissementId())
                .orElseThrow(() -> new RuntimeException("Investissement introuvable"));

        Dividende dividende = converter.toDividendeEntity(dto);
        dividende.setInvestissement(inv);
        dividende = dividendeRepository.save(dividende);

        return converter.toDividendeDto(dividende);
    }

    @Transactional
    public void deleteById(Long id) {
        dividendeRepository.deleteById(id);
    }

    public List<DividendeDTO> getByInvestisseurId(Long investisseurId) {
        return dividendeRepository.findByInvestissement_Investisseur_Id(investisseurId)
                .stream()
                .map(converter::toDividendeDto)
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
                .map(converter::toDividendeDto)
                .toList();
    }
}