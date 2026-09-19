-- Le secteur d'activité d'un projet est un champ libre (le porteur peut
-- taper n'importe quel nom, un nouveau Secteur est créé si besoin) — il ne
-- peut donc pas être couvert par le dictionnaire i18n statique utilisé pour
-- les valeurs de référence connues. On le fait passer par le même pipeline
-- DeepL que le libellé/la description du projet.
ALTER TABLE projet_traductions ADD COLUMN secteur_nom VARCHAR(150);
