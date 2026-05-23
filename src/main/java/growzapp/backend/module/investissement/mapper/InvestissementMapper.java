package growzapp.backend.module.investissement.mapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.dividende.enums.StatutDividende;
import growzapp.backend.module.dividende.mapper.DividendeMapper;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.referentiel.model.Localisation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { DividendeMapper.class })
public abstract class InvestissementMapper {

        @Autowired
        protected DividendeMapper dividendeMapper;

        // --- 1. MAPPING ENTITY -> DTO (Déclaratif) ---
        @Mapping(target = "investisseurId", source = "investisseur.id")
        @Mapping(target = "investisseurNom", expression = "java(i.getInvestisseur() != null ? i.getInvestisseur().getPrenom() + \" \" + i.getInvestisseur().getNom() : \"Inconnu\")")
        @Mapping(target = "projetId", source = "projet.id")
        @Mapping(target = "projetLibelle", source = "projet.libelle", defaultValue = "Projet inconnu")
        @Mapping(target = "prixUnePart", source = "projet.prixUnePart")
        @Mapping(target = "projetPoster", source = "projet.poster")
        @Mapping(target = "numeroContrat", source = "contrat.numeroContrat")
        @Mapping(target = "latitude", source = "projet.siteProjet.latitude")
        @Mapping(target = "longitude", source = "projet.siteProjet.longitude")
        @Mapping(target = "googleMapsUrl", source = "projet.siteProjet.googleMapsUrl")
        @Mapping(target = "contratUrl", expression = "java(i.getContrat() != null ? \"http://localhost:8080/api/contrats/\" + i.getContrat().getNumeroContrat() : null)")
        public abstract InvestissementDTO toDto(Investissement i);

        // --- 2. MAPPING DTO -> ENTITY ---
        @Mapping(target = "id", source = "id")
        @Mapping(target = "nombrePartsPris", source = "nombrePartsPris")
        @Mapping(target = "valeurPartsPrisEnPourcent", source = "valeurPartsPrisEnPourcent")
        @Mapping(target = "frais", source = "frais")
        @Mapping(target = "statutPartInvestissement", source = "statutPartInvestissement")
        @Mapping(target = "date", expression = "java(dto.date() != null ? dto.date() : java.time.LocalDateTime.now())")
        public abstract Investissement toEntity(InvestissementDTO dto);

        // --- 3. CALCULS FINANCIERS COMPLEXE (Post-Mapping) ---
        @AfterMapping
        protected void calculsFinanciers(Investissement i,
                        @MappingTarget InvestissementDTO.InvestissementDTOBuilder builder) {
                // Validation et calcul sécurisé du montant investi
                BigDecimal prixUnePart = (i.getProjet() != null && i.getProjet().getPrixUnePart() != null)
                                ? i.getProjet().getPrixUnePart()
                                : BigDecimal.ZERO;
                BigDecimal montantInvesti = prixUnePart.multiply(BigDecimal.valueOf(i.getNombrePartsPris()));
                builder.montantInvesti(montantInvesti);

                // Fallback dynamique de l'URL Google Maps
                if (i.getProjet() != null && i.getProjet().getSiteProjet() != null) {
                        Localisation site = i.getProjet().getSiteProjet();
                        if (site.getGoogleMapsUrl() == null && site.getLatitude() != null
                                        && site.getLongitude() != null) {
                                builder.googleMapsUrl("https://www.google.com/maps/search/?api=1&query="
                                                + site.getLatitude() + "," + site.getLongitude());
                        }
                }

                // Récupération des dividendes déjà convertis par le sub-mapper DividendeMapper
                List<DividendeDTO> listDividendes = i.getDividendes() != null
                                ? i.getDividendes().stream().map(dividendeMapper::toDto).toList()
                                : List.of();

                // Agrégation des montants
                BigDecimal montantPercu = listDividendes.stream()
                                .filter(d -> d.statutDividende() == StatutDividende.PAYE)
                                .map(DividendeDTO::montantTotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal montantPlanifie = listDividendes.stream()
                                .filter(d -> d.statutDividende() == StatutDividende.PLANIFIE)
                                .map(DividendeDTO::montantTotal)
                                .filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calcul de performance ROI
                double roiRealise = 0.0;
                if (montantInvesti.compareTo(BigDecimal.ZERO) > 0) {
                        roiRealise = montantPercu.divide(montantInvesti, java.math.MathContext.DECIMAL128)
                                        .multiply(new BigDecimal("100")).doubleValue();
                }

                // Compteurs d'états
                long payes = listDividendes.stream().filter(d -> d.statutDividende() == StatutDividende.PAYE).count();
                long planifies = listDividendes.stream().filter(d -> d.statutDividende() == StatutDividende.PLANIFIE)
                                .count();

                String statutGlobal = payes == 0 ? "Aucun dividende"
                                : planifies == 0 ? "Tous payés"
                                                : "Partiel (" + payes + "/" + (payes + planifies) + ")";

                // Injection des calculs dans le Builder de ton Record DTO
                builder.montantTotalPercu(montantPercu)
                                .montantTotalPlanifie(montantPlanifie)
                                .roiRealise(roiRealise)
                                .dividendesPayes((int) payes)
                                .dividendesPlanifies((int) planifies)
                                .statutGlobalDividendes(statutGlobal);
        }
}