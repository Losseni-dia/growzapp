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
import growzapp.backend.module.exchangerate.service.CurrencyService;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

@Slf4j
@Service
public class FacturePdfService {

    private final TemplateEngine templateEngine;
    private final CurrencyService currencyService;

    public FacturePdfService(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine,
            CurrencyService currencyService) {
        this.templateEngine = templateEngine;
        this.currencyService = currencyService;
    }

    public byte[] generateDividendeFacture(Dividende dividende, byte[] barcodePng, Locale locale)
            throws IOException, com.lowagie.text.DocumentException {
        return generateDividendeFacture(dividende, barcodePng, locale, "XOF");
    }

    public byte[] generateDividendeFacture(Dividende dividende, byte[] barcodePng, Locale locale, String devise)
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

        java.math.BigDecimal montantTTC = java.math.BigDecimal.valueOf(dividende.getFacture().getMontantTTC());
        java.math.BigDecimal montantHT = java.math.BigDecimal.valueOf(dividende.getFacture().getMontantHT());
        java.math.BigDecimal montantParPart = dividende.getMontantParPart();

        context.setVariable("deviseLabel", currencyService.getLabel(devise));
        context.setVariable("montantTTCValue", currencyService.formatAmountValue(montantTTC, devise));
        context.setVariable("montantHTFormatted", currencyService.format(montantHT, devise));
        context.setVariable("montantParPartFormatted", currencyService.format(montantParPart, devise));

        if (barcodePng != null) {
            String barcodeBase64 = Base64.getEncoder().encodeToString(barcodePng);
            context.setVariable("barcodeBase64", barcodeBase64);
        }

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

    public byte[] generateDividendeFacture(Dividende dividende, byte[] barcodePng)
            throws IOException, com.lowagie.text.DocumentException {
        return generateDividendeFacture(dividende, barcodePng, Locale.FRENCH);
    }

    public byte[] generatePremiumFacture(Facture facture, Projet projet, User porteur, String sourcePaiement,
            byte[] barcodePng) throws IOException, com.lowagie.text.DocumentException {
        return generatePremiumFacture(facture, projet, porteur, sourcePaiement, barcodePng, Locale.FRENCH, "XOF");
    }

    public byte[] generatePremiumFacture(Facture facture, Projet projet, User porteur, String sourcePaiement,
            byte[] barcodePng, Locale locale) throws IOException, com.lowagie.text.DocumentException {
        return generatePremiumFacture(facture, projet, porteur, sourcePaiement, barcodePng, locale, "XOF");
    }

    public byte[] generatePremiumFacture(Facture facture, Projet projet, User porteur, String sourcePaiement,
            byte[] barcodePng, Locale locale, String devise) throws IOException, com.lowagie.text.DocumentException {

        Context context = new Context(locale);
        context.setVariable("facture", facture);
        context.setVariable("projet", projet);
        context.setVariable("porteur", porteur);
        context.setVariable("sourcePaiement", sourcePaiement);

        java.math.BigDecimal montantTTC = java.math.BigDecimal.valueOf(facture.getMontantTTC());
        java.math.BigDecimal montantHT = java.math.BigDecimal.valueOf(facture.getMontantHT());

        context.setVariable("deviseLabel", currencyService.getLabel(devise));
        context.setVariable("montantTTCValue", currencyService.formatAmountValue(montantTTC, devise));
        context.setVariable("montantHTFormatted", currencyService.format(montantHT, devise));

        if (barcodePng != null) {
            context.setVariable("barcodeBase64", Base64.getEncoder().encodeToString(barcodePng));
        }

        String htmlContent = templateEngine.process("facture/pdf-template-premium", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        try {
            ITextFontResolver fontResolver = renderer.getFontResolver();
            ClassPathResource fontResource = new ClassPathResource("fonts/NotoSans-Regular.ttf");
            if (fontResource.exists()) {
                fontResolver.addFont(fontResource.getURL().toExternalForm(), true);
            }
        } catch (Exception e) {
            log.warn("Erreur chargement police : {}", e.getMessage());
        }

        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream);

        return outputStream.toByteArray();
    }
}