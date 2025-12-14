package growzapp.backend.service;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Contrat;
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
import java.util.Locale; // <--- IMPORT IMPORTANT

@Service
@RequiredArgsConstructor
public class ContratPdfService {

    private final TemplateEngine templateEngine;

    // Surcharge pour compatibilité (appelle la version avec locale par défaut = FR)
    public byte[] genererContratInvestissement(Investissement investissement, String numeroContrat, byte[] qrCodePng)
            throws IOException, DocumentException {
        return genererContratInvestissement(investissement, numeroContrat, qrCodePng, Locale.FRENCH);
    }

    // NOUVELLE MÉTHODE AVEC LOCALE
    public byte[] genererContratInvestissement(Investissement investissement,
            String numeroContrat,
            byte[] qrCodePng,
            Locale locale) throws IOException, DocumentException { // <--- PARAMÈTRE AJOUTÉ

        // On passe la locale au Context Thymeleaf
        Context context = new Context(locale);

        // 1. Injection des objets standards
        context.setVariable("investissement", investissement);
        context.setVariable("projet", investissement.getProjet());
        context.setVariable("user", investissement.getInvestisseur());
        context.setVariable("numeroContrat", numeroContrat);
        context.setVariable("contrat", investissement.getContrat());

        // 2. Nettoyage XML
        Contrat contrat = investissement.getContrat();
        String lienVerif = (contrat != null) ? contrat.getLienVerification() : "";
        context.setVariable("safeLienVerification", cleanForXml(lienVerif));

        String descriptionSafe = cleanForXml(investissement.getProjet().getDescription());
        context.setVariable("safeDescription", descriptionSafe);

        // 3. Gestion image
        if (qrCodePng != null) {
            String qrCodeBase64 = Base64.getEncoder().encodeToString(qrCodePng);
            context.setVariable("qrCodeBase64", qrCodeBase64);
        }

        // 4. Génération du HTML (Thymeleaf va chercher messages_en.properties si
        // locale=EN)
        String html = templateEngine.process("contrat/pdf-template", context);

        // 5. Rendu PDF
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ITextRenderer renderer = new ITextRenderer();

        try {
            // Police compatible Unicode
            String fontPath = new ClassPathResource("fonts/NotoSans-Regular.ttf").getURL().toExternalForm();
            renderer.getFontResolver().addFont(fontPath, true);
        } catch (Exception e) {
            System.err.println("Police non trouvée : " + e.getMessage());
        }

        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);
        renderer.finishPDF();

        return out.toByteArray();
    }

    private String cleanForXml(String input) {
        if (input == null)
            return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}