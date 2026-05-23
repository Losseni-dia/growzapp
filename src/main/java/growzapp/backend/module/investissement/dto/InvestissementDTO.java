package growzapp.backend.module.investissement.dto;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
@Schema(description = "Représentation complète d'un investissement sur un projet")
public record InvestissementDTO(
        @Schema(description = "Identifiant unique de l'investissement", example = "15")
        Long id,

        @Schema(description = "Nombre de parts achetées", example = "5")
        int nombrePartsPris,

        @Schema(description = "Date et heure de l'investissement", example = "2025-09-10T14:30:00")
        LocalDateTime date,

        @Schema(description = "Valeur des parts en pourcentage du projet", type = "number", format = "double", example = "2.5")
        double valeurPartsPrisEnPourcent,

        @Schema(description = "Frais de transaction en pourcentage", type = "number", format = "double", example = "1.5")
        double frais,

        @Schema(description = "Statut de la participation à l'investissement",
                example = "EN_ATTENTE",
                allowableValues = {"EN_ATTENTE", "VALIDE", "ANNULE", "REMBOURSE"})
        StatutPartInvestissement statutPartInvestissement,

        @Schema(description = "Identifiant de l'investisseur", example = "42")
        Long investisseurId,

        @Schema(description = "Nom complet de l'investisseur", example = "John Doe")
        String investisseurNom,

        @Schema(description = "Identifiant du projet financé", example = "7")
        Long projetId,

        @Schema(description = "Libellé du projet financé", example = "Ferme solaire Bobo-Dioulasso")
        String projetLibelle,

        @Schema(description = "Prix unitaire d'une part au moment de l'investissement", type = "number", format = "double", example = "500.00")
        BigDecimal prixUnePart,

        @Schema(description = "Latitude géographique du projet", type = "number", format = "double", example = "11.1771")
        BigDecimal latitude,

        @Schema(description = "Longitude géographique du projet", type = "number", format = "double", example = "-4.2979")
        BigDecimal longitude,

        @Schema(description = "Lien Google Maps vers la localisation du projet", example = "https://maps.google.com/?q=11.1771,-4.2979")
        String googleMapsUrl,

        @Schema(description = "Montant total investi (parts × prix unitaire)", type = "number", format = "double", example = "2500.00")
        BigDecimal montantInvesti,

        @Schema(description = "URL de l'affiche du projet", example = "/uploads/projets/ferme-solaire.jpg")
        String projetPoster,

        @Schema(description = "URL du contrat d'investissement signé (PDF)", example = "/uploads/contrats/CONTRAT-2025-00015.pdf")
        String contratUrl,

        @Schema(description = "Numéro unique du contrat d'investissement", example = "CONTRAT-2025-00015")
        String numeroContrat,

        @Schema(description = "Liste des dividendes associés à cet investissement")
        List<DividendeDTO> dividendes,

        @Schema(description = "Montant total des dividendes déjà perçus", type = "number", format = "double", example = "375.00")
        BigDecimal montantTotalPercu,

        @Schema(description = "Montant total des dividendes planifiés sur la durée du projet", type = "number", format = "double", example = "750.00")
        BigDecimal montantTotalPlanifie,

        @Schema(description = "Retour sur investissement réalisé en pourcentage", type = "number", format = "double", example = "15.0")
        double roiRealise,

        @Schema(description = "Nombre de dividendes déjà versés", example = "3")
        int dividendesPayes,

        @Schema(description = "Nombre total de dividendes planifiés", example = "6")
        int dividendesPlanifies,

        @Schema(description = "Statut global des dividendes (ex: EN_COURS, TERMINE, PLANIFIE)", example = "EN_COURS")
        String statutGlobalDividendes
) {
}
