package growzapp.backend.service;

import growzapp.backend.model.entite.Dividende;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.ITextFontResolver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class FacturePdfService {

    private final TemplateEngine templateEngine;

    public byte[] generateDividendeFacture(Dividende dividende) throws IOException, com.lowagie.text.DocumentException {
        if (dividende.getInvestissement() == null) {
            throw new IllegalStateException("Investissement manquant pour générer la facture");
        }

        Context context = new Context();
        context.setVariable("dividende", dividende);
        context.setVariable("investissement", dividende.getInvestissement());

        String htmlContent = templateEngine.process("facture/pdf-template", context);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        ITextRenderer renderer = new ITextRenderer();

        // AJOUT DE LA POLICE (méthode correcte Flying Saucer 9.x)
        ITextFontResolver fontResolver = renderer.getFontResolver();
        String fontPath = new ClassPathResource("fonts/NotoSans-Regular.ttf").getURL().toExternalForm();
        fontResolver.addFont(fontPath, true); // embeddée

        renderer.setDocumentFromString(htmlContent);
        renderer.layout();
        renderer.createPDF(outputStream); // peut lever DocumentException

        return outputStream.toByteArray();
    }
}