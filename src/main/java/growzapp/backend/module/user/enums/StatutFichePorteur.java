package growzapp.backend.module.user.enums;

public enum StatutFichePorteur {
    NON_SOUMISE, // Le porteur n'a pas encore rempli sa fiche de présentation
    EN_ATTENTE, // Fiche soumise, en attente de validation admin
    VALIDEE, // Fiche approuvée — le porteur peut soumettre des projets
    REJETEE // Fiche rejetée (infos insuffisantes, incohérentes...)
}
