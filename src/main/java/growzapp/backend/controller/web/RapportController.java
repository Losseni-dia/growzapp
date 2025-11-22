// src/main/java/growzapp/backend/controller/web/RapportController.java
package growzapp.backend.controller.web;

import com.lowagie.text.DocumentException;
import growzapp.backend.service.RapportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
public class RapportController {

    private final RapportService rapportService;

    // ENDPOINT TEST (mois précédent)
    // ENDPOINT TEST → Télécharge le PDF
    @GetMapping("/rapport/test")
    public ResponseEntity<ByteArrayResource> testRapport() throws IOException, DocumentException {
        YearMonth mois = YearMonth.now().minusMonths(1);
        byte[] pdf = rapportService.genererRapportMensuel(mois);

        String nomFichier = "TEST_Rapport_Investissements_" + mois + ".pdf";
        ByteArrayResource resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    // ENDPOINT PROD (avec paramètre)
    @GetMapping("/rapport/mensuel")
    public ResponseEntity<ByteArrayResource> genererRapportMensuel(
            @RequestParam("mois") String moisStr) throws IOException, DocumentException {

        YearMonth mois = YearMonth.parse(moisStr, DateTimeFormatter.ofPattern("yyyy-MM"));
        byte[] pdf = rapportService.genererRapportMensuel(mois);

        String nomFichier = "Rapport_Investissements_" + mois + ".pdf";
        ByteArrayResource resource = new ByteArrayResource(pdf);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nomFichier + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}