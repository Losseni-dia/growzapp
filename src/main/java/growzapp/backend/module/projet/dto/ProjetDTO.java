package growzapp.backend.module.projet.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import growzapp.backend.module.investissement.dto.InvestissementDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Représentation complète d'un projet d'investissement")
public class ProjetDTO {

        @Schema(description = "Identifiant unique du projet", example = "1")
        private Long id;

        @Schema(description = "Slug unique pour l'URL", example = "ferme-avicole-de-korhogo")
        private String slug;

        @Schema(description = "URL du poster du projet")
        private String poster;

        @Schema(description = "Référence unique du projet")
        private Integer reference;

        @Schema(description = "Titre du projet", example = "Ferme avicole de Korhogo")
        private String libelle;

        @Schema(description = "Description détaillée du projet")
        private String description;

        @Schema(description = "Valorisation totale du projet", example = "12500000")
        private BigDecimal valuation;

        @Schema(description = "ROI projeté en pourcentage", example = "15.5")
        private double roiProjete;

        @Schema(description = "Nombre de parts disponibles", example = "500")
        private int partsDisponible;

        @Schema(description = "Nombre de parts prises", example = "0")
        private int partsPrises;

        @Schema(description = "Prix d'une part", example = "10000")
        private BigDecimal prixUnePart;

        @Schema(description = "Objectif de financement", example = "5000000")
        private BigDecimal objectifFinancement;

        @Schema(description = "Montant collecté", example = "0")
        private BigDecimal montantCollecte;

        @Schema(description = "Code de la devise", example = "XOF")
        private String currencyCode;

        @Schema(description = "Date de début du financement", example = "2026-08-01")
        private LocalDate dateDebut;

        @Schema(description = "Date de fin du financement", example = "2028-08-01")
        private LocalDate dateFin;

        @Schema(description = "Durée du projet en mois", example = "24")
        private Integer dureeMois;

        @Schema(description = "Pourcentage des parts à lever", example = "40")
        private int valeurTotalePartsEnPourcent;

        @Schema(description = "Statut du projet", example = "SOUMIS")
        private String statutProjet;

        @Schema(description = "Date de création du projet")
        private LocalDateTime createdAt;

        @Schema(description = "Date de certification du projet")
        private LocalDateTime certifiedAt;

        // ── Relations ─────────────────────────────────────────────────────────
        @Schema(description = "ID de la localité")
        private Long localiteId;

        @Schema(description = "ID du porteur")
        private Long porteurId;

        @Schema(description = "ID du site")
        private Long siteId;

        @Schema(description = "ID du secteur")
        private Long secteurId;

        @Schema(description = "ID du pays")
        private Long paysId;

        @Schema(description = "Nom du pays", example = "Côte d'Ivoire")
        private String paysNom;

        @Schema(description = "Nom de la localité", example = "Korhogo")
        private String localiteNom;

        @Schema(description = "Nom complet du porteur", example = "Losseni Diakité")
        private String porteurNom;

        @Schema(description = "Nom du site du projet")
        private String siteNom;

        @Schema(description = "Nom du secteur", example = "Agriculture")
        private String secteurNom;

        @Schema(description = "Latitude du site")
        private Double latitude;

        @Schema(description = "Longitude du site")
        private Double longitude;

        @Schema(description = "Adresse What3Words du site")
        private String what3words;

        @Schema(description = "URL Google Maps du site")
        private String googleMapsUrl;

        @Schema(description = "Documents associés au projet")
        private List<Object> documents = new ArrayList<>();

        @Schema(description = "Investissements réalisés sur ce projet")
        private List<InvestissementDTO> investissements = new ArrayList<>();

        // ── Traductions ───────────────────────────────────────────────────────
        @Schema(description = "Libellé traduit selon la langue de l'utilisateur")
        private String libelleTradu;

        @Schema(description = "Description traduite selon la langue de l'utilisateur")
        private String descriptionTradu;
}