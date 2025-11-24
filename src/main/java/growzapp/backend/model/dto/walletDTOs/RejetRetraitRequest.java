package growzapp.backend.model.dto.walletDTOs;

public record RejetRetraitRequest(String motif) {
    public RejetRetraitRequest {
        if (motif == null || motif.trim().isBlank())
        throw new IllegalArgumentException("Le motif de rejet est obligatoire");}
}