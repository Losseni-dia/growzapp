package growzapp.backend.config.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffre/déchiffre automatiquement un champ String au moment de l'écriture
 * et de la lecture en base — utilisé sur les données KYC sensibles
 * (MED-04 de l'audit). AES-256-GCM, clé dérivée d'une variable
 * d'environnement, jamais codée en dur.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Value("${growzapp.security.kyc-encryption-key}")
    private String encryptionKeyBase64;

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private SecretKeySpec getKey() {
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        return new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), spec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // Concatène IV + données chiffrées, encodé en Base64 pour stockage texte
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Erreur chiffrement champ KYC", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return storedValue;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Erreur déchiffrement champ KYC", e);
        }
    }
}