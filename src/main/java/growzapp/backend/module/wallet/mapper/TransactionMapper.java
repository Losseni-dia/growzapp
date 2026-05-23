package growzapp.backend.module.wallet.mapper;

import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.dto.TransactionDTO;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class TransactionMapper {

    @Autowired
    protected UserRepository userRepository;

    /**
     * 1. POINT D'ENTRÉE PRINCIPAL (Orchestration Métier)
     * Cette méthode intercepte le mapping pour exécuter les requêtes DB et la
     * logique conditionnelle.
     */
    public TransactionDTO toDto(Transaction transaction) {
        if (transaction == null)
            return null;

        // --- Étape A : Récupération des informations du propriétaire du Wallet ---
        Long ownerId = null;
        String ownerPrenom = "Utilisateur";
        String ownerNom = "supprimé";
        String ownerLogin = "inconnu";

        if (transaction.getWalletType() == WalletType.USER && transaction.getWalletId() != null) {
            Optional<Map<String, Object>> info = userRepository.findBasicInfoByWalletId(transaction.getWalletId());
            if (info.isPresent()) {
                Map<String, Object> map = info.get();
                ownerId = (Long) map.get("id");
                ownerPrenom = (String) map.get("prenom");
                ownerNom = (String) map.get("nom");
                ownerLogin = (String) map.get("login");
            }
        }

        // --- Étape B : Extraction et routage du destinataire / expéditeur ---
        Long destinataireUserId = null;
        String destinataireNomComplet = null;
        String destinataireLogin = null;

        Long expediteurUserId = null;
        String expediteurNomComplet = null;
        String expediteurLogin = null;

        if (transaction.getDestinataireWallet() != null && transaction.getDestinataireWallet().getUser() != null) {
            User counterpart = transaction.getDestinataireWallet().getUser();
            String fullName = counterpart.getPrenom() + " " + counterpart.getNom();

            if (transaction.getType() == TypeTransaction.TRANSFER_OUT) {
                destinataireUserId = counterpart.getId();
                destinataireNomComplet = fullName;
                destinataireLogin = counterpart.getLogin(); // Corrigé : On ne passe plus null
            } else if (transaction.getType() == TypeTransaction.TRANSFER_IN) {
                expediteurUserId = counterpart.getId();
                expediteurNomComplet = fullName;
                expediteurLogin = counterpart.getLogin(); // Corrigé : Plus robuste
            }
        }

        // --- Étape C : Envoi de toutes les données calculées au constructeur généré
        // par MapStruct ---
        return toDtoInternal(
                transaction,
                ownerId, ownerPrenom, ownerNom, ownerLogin,
                destinataireUserId, destinataireNomComplet, destinataireLogin,
                expediteurUserId, expediteurNomComplet, expediteurLogin);
    }

    /**
     * 2. LE MOTEUR GENERÉ (Génération automatique des correspondances)
     * MapStruct va implémenter cette méthode en liant l'entité et les variables
     * fournies au Record.
     */
    @Mapping(target = "id", source = "t.id")
    @Mapping(target = "montant", source = "t.montant")
    @Mapping(target = "type", source = "t.type")
    @Mapping(target = "statut", source = "t.statut")
    @Mapping(target = "createdAt", source = "t.createdAt")
    @Mapping(target = "completedAt", source = "t.completedAt")
    @Mapping(target = "description", source = "t.description")
    // Mapping des paramètres contextuels du propriétaire
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "userPrenom", source = "userPrenom")
    @Mapping(target = "userNom", source = "userNom")
    @Mapping(target = "userLogin", source = "userLogin")
    // Mapping des paramètres contextuels de la contrepartie (In/Out)
    @Mapping(target = "destinataireUserId", source = "destinataireUserId")
    @Mapping(target = "destinataireNomComplet", source = "destinataireNomComplet")
    @Mapping(target = "destinataireLogin", source = "destinataireLogin")
    @Mapping(target = "expediteurUserId", source = "expediteurUserId")
    @Mapping(target = "expediteurNomComplet", source = "expediteurNomComplet")
    @Mapping(target = "expediteurLogin", source = "expediteurLogin")
    protected abstract TransactionDTO toDtoInternal(
            Transaction t,
            Long userId, String userPrenom, String userNom, String userLogin,
            Long destinataireUserId, String destinataireNomComplet, String destinataireLogin,
            Long expediteurUserId, String expediteurNomComplet, String expediteurLogin);

    /**
     * 3. MAPPING INVERSE : DTO -> ENTITY
     */
    @Mapping(target = "destinataireWallet", ignore = true)
    @Mapping(target = "walletId", ignore = true)
    @Mapping(target = "walletType", ignore = true)
    @Mapping(target = "referenceType", ignore = true)
    @Mapping(target = "referenceId", ignore = true)
    @Mapping(target = "referenceExterne", ignore = true)
    public abstract Transaction toEntity(TransactionDTO dto);
}