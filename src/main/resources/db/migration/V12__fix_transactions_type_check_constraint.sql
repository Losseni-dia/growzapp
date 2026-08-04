-- transactions_type_check n'a jamais été élargie quand DEBLOCAGE_PROJET,
-- TRANSFER_PROJET_VERS_PERSONNEL et RETRAIT_PROJET ont été ajoutées à
-- l'enum Java TypeTransaction (V11, refonte wallet deux soldes) : toute
-- tentative d'INSERT avec l'une de ces valeurs échoue en violation de
-- contrainte CHECK, cassant le déblocage admin de trésorerie projet ainsi
-- que le retrait/transfert self-service du porteur.
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
        'DEBLOCAGE_PROJET', 'TRANSFER_PROJET_VERS_PERSONNEL', 'RETRAIT_PROJET'
    ]::character varying[])
);
