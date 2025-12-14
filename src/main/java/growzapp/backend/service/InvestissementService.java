// src/main/java/growzapp/backend/service/InvestissementService.java
package growzapp.backend.service;



import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.investisementDTO.InvestissementCreateDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
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
                        .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"))
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

    @Transactional
    public InvestissementDTO investir(Long projetId, int nombrePartsPris, User investisseur) {
            // ... validation nombre de parts ...

            Projet projet = projetRepository.findByIdWithLock(projetId)
                            .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

            // ... validation statut projet ...

            // CORRECTION : prixUnePart est déjà un BigDecimal, pas besoin de valueOf
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
            // CORRECTION : montantInvesti est maintenant un BigDecimal dans l'entité
            investissement.setMontantInvesti(montantTotal);
            investissement.setInvestisseur(investisseur);
            investissement.setProjet(projet);
            investissement.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
            investissement.setDate(LocalDateTime.now());
            investissement.calculerTout();

            investissement = repository.save(investissement);
            return converter.toInvestissementDto(investissement);
    }




    @Transactional
    public Investissement validerInvestissement(Long id) throws Exception {
            Investissement inv = repository.findByIdWithLock(id)
                            .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

            if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                    throw new IllegalStateException("Cet investissement a déjà été traité");
            }

            Projet projet = inv.getProjet();
            User investisseur = inv.getInvestisseur();

            // CORRECTION 1 : inv.getMontantInvesti() est déjà un BigDecimal.
            // Ne pas utiliser BigDecimal.valueOf() dessus.
            BigDecimal montant = inv.getMontantInvesti();

            // WALLET UTILISATEUR
            Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(investisseur.getId())
                            .orElseThrow(() -> new IllegalStateException("Wallet investisseur non trouvé"));

            // WALLET PROJET
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

            // TRANSFERT D'ARGENT
            walletUser.validerInvestissement(montant);
            walletProjet.crediterDisponible(montant);

            // MISE À JOUR DU PROJET
            projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());

            // CORRECTION 2 : L'opérateur + ne fonctionne pas. Utilisez .add()
            // projet.getMontantCollecte() + inv.getMontantInvesti() devient :
            projet.setMontantCollecte(projet.getMontantCollecte().add(montant));

            projetRepository.save(projet);

            inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);

            walletRepository.save(walletUser);
            walletRepository.save(walletProjet);
            return repository.save(inv);
    }



    @Transactional
    public Investissement refuserInvestissement(Long id) {
            Investissement inv = repository.findByIdWithLock(id)
                            .orElseThrow(() -> new EntityNotFoundException("Investissement non trouvé"));

            if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
                    throw new IllegalStateException("Impossible de refuser un investissement déjà traité");
            }

            // CORRECTION : montantInvesti est déjà un BigDecimal
            BigDecimal montant = inv.getMontantInvesti();

            Wallet walletUser = walletRepository.findByUserIdWithPessimisticLock(inv.getInvestisseur().getId())
                            .orElseThrow(() -> new IllegalStateException("Wallet non trouvé"));

            // Libère les fonds bloqués dans le wallet de l'investisseur
            walletUser.debloquerFonds(montant);

            // Mise à jour du projet (soustraire les parts et le montant)
            Projet projet = inv.getProjet();
            projet.setPartsPrises(projet.getPartsPrises() - inv.getNombrePartsPris());

            // CORRECTION : Soustraction de BigDecimals
            if (projet.getMontantCollecte() != null) {
                    projet.setMontantCollecte(projet.getMontantCollecte().subtract(montant));
            }

            inv.setStatutPartInvestissement(StatutPartInvestissement.ANNULE);

            // Sauvegardes
            projetRepository.save(projet);
            walletRepository.save(walletUser);
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
        Investissement investissement  = investissementRepository.findById(projetId)
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



   




}
  


  
   
