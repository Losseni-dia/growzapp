package growzapp.backend.module.kyc.dto;

import lombok.Data;
import java.util.List;

@Data
public class VoveIdResultDTO {
    private String id;
    private String refId;
    private String status; // "successful", "pending", "failed"
    private String createdAt;
    private String updatedAt;
    private String flowId;
    private String sessionId;
    private List<VoveIdDocumentDTO> documents;
}