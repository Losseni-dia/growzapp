package growzapp.backend.module.kyc.dto;

import lombok.Data;

@Data
public class VoveIdDocumentDTO {
    private String stepId;
    private String type;
    private String firstName;
    private String lastName;
    private String dateOfExpiration;
    private String dateOfIssue;
    private String dateOfBirth;
    private String idNumber;
    private String sex;
    private String digitalIDSpoof;
    private String faceOnDocumentSpoof;
    private String textOnDocumentSpoof;
}