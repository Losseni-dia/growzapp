package growzapp.backend.service;

import growzapp.backend.model.entite.Dividende;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

@Slf4j
@Service
public class FacturePdfService {

    private final TemplateEngine templateEngine;

    // On injecte le moteur configuré spécifiquement pour les PDF (avec
    // messages.properties)
    public FacturePdfService(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // ========================================================================
    // MÉTHODE PRINCIPALE (AVEC LANGUE)
    // ========================================================================
    public byte[] generateDividendeFacture(Dividende dividende, Locale locale)
            throws IOException, com.lowagie.text.DocumentException {
        // 1. Vérifications de sécurité
        if (dividende.getInvestissement() == null) {
            throw new IllegalStateException("Investissement manquant pour générer la facture");
        }
        if (dividende.getFacture() == null) {
            throw new IllegalStateException("Facture non liée au dividende (est-elle sauvegardée ?)");
        }

        // 2. Préparation du contexte Thymeleaf AVEC LA LANGUE
        Context context = new Context(locale); // <--- C'est ici que la magie opère

        context.setVariable("dividende", dividende);
        context.setVariable("investissement", dividende.getInvestissement());
        context.setVariable("facture", dividende.getFacture());
        context.setVariable("investisseur", dividende.getInvestissement().getInvestisseur());

        // 3. Génération du HTML
        // Thymeleaf va chercher les traductions dans messages_en.properties si
        // locale=en, etc.
        String htmlContent = templateEngine.process("facture/pdf-template", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        // 4. Gestion de la Police (Sécurisée pour les accents)
        try {
            ITextFontResolver fontResolver = renderer.getFontResolver();
            ClassPathResource fontResource = new ClassPathResource("fonts/NotoSans-Regular.ttf");

            if (fontResource.exists()) {
                String fontPath = fontResource.getURL().toExternalForm();
                fontResolver.addFont(fontPath, true);
            } else {
                log.warn("⚠️ Police 'NotoSans-Regular.ttf' introuvable. Les accents risquent de ne pas s'afficher.");
            }
        } catch (Exception e) {
            log.warn("Erreur chargement police : {}", e.getMessage());
        }

        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    // ========================================================================
    // MÉTHODE DE COMPATIBILITÉ (PAR DÉFAUT = FR)
    // ========================================================================
    // Utilisée par le processus asynchrone qui génère la facture initiale lors du
    // paiement
    public byte[] generateDividendeFacture(Dividende dividende) throws IOException, com.lowagie.text.DocumentException {
        return generateDividendeFacture(dividende, Locale.FRENCH);
    }
}