package growzapp.backend.module.investissement.service;

import growzapp.backend.module.investissement.dto.InvestissementCreateDTO;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.mapper.InvestissementMapper;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.investissement.repository.InvestissementRepository;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.notification.service.NotificationService;
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

    private final InvestissementRepository investissementRepository;
    private final InvestissementMapper investissementMapper;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public List<InvestissementDTO> getAll() {
        return investissementRepository.findAll().stream()
                .map(investissementMapper::toDto)
                .toList();
    }

    public Page<InvestissementDTO> getAllAdmin(Pageable pageable) {
        Page<Investissement> page = investissementRepository.findAll(pageable);
        return page.map(investissementMapper::toDto);
    }

    public List<InvestissementDTO> getAllAdmin(String search) {
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            return investissementRepository.findBySearchTerm(like)
                    .stream()
                    .map(investissementMapper::toDto)
                    .toList();
        }
        return investissementRepository.findAll()
                .stream()
                .map(investissementMapper::toDto)
                .toList();
    }

    @Transactional
    public void annulerInvestissement(Long id) {
        Investissement inv = investissementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Investissement non trouvé : " + id));

        if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
            throw new IllegalStateException("Impossible d'annuler un investissement déjà validé ou annulé");
        }

        inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
    }

    @Transactional
    public InvestissementDTO save(InvestissementCreateDTO dto, Long id) {
        Investissement entity = id != null
                ? investissementRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"))
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
                        "Transaction d'investissement associée introuvable."));

        tx.markAsSuccess();
        transactionRepository.save(tx);

        projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());
        projet.setMontantCollecte(projet.getMontantCollecte().add(montant));
        projetRepository.save(projet);

        inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);
        walletRepository.save(walletUser);
        walletRepository.save(walletProjet);

        Investissement savedInv = investissementRepository.save(inv);

        notificationService.notifyProjectOwner(
                projet.getPorteur(),
                "Nouvel investissement !",
                "Félicitations ! Un montant de " + montant + " FCFA a été investi dans votre projet "
                        + projet.getLibelle(),
                projet.getId()
        );

        notificationService.notifyExistingInvestors(
                projet,
                montant,
                investisseur);

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
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction d'investissement associée introuvable."));

        tx.markAsFailed();
        transactionRepository.save(tx);

        Projet projet = inv.getProjet();
        projet.setPartsPrises(projet.getPartsPrises() - inv.getNombrePartsPris());
        if (projet.getMontantCollecte() != null) {
            projet.setMontantCollecte(projet.getMontantCollecte().subtract(montant));
        }
        projetRepository.save(projet);

        inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);
        return investissementRepository.save(inv);
    }

    public InvestissementDTO getInvestissementWithDividendes(Long id) {
        Investissement inv = investissementRepository.findById(id).orElseThrow();
        return investissementMapper.toDto(inv);
    }

    public Investissement findEntityById(Long id) {
        return investissementRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));
    }

    public InvestissementDTO getInvestissementDtoById(Long id) {
        Investissement inv = findEntityById(id);
        return investissementMapper.toDto(inv);
    }

    public List<InvestissementDTO> getAllInvestissements() {
        return investissementRepository.findAll().stream()
                .map(investissementMapper::toDto)
                .toList();
    }

    public InvestissementDTO getInvestissementWithAllDividendes(Long projetId) {
        Investissement investissement = investissementRepository.findById(projetId)
                .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));
        return investissementMapper.toDto(investissement);
    }

    public List<InvestissementDTO> getByInvestisseurId(Long investisseurId) {
        return investissementRepository.findByInvestisseurId(investisseurId).stream()
                .map(investissementMapper::toDto)
                .toList();
    }

    public List<InvestissementDTO> getInvestissementsByProjetId(Long projetId) {
        return investissementRepository.findByProjetId(projetId)
                .stream()
                .map(investissementMapper::toDto)
                .toList();
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
            BigDecimal montantDeLInvestissement = inv.getMontantInvesti() != null ? inv.getMontantInvesti()
                    : BigDecimal.ZERO;

            cumulProgressif = cumulProgressif.add(montantDeLInvestissement);
            statsMap.put(jour, cumulProgressif);
        }

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
