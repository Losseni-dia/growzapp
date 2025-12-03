package growzapp.backend.model.dto.contratDTO;

// src/main/java/growzapp/backend/dto/ContratPublicDTO.java
public record ContratPublicDTO(
        boolean valide,
        String numeroContrat,
        String projet,
        String investisseur,
        double montant,
        String date) {
}