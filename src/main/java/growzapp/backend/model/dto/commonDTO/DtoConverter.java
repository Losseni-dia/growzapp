package growzapp.backend.model.dto.commonDTO;

import growzapp.backend.model.dto.dividendeDTO.DividendeDTO;
import growzapp.backend.model.dto.documentDTO.DocumentDTO;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.dto.investisementDTO.InvestissementDTO;
import growzapp.backend.model.dto.localisationDTO.LocalisationDTO;
import growzapp.backend.model.dto.localiteDTO.LocaliteDTO;
import growzapp.backend.model.dto.paysDTO.PaysDTO;
import growzapp.backend.model.dto.projetDTO.ProjetDTO;
import growzapp.backend.model.dto.secteurDTO.SecteurDTO;
import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.model.dto.walletDTOs.TransactionDTO;
import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutDividende;
import growzapp.backend.model.enumeration.StatutFacture;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.model.enumeration.TypeTransaction;
import growzapp.backend.model.enumeration.WalletType;
import growzapp.backend.repository.LocalisationRepository;
import growzapp.backend.repository.SecteurRepository;
import growzapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.util.Objects;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor // AJOUTÉ – injecte les repositories ci-dessous
public class DtoConverter {

        private final SecteurRepository secteurRepository;
        private final UserRepository userRepository;
        private final LocalisationRepository localisationRepository;
      

        // === User ===
        public UserDTO toUserDto(User user) {
                if (user == null)
                        return null;

                UserDTO dto = new UserDTO();
                dto.setId(user.getId());
                dto.setImage(user.getImage());
                dto.setLogin(user.getLogin());
                dto.setPrenom(user.getPrenom());
                dto.setNom(user.getNom());
                dto.setEmail(user.getEmail());
                dto.setContact(user.getContact());
                dto.setSexe(user.getSexe());
                dto.setEnabled(user.isEnabled());

                // === AJOUT POUR LA LANGUE ===
                // On transmet la préférence de l'utilisateur au Frontend
                dto.setInterfaceLanguage(user.getInterfaceLanguage());

                // Localité + Pays
                if (user.getLocalite() != null) {
                        Localite loc = user.getLocalite();
                        String paysNom = loc.getPays() != null ? loc.getPays().getNom() : null;

                        LocaliteDTO locDto = new LocaliteDTO(
                                        loc.getId(),
                                        loc.getCodePostal(),
                                        loc.getNom(),
                                        paysNom,
                                        List.of(),
                                        List.of());
                        dto.setLocalite(locDto);
                }

                // Rôles
                dto.setRoles(user.getRoles().stream()
                                .map(r -> r.getRole())
                                .toList());

                // Langues (Parlées - compétences)
                dto.setLangues(user.getLangues() != null
                                ? user.getLangues().stream().map(Langue::getNom).toList()
                                : List.of());

                // PROJETS CRÉÉS PAR L'UTILISATEUR (porteur)
                // PROJETS CRÉÉS PAR L'UTILISATEUR (porteur)
                dto.setProjets(user.getProjets().stream()
                                .map(p -> new ProjetDTO(
                                                p.getId(),
                                                p.getPoster(),
                                                p.getReference(),
                                                p.getLibelle(),
                                                p.getDescription(),
                                                p.getValuation(), // BigDecimal
                                                p.getRoiProjete(), // double
                                                p.getPartsDisponible(), // int
                                                p.getPartsPrises(), // int
                                                p.getPrixUnePart(), // BigDecimal
                                                p.getObjectifFinancement(), // BigDecimal
                                                p.getMontantCollecte(), // BigDecimal
                                                "XOF", // <-- AJOUT : currencyCode (indispensable pour le Record)
                                                p.getDateDebut(),
                                                p.getDateFin(),
                                                p.getValeurTotalePartsEnPourcent(),
                                                p.getStatutProjet(),
                                                p.getCreatedAt(),
                                                p.getSiteProjet() != null && p.getSiteProjet().getLocalite() != null
                                                                ? p.getSiteProjet().getLocalite().getId()
                                                                : null,
                                                p.getPorteur() != null ? p.getPorteur().getId() : null,
                                                p.getSiteProjet() != null ? p.getSiteProjet().getId() : null,
                                                p.getSecteur() != null ? p.getSecteur().getId() : null,
                                                p.getSiteProjet() != null && p.getSiteProjet().getLocalite() != null
                                                                && p.getSiteProjet().getLocalite().getPays() != null
                                                                                ? p.getSiteProjet().getLocalite()
                                                                                                .getPays().getId()
                                                                                : null,
                                                p.getSiteProjet() != null && p.getSiteProjet().getLocalite() != null
                                                                && p.getSiteProjet().getLocalite().getPays() != null
                                                                                ? p.getSiteProjet().getLocalite()
                                                                                                .getPays().getNom()
                                                                                : null,
                                                p.getSiteProjet() != null && p.getSiteProjet().getLocalite() != null
                                                                ? p.getSiteProjet().getLocalite().getNom()
                                                                : null,
                                                p.getPorteur() != null
                                                                ? p.getPorteur().getPrenom() + " "
                                                                                + p.getPorteur().getNom()
                                                                : null,
                                                p.getSiteProjet() != null ? p.getSiteProjet().getNom() : null,
                                                p.getSecteur() != null ? p.getSecteur().getNom() : null,
                                                List.of(), // documents
                                                List.of() // investissements
                                ))
                                .toList());

// INVESTISSEMENTS DE L'UTILISATEUR
dto.setInvestissements(user.getInvestissements().stream()
    .map(inv -> {
        // Préparation des montants BigDecimal
        BigDecimal prixUnePart = (inv.getProjet() != null) 
            ? inv.getProjet().getPrixUnePart() 
            : BigDecimal.ZERO;

        BigDecimal montantInvesti = prixUnePart.multiply(BigDecimal.valueOf(inv.getNombrePartsPris()));

        return InvestissementDTO.builder()
            .id(inv.getId())
            .nombrePartsPris(inv.getNombrePartsPris())
            .date(inv.getDate())
            .valeurPartsPrisEnPourcent(inv.getValeurPartsPrisEnPourcent())
            .frais(inv.getFrais())
            .statutPartInvestissement(inv.getStatutPartInvestissement())
            .projetId(inv.getProjet() != null ? inv.getProjet().getId() : null)
            .projetLibelle(inv.getProjet() != null ? inv.getProjet().getLibelle() : null)
            
            // Correction des types monétaires
            .prixUnePart(prixUnePart) 
            .montantInvesti(montantInvesti)
            
            .projetPoster(inv.getProjet() != null ? inv.getProjet().getPoster() : null)
            .contratUrl(inv.getContrat() != null 
                ? "/api/contrats/" + inv.getContrat().getNumeroContrat() 
                : null)
            .numeroContrat(inv.getContrat() != null 
                ? inv.getContrat().getNumeroContrat() 
                : null)
            
            // Initialisation avec BigDecimal.ZERO au lieu de 0.0
            .dividendes(List.of())
            .montantTotalPercu(BigDecimal.ZERO)
            .montantTotalPlanifie(BigDecimal.ZERO)
            .roiRealise(0.0) // % reste en double
            .dividendesPayes(0)
            .dividendesPlanifies(0)
            .statutGlobalDividendes("Aucun dividende")
            .build();
                })
                .toList());

                return dto;
        }

        public User toUserEntity(UserDTO dto) {
                User user = new User();
                user.setId(dto.getId());
                user.setImage(dto.getImage());
                user.setLogin(dto.getLogin());
                user.setPrenom(dto.getPrenom());
                user.setNom(dto.getNom());
                user.setEmail(dto.getEmail());
                user.setContact(dto.getContact());
                user.setSexe(dto.getSexe());
                user.setEnabled(dto.isEnabled());
                return user;
        }
      
    // === Secteur ===
    public SecteurDTO toSecteurDto(Secteur secteur) {
        List<String> projetNoms = secteur.getProjets().stream()
                .map(Projet::getLibelle)
                .toList();

        return new SecteurDTO(
                secteur.getId(),
                secteur.getNom(),
                projetNoms);
    }

    public Secteur toSecteurEntity(SecteurDTO dto) {
        Secteur secteur = new Secteur();
        secteur.setId(dto.id());
        secteur.setNom(dto.nom());
        return secteur;
    }

    // === Pays ===
    public PaysDTO toPaysDto(Pays pays) {
        List<String> localiteNoms = pays.getLocalites().stream()
                .map(Localite::getNom)
                .toList();

        return new PaysDTO(
                pays.getId(),
                pays.getNom(),
                localiteNoms);
    }

    public Pays toPaysEntity(PaysDTO dto) {
        Pays pays = new Pays();
        pays.setId(dto.id());
        pays.setNom(dto.nom());
        return pays;
    }

    // === Localite ===
    public LocaliteDTO toLocaliteDto(Localite localite) {
        String paysNom = localite.getPays() != null ? localite.getPays().getNom() : null;

        List<LocaliteDTO.UserInfo> userInfos = localite.getUsers().stream()
                .map(user -> new LocaliteDTO.UserInfo(
                        user.getNom(),
                        user.getPrenom(),
                        user.getEmail()))
                .toList();

        List<LocaliteDTO.SiteInfo> siteInfos = localite.getLocalisations().stream()
                .map(loc -> new LocaliteDTO.SiteInfo(
                        loc.getId(),
                        loc.getNom(),
                        loc.getContact()))
                .toList();

        return new LocaliteDTO(
                localite.getId(),
                localite.getCodePostal(),
                localite.getNom(),
                paysNom,
                userInfos,
                siteInfos);
    }

    public Localite toLocaliteEntity(LocaliteDTO dto) {
        Localite localite = new Localite();
        localite.setId(dto.id());
        localite.setCodePostal(dto.codePostal());
        localite.setNom(dto.nom());
        return localite;
    }

    // Dans DtoConverter.java
    public LocalisationDTO toLocalisationDto(Localisation localisation) {
            String localiteNom = localisation.getLocalite() != null ? localisation.getLocalite().getNom() : null;
            Long localiteId = localisation.getLocalite() != null ? localisation.getLocalite().getId() : null;
            String paysNom = localisation.getLocalite() != null && localisation.getLocalite().getPays() != null
                            ? localisation.getLocalite().getPays().getNom()
                            : null;

            List<String> projetNoms = localisation.getProjets() != null
                            ? localisation.getProjets().stream()
                                            .map(Projet::getLibelle)
                                            .toList()
                            : List.of();

            return new LocalisationDTO(
                            localisation.getId(),
                            localisation.getNom(),
                            localisation.getAdresse(),
                            localisation.getContact(),
                            localisation.getResponsable(),
                            localisation.getCreatedAt(),
                            localiteNom,
                            localiteId,
                            paysNom, // AJOUTÉ
                            projetNoms);
    }

    public Localisation toLocalisationEntity(LocalisationDTO dto) {
            Localisation localisation = new Localisation();
            localisation.setId(dto.id());
            localisation.setNom(dto.nom());
            localisation.setAdresse(dto.adresse());
            localisation.setContact(dto.contact());
            localisation.setResponsable(dto.responsable());
            localisation.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now());
            return localisation;
    }



    // === Projet ===
    public ProjetDTO toProjetDto(Projet projet) {
            // IDs
            Long localiteId = projet.getSiteProjet() != null && projet.getSiteProjet().getLocalite() != null
                            ? projet.getSiteProjet().getLocalite().getId()
                            : null;

            Long porteurId = projet.getPorteur() != null ? projet.getPorteur().getId() : null;

            Long siteId = projet.getSiteProjet() != null ? projet.getSiteProjet().getId() : null;

            Long secteurId = projet.getSecteur() != null ? projet.getSecteur().getId() : null;

            // === PAYS ID (CORRIGÉ !) ===
            Long paysId = null;
            String paysNom = null;
            if (projet.getSiteProjet() != null
                            && projet.getSiteProjet().getLocalite() != null
                            && projet.getSiteProjet().getLocalite().getPays() != null) {
                    paysId = projet.getSiteProjet().getLocalite().getPays().getId(); // ← Long paysId
                    paysNom = projet.getSiteProjet().getLocalite().getPays().getNom(); // ← String paysNom
            }

            // Noms
            String localiteNom = localiteId != null ? projet.getSiteProjet().getLocalite().getNom() : null;
            String porteurNom = porteurId != null
                            ? projet.getPorteur().getPrenom() + " " + projet.getPorteur().getNom()
                            : null;
            String siteNom = siteId != null ? projet.getSiteProjet().getNom() : null;
            String secteurNom = secteurId != null ? projet.getSecteur().getNom() : null;

            // Documents et Investissements
            List<DocumentDTO> docDtos = projet.getDocuments().stream()
                            .map(this::toDocumentDto)
                            .toList();

            List<InvestissementDTO> investissementDtos = projet.getInvestissements().stream()
                            .map(this::toInvestissementDto)
                            .toList();
           

            return new ProjetDTO(
                            projet.getId(),
                            projet.getPoster(),
                            projet.getReference(),
                            projet.getLibelle(),
                            projet.getDescription(),
                            projet.getValuation(), // Est maintenant un BigDecimal
                            projet.getRoiProjete(),
                            projet.getPartsDisponible(),
                            projet.getPartsPrises(),
                            projet.getPrixUnePart(), // Est maintenant un BigDecimal
                            projet.getObjectifFinancement(), // Est maintenant un BigDecimal
                            projet.getMontantCollecte(), // Est maintenant un BigDecimal
                            "XOF",
                            projet.getDateDebut(),
                            projet.getDateFin(),
                            projet.getValeurTotalePartsEnPourcent(),
                            projet.getStatutProjet(),
                            projet.getCreatedAt(),
                            localiteId,
                            porteurId,
                            siteId,
                            secteurId,
                            paysId,
                            paysNom,
                            localiteNom,
                            porteurNom,
                            siteNom,
                            secteurNom,
                            docDtos,
                            investissementDtos);
    }

    public Projet toProjetEntity(ProjetDTO dto) {
            Projet projet = new Projet();
            projet.setId(dto.id());
            projet.setPoster(dto.poster());
            projet.setReference(dto.reference());
            projet.setLibelle(dto.libelle());
            projet.setDescription(dto.description());
            projet.setValuation(dto.valuation());
            projet.setRoiProjete(dto.roiProjete());
            projet.setPartsDisponible(dto.partsDisponible());
            projet.setPartsPrises(dto.partsPrises());
            projet.setPrixUnePart(dto.prixUnePart());
            projet.setObjectifFinancement(dto.objectifFinancement());
            projet.setMontantCollecte(dto.montantCollecte());
            projet.setDateDebut(dto.dateDebut());
            projet.setDateFin(dto.dateFin());
            projet.setValeurTotalePartsEnPourcent(dto.valeurTotalePartsEnPourcent());
            projet.setStatutProjet(dto.statutProjet() != null ? dto.statutProjet() : StatutProjet.EN_PREPARATION);
            projet.setCreatedAt(dto.createdAt() != null ? dto.createdAt() : LocalDateTime.now());
            return projet;
    }
    
    // === MÉTHODE CRITIQUE POUR L'UPDATE – À NE SURTOUT PAS SUPPRIMER ===
    public void updateProjetFromDto(ProjetDTO dto, Projet entity) {
            if (dto.libelle() != null && !dto.libelle().trim().isEmpty()) {
                    entity.setLibelle(dto.libelle().trim());
            }
            if (dto.description() != null)
                    entity.setDescription(dto.description());
            if (dto.reference() != null)
                    entity.setReference(dto.reference());

            // Correction pour les doubles (ROI et Parts sont restés en double/int)
            if (dto.roiProjete() > 0)
                    entity.setRoiProjete(dto.roiProjete());
            if (dto.partsDisponible() > 0)
                    entity.setPartsDisponible(dto.partsDisponible());

            // CORRECTION BIGDECIMAL : Utilisation de compareTo pour les montants
            if (dto.prixUnePart() != null && dto.prixUnePart().compareTo(BigDecimal.ZERO) > 0)
                    entity.setPrixUnePart(dto.prixUnePart());

            if (dto.objectifFinancement() != null && dto.objectifFinancement().compareTo(BigDecimal.ZERO) > 0)
                    entity.setObjectifFinancement(dto.objectifFinancement());

            if (dto.dateDebut() != null)
                    entity.setDateDebut(dto.dateDebut());
            if (dto.dateFin() != null)
                    entity.setDateFin(dto.dateFin());
            if (dto.statutProjet() != null)
                    entity.setStatutProjet(dto.statutProjet());

            // Relations (Secteur, Porteur, Site) - Inchangé
            if (dto.secteurId() != null && dto.secteurId() > 0) {
                    entity.setSecteur(secteurRepository.findById(dto.secteurId())
                                    .orElseThrow(() -> new RuntimeException(
                                                    "Secteur non trouvé (ID: " + dto.secteurId() + ")")));
            }
            if (dto.porteurId() != null && dto.porteurId() > 0) {
                    entity.setPorteur(userRepository.findById(dto.porteurId())
                                    .orElseThrow(() -> new RuntimeException(
                                                    "Porteur non trouvé (ID: " + dto.porteurId() + ")")));
            }
            if (dto.siteId() != null && dto.siteId() > 0) {
                    entity.setSiteProjet(localisationRepository.findById(dto.siteId())
                                    .orElseThrow(() -> new RuntimeException(
                                                    "Site non trouvé (ID: " + dto.siteId() + ")")));
            }
    }

    public InvestissementDTO toInvestissementDto(Investissement investissement) {
            // === Base & Noms ===
            Long investisseurId = investissement.getInvestisseur() != null ? investissement.getInvestisseur().getId()
                            : null;
            String investisseurNom = investissement.getInvestisseur() != null
                            ? investissement.getInvestisseur().getPrenom() + " "
                                            + investissement.getInvestisseur().getNom()
                            : "Inconnu";

            Long projetId = investissement.getProjet() != null ? investissement.getProjet().getId() : null;
            String projetLibelle = investissement.getProjet() != null ? investissement.getProjet().getLibelle()
                            : "Projet inconnu";

            // === CALCULS BIGDECIMAL (Sûr pour l'argent) ===
            BigDecimal prixUnePart = (investissement.getProjet() != null) ? investissement.getProjet().getPrixUnePart()
                            : BigDecimal.ZERO;

            BigDecimal montantInvesti = prixUnePart.multiply(BigDecimal.valueOf(investissement.getNombrePartsPris()));

            // === Contrat & Poster ===
            String projetPoster = investissement.getProjet() != null ? investissement.getProjet().getPoster() : null;
            String contratUrl = (investissement.getContrat() != null)
                            ? "http://localhost:8080/api/contrats/" + investissement.getContrat().getNumeroContrat()
                            : null;
            String numeroContrat = (investissement.getContrat() != null)
                            ? investissement.getContrat().getNumeroContrat()
                            : null;

            // === Gestion des Dividendes ===
            List<DividendeDTO> dividendes = investissement.getDividendes() != null
                            ? investissement.getDividendes().stream().map(this::toDividendeDto).toList()
                            : List.of();

            BigDecimal montantPercu = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PAYE)
                            .map(DividendeDTO::montantTotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal montantPlanifie = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PLANIFIE)
                            .map(DividendeDTO::montantTotal)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            // === ROI & Stats (Calculé en double pour le % visuel) ===
            double roiRealise = 0.0;
            if (montantInvesti.compareTo(BigDecimal.ZERO) > 0) {
                    roiRealise = montantPercu.divide(montantInvesti, java.math.MathContext.DECIMAL128)
                                    .multiply(new BigDecimal("100")).doubleValue();
            }

            long payes = dividendes.stream().filter(d -> d.statutDividende() == StatutDividende.PAYE).count();
            long planifies = dividendes.stream().filter(d -> d.statutDividende() == StatutDividende.PLANIFIE).count();

            String statutGlobal = payes == 0 ? "Aucun dividende"
                            : planifies == 0 ? "Tous payés" : "Partiel (" + payes + "/" + (payes + planifies) + ")";

            // === RETOUR BUILDER (Types alignés avec le record InvestissementDTO) ===
            return InvestissementDTO.builder()
                            .id(investissement.getId())
                            .nombrePartsPris(investissement.getNombrePartsPris())
                            .date(investissement.getDate())
                            .valeurPartsPrisEnPourcent(investissement.getValeurPartsPrisEnPourcent())
                            .frais(investissement.getFrais())
                            .statutPartInvestissement(investissement.getStatutPartInvestissement())
                            .investisseurId(investisseurId)
                            .investisseurNom(investisseurNom)
                            .projetId(projetId)
                            .projetLibelle(projetLibelle)
                            .prixUnePart(prixUnePart) // Envoie le BigDecimal
                            .montantInvesti(montantInvesti) // Envoie le BigDecimal
                            .projetPoster(projetPoster)
                            .contratUrl(contratUrl)
                            .numeroContrat(numeroContrat)
                            .dividendes(dividendes)
                            .montantTotalPercu(montantPercu) // Envoie le BigDecimal
                            .montantTotalPlanifie(montantPlanifie) // Envoie le BigDecimal
                            .roiRealise(roiRealise)
                            .dividendesPayes((int) payes)
                            .dividendesPlanifies((int) planifies)
                            .statutGlobalDividendes(statutGlobal)
                            .build();
    }

    public Investissement toInvestissementEntity(InvestissementDTO dto) {
        Investissement investissement = new Investissement();
        investissement.setId(dto.id());
        investissement.setNombrePartsPris(dto.nombrePartsPris());
        investissement.setDate(dto.date() != null ? dto.date() : LocalDateTime.now());
        investissement.setValeurPartsPrisEnPourcent(dto.valeurPartsPrisEnPourcent());
        investissement.setFrais(dto.frais());
        investissement.setStatutPartInvestissement(dto.statutPartInvestissement());
        return investissement;
    }

    // === Document ===
    // === DocumentDTO ↔ Document Entity ===
    public DocumentDTO toDocumentDto(Document document) {
            return new DocumentDTO(
                            document.getId(),
                            document.getNom(),
                            document.getUrl(), // ← getUrl() reconstruit proprement /files/documents/uuid_xxx
                            document.getType(),
                            document.getUploadedAt());
    }

    public Document toDocumentEntity(DocumentDTO dto) {
            Document doc = new Document();
            doc.setId(dto.id());
            doc.setNom(dto.nom());
            // On ne touche PAS à filename ici → c’est géré uniquement à l’upload
            doc.setType(dto.type());
            doc.setUploadedAt(dto.uploadedAt() != null ? dto.uploadedAt() : LocalDateTime.now());
            return doc;
    }

    public FactureDTO toFactureDto(Facture facture) {
            if (facture == null)
                    return null;

            Long investisseurId = facture.getInvestisseur() != null ? facture.getInvestisseur().getId() : null;
            String investisseurNom = facture.getInvestisseur() != null
                            ? facture.getInvestisseur().getPrenom() + " " + facture.getInvestisseur().getNom()
                            : "Inconnu";

            Long dividendeId = facture.getDividende() != null ? facture.getDividende().getId() : null;

            return new FactureDTO(
                            facture.getId(),
                            facture.getNumeroFacture(),
                            facture.getMontantHT(),
                            facture.getTva(),
                            facture.getMontantTTC(),
                            facture.getDateEmission(),
                            facture.getDatePaiement(),
                            facture.getStatut(),
                            dividendeId,
                            investisseurId,
                            investisseurNom,
                            facture.getFichierUrl());
    }

    public Facture toFactureEntity(FactureDTO dto) {
            if (dto == null)
                    return null;

            Facture facture = new Facture();
            facture.setId(dto.id());
            facture.setNumeroFacture(dto.numeroFacture());
            facture.setMontantHT(dto.montantHT() != null ? dto.montantHT() : 0.0);
            facture.setTva(dto.tva() != null ? dto.tva() : 0.0);
            facture.setMontantTTC(dto.montantTTC() != null ? dto.montantTTC() : 0.0);
            facture.setDateEmission(dto.dateEmission());
            facture.setDatePaiement(dto.datePaiement());
            facture.setStatut(dto.statut() != null ? dto.statut() : StatutFacture.EMISE);
            facture.setFichierUrl(dto.fichierUrl());
            return facture;
    }

    // ========================================================================
    // ========================= DIVIDENDE (CORRIGÉ) ==========================
    // ========================================================================
    public DividendeDTO toDividendeDto(Dividende dividende) {
            if (dividende == null)
                    return null;

            String investissementInfo = "Investissement inconnu";
            BigDecimal montantTotal = BigDecimal.ZERO;
            Long investissementId = null;

            // Champs simples pour la facture (extraction directe)
            String factureUrl = null;
            String fileName = null;
            FactureDTO factureDto = null;

            // Gestion Investissement
            if (dividende.getInvestissement() != null) {
                    Investissement inv = dividende.getInvestissement();
                    investissementId = inv.getId();

                    Projet projet = inv.getProjet();
                    User investisseur = inv.getInvestisseur();

                    String projetNom = projet != null ? projet.getLibelle() : "Projet inconnu";
                    String investisseurNom = investisseur != null
                                    ? investisseur.getPrenom() + " " + investisseur.getNom()
                                    : "Investisseur inconnu";

                    investissementInfo = projetNom + " - " + investisseurNom;

                    // Calcul du montant total
                    if (dividende.getMontantParPart() != null) {
                            montantTotal = dividende.getMontantParPart()
                                            .multiply(BigDecimal.valueOf(inv.getNombrePartsPris()));
                    }
            }

            // Gestion Facture (Extraction propre)
            if (dividende.getFacture() != null) {
                    // Conversion complète en DTO pour avoir l'ID et le numéro
                    factureDto = toFactureDto(dividende.getFacture());

                    // Remplissage des champs de compatibilité
                    if (factureDto.fichierUrl() != null) {
                            factureUrl = factureDto.fichierUrl();
                            fileName = factureUrl.substring(factureUrl.lastIndexOf("/") + 1);
                    }
            }

            return new DividendeDTO(
                            dividende.getId(),
                            dividende.getMontantParPart() != null ? dividende.getMontantParPart() : BigDecimal.ZERO,
                            dividende.getStatutDividende(),
                            dividende.getMoyenPaiement(),
                            dividende.getDatePaiement() != null ? dividende.getDatePaiement().toLocalDate() : null,
                            investissementId,
                            investissementInfo,
                            montantTotal,
                            fileName, // optionnel
                            factureUrl, // URL directe
                            factureDto,
                            dividende.getMotif()
                                             
                                             // <--- AJOUT CRUCIAL : Le DTO complet de la facture
            );
    }

    public Dividende toDividendeEntity(DividendeDTO dto) {
            if (dto == null) {
                    return null;
            }

            Dividende dividende = new Dividende();
            dividende.setId(dto.id());

            // Gestion sécurisée du montant (évite null)
            dividende.setMontantParPart(
                            dto.montantParPart() != null ? dto.montantParPart() : BigDecimal.ZERO);

            // Statut par défaut si non fourni : PLANIFIE
            dividende.setStatutDividende(
                            dto.statutDividende() != null ? dto.statutDividende() : StatutDividende.PLANIFIE);

            dividende.setMoyenPaiement(dto.moyenPaiement());

            // Conversion LocalDate (DTO) -> LocalDateTime (Entity)
            if (dto.datePaiement() != null) {
                    dividende.setDatePaiement(dto.datePaiement().atStartOfDay());
            }

            // Note : On ne set pas l'Investissement ou la Facture ici car cela demande
            // généralement de faire des appels en base de données (findById).
            // C'est le rôle du Service de faire ces liens.

            return dividende;
    }


        // ==================================================================
    // ========================= TRANSACTION ============================
    // ==================================================================

        // Dans DtoConverter.java → AJOUTE ÇA À LA FIN

        public TransactionDTO toTransactionDto(Transaction transaction) {
                if (transaction == null) {
                        return null;
                }

                // === PROPRIÉTAIRE DU WALLET (on ne charge PLUS tout l'utilisateur) ===
                Long ownerId = null;
                String ownerPrenom = "Utilisateur";
                String ownerNom = "supprimé";
                String ownerLogin = "inconnu";

                if (transaction.getWalletType() == WalletType.USER && transaction.getWalletId() != null) {
                        Optional<Map<String, Object>> info = userRepository
                                        .findBasicInfoByWalletId(transaction.getWalletId());
                        if (info.isPresent()) {
                                Map<String, Object> map = info.get();
                                ownerId = (Long) map.get("id");
                                ownerPrenom = (String) map.get("prenom");
                                ownerNom = (String) map.get("nom");
                                ownerLogin = (String) map.get("login");
                        }
                }

                // === CONTREPARTIE TRANSFERT (déjà chargé par EntityGraph) ===
                String counterpartFullName = null;
                Long counterpartId = null;

                if (transaction.getDestinataireWallet() != null
                                && transaction.getDestinataireWallet().getUser() != null) {
                        User counterpart = transaction.getDestinataireWallet().getUser();
                        counterpartFullName = counterpart.getPrenom() + " " + counterpart.getNom();
                        counterpartId = counterpart.getId();
                }

                return new TransactionDTO(
                                transaction.getId(),
                                transaction.getMontant(),
                                transaction.getType(),
                                transaction.getStatut(),
                                transaction.getCreatedAt(),
                                transaction.getCompletedAt(),
                                transaction.getDescription(),
                                ownerId,
                                ownerPrenom,
                                ownerNom,
                                ownerLogin,
                                transaction.getType() == TypeTransaction.TRANSFER_OUT ? counterpartId : null,
                                transaction.getType() == TypeTransaction.TRANSFER_OUT ? counterpartFullName : null,
                                null,
                                transaction.getType() == TypeTransaction.TRANSFER_IN ? counterpartId : null,
                                transaction.getType() == TypeTransaction.TRANSFER_IN ? counterpartFullName : null,
                                null);
        }

}