package growzapp.backend.module.kyc.service;

import growzapp.backend.module.kyc.dto.VoveIdDocumentDTO;
import growzapp.backend.module.kyc.dto.VoveIdResultDTO;
import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class KycVoveIdService {

    private final UserRepository userRepository;

    @Transactional
    public void updateKycStatusFromVoveId(Long userId, VoveIdResultDTO result) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        String status = result.getStatus();
        System.out.println("Traitement KYC — userId: " + userId
                + " — status: " + status);

        switch (status) {

            case "successful" -> {
                user.setKycStatus(KycStatus.VALIDE);
                user.setKycDateValidation(LocalDateTime.now());

                // Extraire les données du premier document
                if (result.getDocuments() != null
                        && !result.getDocuments().isEmpty()) {
                    VoveIdDocumentDTO doc = result.getDocuments().get(0);
                    user.setKycNumeroPiece(doc.getIdNumber());

                    if (doc.getDateOfExpiration() != null) {
                        try {
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                            user.setKycDateExpiration(
                                    LocalDate.parse(doc.getDateOfExpiration(), formatter));
                        } catch (Exception e) {
                            System.err.println("Erreur parsing date: "
                                    + doc.getDateOfExpiration());
                        }
                    }
                }
                System.out.println("KYC VALIDE pour user: " + userId);
            }

            case "failed" -> {
                user.setKycStatus(KycStatus.REJETE);
                user.setKycCommentaireRejet("Verification refusee par VOVE ID");
                System.out.println("KYC REJETE pour user: " + userId);
            }

            default -> {
                user.setKycStatus(KycStatus.EN_ATTENTE);
                System.out.println("KYC EN_ATTENTE pour user: " + userId);
            }
        }

        userRepository.save(user);
    }
}