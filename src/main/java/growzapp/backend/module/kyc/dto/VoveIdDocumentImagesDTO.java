package growzapp.backend.module.kyc.dto;

import lombok.Data;
import java.util.List;

/**
 * Réponse de GET /v2/users/{refId}/documents — endpoint séparé de VOVE ID
 * qui renvoie les images du dossier (base64), à ne consulter qu'à la
 * demande et sans jamais logger le contenu (recommandation VOVE ID).
 */
@Data
public class VoveIdDocumentImagesDTO {
    private String selfie; // base64
    private List<Item> documents;

    @Data
    public static class Item {
        private String stepId;
        private String type;
        private String front; // base64
        private String back; // base64
        private String photo; // base64
        private String frontCropped; // base64
        private String backCropped; // base64
    }
}
