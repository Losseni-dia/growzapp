package growzapp.backend.module.wallet.enums;

public enum TypeTransaction {
    DEPOT,
    RETRAIT,
    TRANSFER_OUT,
    TRANSFER_IN,
    INVESTISSEMENT,
    PAIEMENT_STRIPE,
    PAIEMENT_OM,
    PAIEMENT_MTN,
    PAIEMENT_WAVE,
    REMBOURSEMENT,

    // NOUVEAU : RETRAITS RÉELS
    PAYOUT_OM,
    PAYOUT_MTN,
    PAYOUT_WAVE,
    PAYOUT_OM_SN,
    PAYOUT_WAVE_SN,
    PAYOUT_MOOV,
    PAYOUT_STRIPE,
    PAYOUT_BANK,


    CREDIT_PROJET,
    VIREMENT_PORTEUR,
    RETRAIT_MOBILE_MONEY,

    VERSEMENT_PORTEUR,
    VERSEMENT_DIVIDENDE, DIVIDENDE_ENTRANT, DIVIDENDE_SORTANT, DIVIDENDE, RETRAIT_ADMIN,

    // Refonte wallet projet (déblocage de trésorerie séquestrée)
    DEBLOCAGE_PROJET,
    TRANSFER_PROJET_VERS_PERSONNEL,
    RETRAIT_PROJET,

    // Achat du statut Premium (mise en avant catalogue)
    PREMIUM_PROJET,

    // Commande fournisseur : paiement direct wallet projet -> wallet
    // fournisseur, sans jamais transiter par le porteur.
    PAIEMENT_FOURNISSEUR,

    // GrowzMarket : achat grand public d'un produit/service vendu par un
    // porteur — débite le wallet personnel de l'acheteur, crédite le
    // soldeBloque du wallet du projet vendeur (même gouvernance que l'argent
    // des investisseurs, débloqué ensuite par l'admin).
    VENTE_MARKET
}