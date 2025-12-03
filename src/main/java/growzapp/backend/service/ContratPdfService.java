package growzapp.backend.service;

import growzapp.backend.model.entite.Investissement;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.lowagie.text.DocumentException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ContratPdfService {

    private final TemplateEngine templateEngine;

    public byte[] genererContratInvestissement(Investissement investissement,
            String numeroContrat,
            byte[] qrCodePng) throws IOException, DocumentException {

        Context context = new Context();
        context.setVariable("investissement", investissement);
        context.setVariable("projet", investissement.getProjet());
        context.setVariable("user", investissement.getInvestisseur());
        context.setVariable("numeroContrat", numeroContrat);

        // LÀ, C'EST LA LIGNE QUI MANQUAIT
        context.setVariable("contrat", investissement.getContrat());

        // BASE64 DANS LE SERVICE (PAS DANS LE TEMPLATE)
        String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCodePng);
        context.setVariable("qrCodeBase64", qrCodeBase64);

        String html = templateEngine.process("contrat/pdf-template", context);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        try {
            String fontPath = new ClassPathResource("fonts/NotoSans-Regular.ttf")
                    .getURL().toExternalForm();
            renderer.getFontResolver().addFont(fontPath, true);
        } catch (Exception e) {
            System.err.println("Police non trouvée, utilisation du fallback.");
        }

        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);
        renderer.finishPDF();

        return out.toByteArray();
    }
}