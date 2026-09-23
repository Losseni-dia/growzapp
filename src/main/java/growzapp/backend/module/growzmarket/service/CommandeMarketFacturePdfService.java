package growzapp.backend.module.growzmarket.service;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextFontResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import growzapp.backend.module.growzmarket.model.CommandeMarket;
import lombok.extern.slf4j.Slf4j;

// Génère automatiquement la facture PDF d'un achat GrowzMarket, à la
// confirmation du retrait — même principe que
// growzapp.backend.module.fournisseur.service.CommandeFacturePdfService.
@Slf4j
@Service
public class CommandeMarketFacturePdfService {

    private final TemplateEngine templateEngine;

    public CommandeMarketFacturePdfService(@Qualifier("pdfTemplateEngine") TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] generateFacture(CommandeMarket commande) {
        try {
            Context context = new Context(Locale.FRENCH);
            context.setVariable("commande", commande);
            context.setVariable("projet", commande.getProjet());
            context.setVariable("acheteur", commande.getAcheteur());
            context.setVariable("lignes", commande.getLignes());

            String htmlContent = templateEngine.process("facture/pdf-template-market", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            try {
                ITextFontResolver fontResolver = renderer.getFontResolver();
                ClassPathResource fontResource = new ClassPathResource("fonts/NotoSans-Regular.ttf");
                if (fontResource.exists()) {
                    fontResolver.addFont(fontResource.getURL().toExternalForm(), true);
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
        } catch (Exception e) {
            throw new RuntimeException("Échec de la génération de la facture PDF pour la commande GrowzMarket "
                    + commande.getId(), e);
        }
    }
}
