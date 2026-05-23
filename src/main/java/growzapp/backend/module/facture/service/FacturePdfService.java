package growzapp.backend.module.facture.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import growzapp.backend.module.dividende.model.Dividende;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

@Slf4j
@Service
public class FacturePdfService {

    private final TemplateEngine templateEngine;

    public FacturePdfService(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateDividendeFacture(Dividende dividende, Locale locale)
            throws IOException, com.lowagie.text.DocumentException {

        if (dividende.getInvestissement() == null) {
            throw new IllegalStateException("Investissement manquant pour générer la facture");
        }
        if (dividende.getFacture() == null) {
            throw new IllegalStateException("Facture non liée au dividende (est-elle sauvegardée ?)");
        }

        Context context = new Context(locale);
        context.setVariable("dividende", dividende);
        context.setVariable("investissement", dividende.getInvestissement());
        context.setVariable("facture", dividende.getFacture());
        context.setVariable("investisseur", dividende.getInvestissement().getInvestisseur());

        String htmlContent = templateEngine.process("facture/pdf-template", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        try {
            ITextFontResolver fontResolver = renderer.getFontResolver();
            ClassPathResource fontResource = new ClassPathResource("fonts/NotoSans-Regular.ttf");
            if (fontResource.exists()) {
                String fontPath = fontResource.getURL().toExternalForm();
                fontResolver.addFont(fontPath, true);
            } else {
                log.warn("Police 'NotoSans-Regular.ttf' introuvable.");
            }
        } catch (Exception e) {
            log.warn("Erreur chargement police : {}", e.getMessage());
        }

        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }

    public byte[] generateDividendeFacture(Dividende dividende)
            throws IOException, com.lowagie.text.DocumentException {
        return generateDividendeFacture(dividende, Locale.FRENCH);
    }
}
