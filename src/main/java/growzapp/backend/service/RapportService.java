package growzapp.backend.service;

import com.lowagie.text.DocumentException; // AJOUTE CET IMPORT
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.repository.InvestissementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RapportService {

    private final InvestissementRepository investissementRepository;
    private final TemplateEngine templateEngine;

    /**
     * Génère un rapport PDF mensuel
     * 
     * @throws IOException       si erreur de lecture police ou écriture
     * @throws DocumentException si erreur Flying Saucer
     */
    public byte[] genererRapportMensuel(YearMonth mois) throws IOException, DocumentException {
        // 1. Période
        LocalDate debut = mois.atDay(1);
        LocalDateTime debutDateTime = debut.atStartOfDay();
        LocalDate fin = mois.atEndOfMonth();
        LocalDateTime finDateTime = fin.atTime(23, 59, 59);

        List<Investissement> investissements = investissementRepository
                .findValidInvestmentsByDateRange(debutDateTime, finDateTime, StatutPartInvestissement.VALIDE);

      // On utilise stream().map() au lieu de mapToDouble()
BigDecimal totalCollecte = investissements.stream()
    .map(inv -> {
        BigDecimal prixPart = inv.getProjet().getPrixUnePart();
        // Correction : multiplier le BigDecimal par le nombre de parts (converti en BigDecimal)
        return prixPart.multiply(BigDecimal.valueOf(inv.getNombrePartsPris()));
    })
    // Correction : Additionner les BigDecimal entre eux
    .reduce(BigDecimal.ZERO, BigDecimal::add);

        long projetsDistincts = investissements.stream()
                .map(inv -> inv.getProjet().getId())
                .distinct()
                .count();

        // 4. Contexte Thymeleaf
        Context context = new Context();
        context.setVariable("mois", debut);
        context.setVariable("investissements", investissements);
        context.setVariable("totalCollecte", totalCollecte);
        context.setVariable("projetsDistincts", projetsDistincts);

        // 5. HTML
        String html = templateEngine.process("rapport/mensuel-investissements", context);

        // 6. PDF
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
        renderer.createPDF(out); // DocumentException gérée par throws
        renderer.finishPDF();

        return out.toByteArray();
    }
}