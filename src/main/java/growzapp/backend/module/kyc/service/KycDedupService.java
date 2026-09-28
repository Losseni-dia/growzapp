package growzapp.backend.module.kyc.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import growzapp.backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * Détecte qu'un même numéro de pièce d'identité sert à créer plusieurs
 * comptes — que ce soit via VOVE ID ou via la soumission manuelle, les deux
 * flux alimentent le même hash et sont donc vérifiés ensemble.
 *
 * kyc_numero_piece (chiffré AES-GCM, IV aléatoire) ne permet aucune
 * comparaison directe en SQL : une même valeur produit un texte chiffré
 * différent à chaque écriture. On calcule donc en parallèle un HMAC-SHA256
 * déterministe du numéro normalisé, stocké en clair (c'est un hash à sens
 * unique, pas le document lui-même) et indexé pour permettre la recherche
 * de doublon.
 */
@Service
@RequiredArgsConstructor
public class KycDedupService {

    private final UserRepository userRepository;

    @Value("${growzapp.security.kyc-encryption-key}")
    private String hashKeyBase64;

    // Normalise avant hash pour attraper les variantes triviales
    // (espaces, tirets, casse) qui désigneraient le même document réel.
    private String normalize(String numeroPiece) {
        return numeroPiece.trim().toUpperCase(Locale.ROOT).replaceAll("[\\s\\-]", "");
    }

    public String hash(String numeroPiece) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(hashKeyBase64);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            byte[] result = mac.doFinal(normalize(numeroPiece).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Erreur calcul hash KYC", e);
        }
    }

    /**
     * Lève une IllegalArgumentException (400, message clair pour
     * l'utilisateur) si ce numéro de pièce est déjà associé à un autre
     * compte que currentUserId.
     */
    public String verifierPasDeDoublon(String numeroPiece, Long currentUserId) {
        String hash = hash(numeroPiece);
        userRepository.findFirstByKycNumeroPieceHashAndIdNot(hash, currentUserId)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(
                            "Ce numéro de pièce d'identité est déjà associé à un autre compte GrowzApp.");
                });
        return hash;
    }
}
