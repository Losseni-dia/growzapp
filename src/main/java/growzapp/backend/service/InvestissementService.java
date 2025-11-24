// src/main/java/growzapp/backend/service/InvestissementService.java
package growzapp.backend.service;


import com.google.zxing.WriterException;
import com.lowagie.text.DocumentException;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.investisementDTO.InvestissementCreateDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementRequestDto;
import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.model.entite.Wallet;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.InvestissementRepository;
import growzapp.backend.repository.ProjetRepository;
import growzapp.backend.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InvestissementService {

    private final InvestissementRepository repository;
    private final ContratService contratService;
    private final EmailService emailService;
    private final InvestissementRepository investissementRepository;
    private final DtoConverter converter;
    private final ProjetRepository projetRepository;
    private final UserRepository userRepository;
    

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


    

@Transactional
public Investissement validerInvestissement(Long id) throws IOException, DocumentException, WriterException {
    Investissement inv = investissementRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Investissement non trouvé"));

    if (inv.getStatutPartInvestissement() != StatutPartInvestissement.EN_ATTENTE) {
        throw new IllegalStateException("Investissement déjà traité");
    }

    Projet projet = inv.getProjet();
    Wallet wallet = inv.getInvestisseur().getWallet();

    // 1. Valider les fonds bloqués → transférer définitivement
    wallet.validerInvestissement(inv.getMontantInvesti());

    // 2. Mettre à jour le projet
    projet.setPartsPrises(projet.getPartsPrises() + inv.getNombrePartsPris());
    projet.setMontantCollecte(projet.getMontantCollecte() + inv.getMontantInvesti());

    // 3. Générer contrat + PDF + email
    Contrat contrat = contratService.genererEtSauvegarderContrat(inv);
    inv.setContrat(contrat);

    byte[] pdf = contratService.genererPdfDepuisContrat(contrat);
    emailService.envoyerContratParEmail(inv, pdf);

    // 4. Statut final
    inv.setStatutPartInvestissement(StatutPartInvestissement.VALIDE);

    return investissementRepository.save(inv);
}

   public Investissement findById(Long id) {
       return repository.findById(id)
               .orElse(null); // Retourne null si non trouvé
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





@Transactional
public InvestissementDTO investir(InvestissementRequestDto dto, User investisseur) {
    // 1. Récupérer le projet avec verrou pessimiste (évite les sur-réservations)
    Projet projet = projetRepository.findByIdWithLock(dto.projetId())
        .orElseThrow(() -> new EntityNotFoundException("Projet non trouvé"));

    if (projet.getStatutProjet() != StatutProjet.EN_COURS) {
        throw new IllegalStateException("Ce projet n'accepte plus d'investissements");
    }

    // 2. Calculs
    double prixPart = projet.getPrixUnePart();
    double montantTotal = dto.nombrePartsPris() * prixPart;

    if (dto.nombrePartsPris() <= 0) {
        throw new IllegalArgumentException("Le nombre de parts doit être positif");
    }

    int partsRestantes = projet.getPartsDisponible() - projet.getPartsPrises();
    if (dto.nombrePartsPris() > partsRestantes) {
        throw new IllegalStateException("Pas assez de parts disponibles");
    }

    // 3. Récupérer le wallet de l'investisseur
    Wallet wallet = investisseur.getWallet();
    if (wallet == null) {
        throw new IllegalStateException("Wallet non configuré");
    }

    // 4. Bloquer les fonds
    wallet.bloquerFonds(montantTotal);

    // 5. Créer l'investissement
    Investissement investissement = new Investissement();
    investissement.setNombrePartsPris(dto.nombrePartsPris());
    investissement.setMontantInvesti(montantTotal);
    investissement.setInvestisseur(investisseur);
    investissement.setProjet(projet);
    investissement.setStatutPartInvestissement(StatutPartInvestissement.EN_ATTENTE);
    investissement.setDate(LocalDateTime.now());
    investissement.calculerPourcentageEquity(); // méthode dans l'entité

    investissement = investissementRepository.save(investissement);

    return converter.toInvestissementDto(investissement);
}
  
}

  
   
