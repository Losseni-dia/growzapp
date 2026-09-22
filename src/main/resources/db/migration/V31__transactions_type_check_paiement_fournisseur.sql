-- Même piège que V12/V22 : PAIEMENT_FOURNISSEUR a été ajouté à l'enum Java
-- TypeTransaction (module fournisseur) sans jamais élargir la contrainte
-- CHECK correspondante en base — toute validation admin d'une commande
-- fournisseur échouait en violation de contrainte (500) dès l'INSERT de la
-- transaction de séquestre.
ALTER TABLE transactions DROP CONSTRAINT transactions_type_check;

ALTER TABLE transactions ADD CONSTRAINT transactions_type_check CHECK (
    (type)::text = ANY (ARRAY[
        'DEPOT', 'RETRAIT', 'TRANSFER_OUT', 'TRANSFER_IN', 'INVESTISSEMENT',
        'PAIEMENT_STRIPE', 'PAIEMENT_OM', 'PAIEMENT_MTN', 'PAIEMENT_WAVE',
        'REMBOURSEMENT', 'PAYOUT_OM', 'PAYOUT_MTN', 'PAYOUT_WAVE',
        'PAYOUT_OM_SN', 'PAYOUT_WAVE_SN', 'PAYOUT_MOOV', 'PAYOUT_STRIPE',
        'PAYOUT_BANK', 'CREDIT_PROJET', 'VIREMENT_PORTEUR',
        'RETRAIT_MOBILE_MONEY', 'VERSEMENT_PORTEUR', 'VERSEMENT_DIVIDENDE',
        'DIVIDENDE_ENTRANT', 'DIVIDENDE_SORTANT', 'DIVIDENDE', 'RETRAIT_ADMIN',
        'DEBLOCAGE_PROJET', 'TRANSFER_PROJET_VERS_PERSONNEL', 'RETRAIT_PROJET',
        'PREMIUM_PROJET', 'PAIEMENT_FOURNISSEUR'
    ]::character varying[])
);
