-- Remplace la fonctionnalité "certification" (jamais réellement câblée : le
-- champ certified_at n'était renseigné nulle part côté application) par un
-- vrai statut Premium payant, avec période d'activation.

ALTER TABLE projets DROP COLUMN certified_at;

ALTER TABLE projets ADD COLUMN premium_debut TIMESTAMP NULL;
ALTER TABLE projets ADD COLUMN premium_fin TIMESTAMP NULL;

-- Nouveau type de transaction pour l'achat du statut Premium.
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
        'PREMIUM_PROJET'
    ]::character varying[])
);
