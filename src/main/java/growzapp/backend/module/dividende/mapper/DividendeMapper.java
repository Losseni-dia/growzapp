package growzapp.backend.module.dividende.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import growzapp.backend.module.dividende.dto.DividendeDTO;
import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.facture.mapper.FactureMapper;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = { FactureMapper.class })
public abstract class DividendeMapper {

    @Autowired
    protected FactureMapper factureMapper;

    /**
     * 1. POINT D'ENTRÉE PRINCIPAL (Orchestration Métier)
     * Cette méthode intercepte le flux pour exécuter tes calculs personnalisés
     * avant de confier l'instanciation du Record à MapStruct.
     */
    public DividendeDTO toDto(Dividende dividende) {
        if (dividende == null)
            return null;

        // Valeurs par défaut et initialisation
        String investissementInfo = "Investissement inconnu";
        BigDecimal montantTotal = BigDecimal.ZERO;
        Long investissementId = null;
        String factureUrl = null;
        String fileName = null;
        FactureDTO factureDto = null;

        // Traitement de l'investissement rattaché
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

            if (dividende.getMontantParPart() != null) {
                montantTotal = dividende.getMontantParPart()
                        .multiply(BigDecimal.valueOf(inv.getNombrePartsPris()));
            }
        }

        // Traitement de la facture et découpage sécurisé de l'URL du fichier PDF
        if (dividende.getFacture() != null) {
            factureDto = factureMapper.toFactureDto(dividende.getFacture());
            if (factureDto != null && factureDto.fichierUrl() != null) {
                factureUrl = factureDto.fichierUrl();
                if (factureUrl.contains("/")) {
                    fileName = factureUrl.substring(factureUrl.lastIndexOf("/") + 1);
                } else {
                    fileName = factureUrl;
                }
            }
        }

        // Conversion sécurisée de la date de paiement
        LocalDate datePaiementLocal = dividende.getDatePaiement() != null
                ? dividende.getDatePaiement().toLocalDate()
                : null;

        BigDecimal mParPart = dividende.getMontantParPart() != null ? dividende.getMontantParPart() : BigDecimal.ZERO;

        // Envoi des variables calculées au constructeur généré
        return toDtoInternal(
                dividende,
                mParPart,
                datePaiementLocal,
                investissementId,
                investissementInfo,
                montantTotal,
                fileName,
                factureUrl,
                factureDto);
    }

    /**
     * 2. LE MOTEUR GÉNÉRÉ
     * MapStruct va implémenter cette méthode automatiquement en mappant le reste
     * des structures.
     */
    @Mapping(target = "id", source = "d.id")
    @Mapping(target = "montantParPart", source = "montantParPart")
    @Mapping(target = "statutDividende", source = "d.statutDividende")
    @Mapping(target = "moyenPaiement", source = "d.moyenPaiement")
    @Mapping(target = "datePaiement", source = "datePaiement")
    @Mapping(target = "investissementId", source = "investissementId")
    @Mapping(target = "investissementInfo", source = "investissementInfo")
    @Mapping(target = "montantTotal", source = "montantTotal")
    @Mapping(target = "fileName", source = "fileName")
    @Mapping(target = "factureUrl", source = "factureUrl")
    @Mapping(target = "facture", source = "factureDto")
    @Mapping(target = "motif", source = "d.motif")
    protected abstract DividendeDTO toDtoInternal(
            Dividende d,
            BigDecimal montantParPart,
            LocalDate datePaiement,
            Long investissementId,
            String investissementInfo,
            BigDecimal montantTotal,
            String fileName,
            String factureUrl,
            FactureDTO factureDto);

    /**
     * 3. MAPPING INVERSE : DTO -> ENTITY
     */
    @Mapping(target = "investissement", ignore = true)
    @Mapping(target = "facture", ignore = true)
    @Mapping(target = "datePaiement", expression = "java(dto.datePaiement() != null ? dto.datePaiement().atStartOfDay() : null)")
    @Mapping(target = "montantParPart", expression = "java(dto.montantParPart() != null ? dto.montantParPart() : java.math.BigDecimal.ZERO)")
    @Mapping(target = "statutDividende", expression = "java(dto.statutDividende() != null ? dto.statutDividende() : growzapp.backend.module.dividende.enums.StatutDividende.PLANIFIE)")
    public abstract Dividende toEntity(DividendeDTO dto);
}