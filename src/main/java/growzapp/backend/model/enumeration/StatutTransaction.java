package growzapp.backend.model.enumeration;

public enum StatutTransaction {
    EN_COURS,
    SUCCESS,
    FAILED,
    EN_ATTENTE_VALIDATION, // ex: retrait en attente d'approbation admin
    REJETEE
}