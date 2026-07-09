package growzapp.backend.module.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoveIdSessionDTO {
    private String refId;
    private String widgetUrl;
    private String publicKey;
}