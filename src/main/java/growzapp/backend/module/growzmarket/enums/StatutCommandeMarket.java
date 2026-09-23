package growzapp.backend.module.growzmarket.enums;

public enum StatutCommandeMarket {
    // Payée immédiatement à la commande — aucune validation admin
    // intermédiaire, contrairement au module Fournisseur : c'est une vente
    // directe, pas un achat déclenché après approbation.
    PAYEE,
    // Le porteur a préparé la commande, prête à être récupérée au point de
    // retrait déclaré sur l'article.
    PRETE_AU_RETRAIT,
    // L'acheteur a confirmé avoir récupéré sa commande.
    RETIREE,
    // Non récupérée après le délai (voir CommandeMarketService) — l'admin
    // doit arbitrer.
    NON_RETIREE,
    // Litige ouvert par l'acheteur ou le porteur avant retrait confirmé.
    LITIGE,
    // Résolution : commande annulée, acheteur remboursé.
    ANNULEE
}
