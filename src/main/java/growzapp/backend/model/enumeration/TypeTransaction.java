package growzapp.backend.model.enumeration;

public enum TypeTransaction {
    DEPOT,
    RETRAIT,
    INVESTISSEMENT,
    REMBOURSEMENT,
    TRANSFER_OUT, // Débit lors d'un transfert vers un autre user
    TRANSFER_IN // Crédit lors d'un transfert reçu
}