package growzapp.backend.config.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Chiffre/déchiffre automatiquement un champ LocalDate — délègue le vrai
 * chiffrement AES à EncryptedStringConverter (réutilisation, pas de
 * duplication de la logique cryptographique). Utilisé sur dateNaissance
 * (MED-04, extension au-delà du numéro de pièce seul).
 */
@Converter
@Component
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    @Autowired
    private EncryptedStringConverter stringConverter;

    @Override
    public String convertToDatabaseColumn(LocalDate date) {
        if (date == null) {
            return null;
        }
        String isoDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        return stringConverter.convertToDatabaseColumn(isoDate);
    }

    @Override
    public LocalDate convertToEntityAttribute(String storedValue) {
        if (storedValue == null || storedValue.isBlank()) {
            return null;
        }
        String isoDate = stringConverter.convertToEntityAttribute(storedValue);
        return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
    }
}