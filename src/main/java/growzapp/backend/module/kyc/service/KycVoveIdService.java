package growzapp.backend.module.kyc.service;

import growzapp.backend.module.kyc.dto.VoveIdDocumentDTO;
import growzapp.backend.module.kyc.dto.VoveIdDocumentImagesDTO;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class KycVoveIdService {

    private final UserRepository userRepository;
    private final VoveIdService voveIdService;
    private final KycStorageService kycStorageService;

    // VOVE ID ne documente pas un format de date unique et stable dans ses
    // réponses — "dd/MM/yyyy" seul faisait échouer silencieusement le parsing
    // (exception attrapée, date jamais enregistrée) dès que le format réel
    // différait (ex. ISO "yyyy-MM-dd"). On tente les formats plausibles dans
    // l'ordre avant d'abandonner.
    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"));

    private LocalDate parseVoveIdDate(String raw) {
        for (DateTimeFormatter formatter : DATE_FORMATS) {
            try {
                return LocalDate.parse(raw, formatter);
            } catch (Exception ignored) {
                // essaie le format suivant
            }
        }
        System.err.println("Erreur parsing date VOVE ID (aucun format reconnu) : " + raw);
        return null;
    }

    // Archive les images du dossier (recto/verso/selfie) dans notre propre
    // stockage — VOVE ID les renvoie en base64 via un endpoint séparé.
    // L'utilisateur/l'admin doit pouvoir les consulter en cas de litige,
    // comme pour un dossier soumis manuellement — sans ça, une vérification
    // automatique ne laissait aucune trace consultable de son côté.
    // Ne s'exécute qu'une fois (si le selfie n'est pas déjà archivé), pour
    // ne pas rappeler VOVE ID à chaque rafraîchissement de statut.
    private void archiverImagesVoveId(User user, String refId) {
        if (user.getKycSelfieUrl() != null || refId == null) {
            return;
        }
        try {
            VoveIdDocumentImagesDTO images = voveIdService.getVerificationDocuments(refId);
            if (images == null) {
                return;
            }
            if (images.getSelfie() != null && !images.getSelfie().isBlank()) {
                user.setKycSelfieUrl(kycStorageService.saveBase64(images.getSelfie(), ".jpg"));
            }
            if (images.getDocuments() != null && !images.getDocuments().isEmpty()) {
                VoveIdDocumentImagesDTO.Item doc = images.getDocuments().get(0);
                if (doc.getFront() != null && !doc.getFront().isBlank()) {
                    user.setKycRectoUrl(kycStorageService.saveBase64(doc.getFront(), ".jpg"));
                }
                if (doc.getBack() != null && !doc.getBack().isBlank()) {
                    user.setKycVersoUrl(kycStorageService.saveBase64(doc.getBack(), ".jpg"));
                }
            }
        } catch (Exception e) {
            // Ne bloque jamais la validation KYC elle-même si l'archivage
            // échoue — l'important est le statut, les images sont un plus.
            System.err.println("Erreur archivage images VOVE ID (refId=" + refId + ") : " + e.getMessage());
        }
    }

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
                        LocalDate expiration = parseVoveIdDate(doc.getDateOfExpiration());
                        if (expiration != null) {
                            user.setKycDateExpiration(expiration);
                        }
                    }
                }

                archiverImagesVoveId(user, result.getRefId());
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