// src/main/java/growzapp/backend/model/dto/dividendeDTO/DividendeHistoriqueAdminDTO.java

package growzapp.backend.model.dto.dividendeDTO;

public record DividendeHistoriqueAdminDTO(
        Long id,
        Double montantTotal,
        String datePaiement, // formatée ou ISO
        String motif,
        String investisseurNom, // ← nouveau
        String factureUrl // ← nouveau (ou null si pas générée)
) {
}