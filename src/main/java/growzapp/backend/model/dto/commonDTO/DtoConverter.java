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
import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutDividende;
import growzapp.backend.model.enumeration.StatutProjet;
import growzapp.backend.repository.LocalisationRepository;
import growzapp.backend.repository.SecteurRepository;
import growzapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor // AJOUTÉ – injecte les repositories ci-dessous
public class DtoConverter {

        private final SecteurRepository secteurRepository;
        private final UserRepository userRepository;
        private final LocalisationRepository localisationRepository;

        public UserDTO toUserDto(User user) {
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

                if (user.getLocalite() != null) {
                        Localite loc = user.getLocalite();
                        dto.setLocalite(new LocaliteDTO(
                                        loc.getId(),
                                        loc.getCodePostal(),
                                        loc.getNom(),
                                        loc.getPays() != null ? loc.getPays().getNom() : null,
                                        List.of(),
                                        List.of()));
                }

                dto.setRoles(user.getRoles().stream()
                                .map(r -> r.getRole())
                                .toList());

                dto.setLangues(user.getLangues() != null
                                ? user.getLangues().stream().map(Langue::getNom).toList()
                                : List.of());

                dto.setProjets(user.getProjets().stream()
                                .map(this::toProjetDto)
                                .toList());

                // === INVESTISSEMENTS FAITS ===
                dto.setInvestissements(user.getInvestissements().stream()
                                .map(this::toInvestissementDto)
                                .toList());
                return dto;
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
                            projet.getValuation(),
                            projet.getRoiProjete(),
                            projet.getPartsDisponible(),
                            projet.getPartsPrises(),
                            projet.getPrixUnePart(),
                            projet.getObjectifFinancement(),
                            projet.getMontantCollecte(),
                            projet.getDateDebut(),
                            projet.getDateFin(),
                            projet.getValeurTotalePartsEnPourcent(),
                            projet.getStatutProjet(),
                            projet.getCreatedAt(),

                            localiteId,
                            porteurId,
                            siteId,
                            secteurId,
                            paysId, // ← Maintenant c’est bien un Long !
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
            if (dto.roiProjete() >0)
                    entity.setRoiProjete(dto.roiProjete());
            if (dto.partsDisponible() >0)
                    entity.setPartsDisponible(dto.partsDisponible());
            if (dto.prixUnePart() >0)
                    entity.setPrixUnePart(dto.prixUnePart());
            if (dto.objectifFinancement() >0)
                    entity.setObjectifFinancement(dto.objectifFinancement());
            if (dto.dateDebut() != null)
                    entity.setDateDebut(dto.dateDebut());
            if (dto.dateFin() != null)
                    entity.setDateFin(dto.dateFin());
            if (dto.statutProjet() != null)
                    entity.setStatutProjet(dto.statutProjet());

            // Relations
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

    // === Investissement ===
    public InvestissementDTO toInvestissementDto(Investissement investissement) {
            // === Base ===
            Long investisseurId = investissement.getInvestisseur() != null
                            ? investissement.getInvestisseur().getId()
                            : null;

            String investisseurNom = investissement.getInvestisseur() != null
                            ? investissement.getInvestisseur().getPrenom() + " "
                                            + investissement.getInvestisseur().getNom()
                            : "Inconnu";

            Long projetId = investissement.getProjet() != null
                            ? investissement.getProjet().getId()
                            : null;

            String projetLibelle = investissement.getProjet() != null
                            ? investissement.getProjet().getLibelle()
                            : "Projet inconnu";

            double prixUnePart = investissement.getProjet() != null
                            ? investissement.getProjet().getPrixUnePart()
                            : 0.0;

            // === Dividendes ===
            List<DividendeDTO> dividendes = investissement.getDividendes() != null
                            ? investissement.getDividendes().stream()
                                            .map(this::toDividendeDto)
                                            .toList()
                            : List.of();

            // === Calculs ===
            double montantPercu = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PAYE)
                            .mapToDouble(DividendeDTO::montantTotal)
                            .sum();

            double montantPlanifie = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PLANIFIE)
                            .mapToDouble(DividendeDTO::montantTotal)
                            .sum();

            double valeurInitiale = investissement.getNombrePartsPris() * prixUnePart;
            double roiRealise = valeurInitiale > 0 ? (montantPercu / valeurInitiale) * 100 : 0;

            long payes = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PAYE)
                            .count();

            long planifies = dividendes.stream()
                            .filter(d -> d.statutDividende() == StatutDividende.PLANIFIE)
                            .count();

            String statutGlobal = payes == 0 ? "Aucun dividende"
                            : planifies == 0 ? "Tous payés"
                                            : "Partiel (" + payes + "/" + (payes + planifies) + ")";

            // === RETOUR DTO — ORDRE EXACT ===
            return new InvestissementDTO(
                            investissement.getId(), // 1
                            investissement.getNombrePartsPris(), // 2
                            investissement.getDate(), // 3
                            investissement.getValeurPartsPrisEnPourcent(), // 4
                            investissement.getFrais(), // 5
                            investissement.getStatutPartInvestissement(), // 6
                            investisseurId, // 7
                            projetId, // 8
                            investisseurNom, // 9
                            projetLibelle, // 10
                            prixUnePart, // 11
                            dividendes, // 12
                            montantPercu, // 13
                            montantPlanifie, // 14
                            roiRealise, // 15
                            (int) payes, // 16
                            (int) planifies, // 17
                            statutGlobal // 18
            );
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
    public DocumentDTO toDocumentDto(Document document) {
        return new DocumentDTO(
                document.getId(),
                document.getNom(),
                document.getUrl(),
                document.getType(),
                document.getUploadedAt());
    }

    public Document toDocumentEntity(DocumentDTO dto) {
        Document doc = new Document();
        doc.setId(dto.id());
        doc.setNom(dto.nom());
        doc.setUrl(dto.url());
        doc.setType(dto.type());
        doc.setUploadedAt(dto.uploadedAt() != null ? dto.uploadedAt() : LocalDateTime.now());
        return doc;
    }

    // === Dividende ===
    // === Dividende ===
    public DividendeDTO toDividendeDto(Dividende dividende) {
            String investissementInfo = "Investissement inconnu";
            double montantTotal = 0.0;
            Long investissementId = null;

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
                    montantTotal = inv.getNombrePartsPris() * dividende.getMontantParPart();
            }

            String fileName = null;
            if (dividende.getFacture() != null && dividende.getFacture().getFichierUrl() != null) {
                    String url = dividende.getFacture().getFichierUrl();
                    fileName = url.substring(url.lastIndexOf("/") + 1);
            }

            return new DividendeDTO(
                            dividende.getId(),
                            dividende.getMontantParPart(),
                            dividende.getStatutDividende(),
                            dividende.getMoyenPaiement(),
                            dividende.getDatePaiement() != null ? dividende.getDatePaiement().toLocalDate() : null,
                            investissementId,
                            investissementInfo,
                            montantTotal,
                            fileName);
    }


    public Dividende toDividendeEntity(DividendeDTO dto) {
            Dividende dividende = new Dividende();

            dividende.setId(dto.id());
            dividende.setMontantParPart(dto.montantParPart());
            dividende.setStatutDividende(
                            dto.statutDividende() != null ? dto.statutDividende() : StatutDividende.PLANIFIE);
            dividende.setMoyenPaiement(dto.moyenPaiement());
            dividende.setDatePaiement(dto.datePaiement() != null ? dto.datePaiement().atStartOfDay() : null);

            return dividende;
    }
    // === Facture ===
    public FactureDTO toFactureDto(Facture facture) {
        Long investisseurId = facture.getInvestisseur() != null ? facture.getInvestisseur().getId() : null;
        String investisseurNom = facture.getInvestisseur() != null
                ? facture.getInvestisseur().getPrenom() + " " + facture.getInvestisseur().getNom()
                : null;

        return new FactureDTO(
                facture.getId(),
                facture.getNumeroFacture(),
                facture.getMontantHT(),
                facture.getTva(),
                facture.getMontantTTC(),
                facture.getDateEmission(),
                facture.getDatePaiement(),
                facture.getStatut(),
                facture.getDividende().getId(),
                investisseurId,
                investisseurNom,
                facture.getFichierUrl());
    }

    public Facture toFactureEntity(FactureDTO dto) {
        Facture facture = new Facture();
        facture.setId(dto.id());
        facture.setNumeroFacture(dto.numeroFacture());
        facture.setMontantHT(dto.montantHT() != null ? dto.montantHT() : 0.0);
        facture.setTva(dto.tva() != null ? dto.tva() : 0.0);
        facture.setMontantTTC(dto.montantTTC() != null ? dto.montantTTC() : 0.0);
        facture.setDateEmission(dto.dateEmission());
        facture.setDatePaiement(dto.datePaiement());
        facture.setStatut(dto.statut());
        return facture;
    }
}