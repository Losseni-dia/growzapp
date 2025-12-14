package growzapp.backend.controller.api;

import com.lowagie.text.DocumentException;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.service.FactureService;
import growzapp.backend.service.FileStorageService;
import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/factures")
@RequiredArgsConstructor
public class FactureRestController {

    private final FactureService factureService;
    private final FileStorageService fileStorageService;
@GetMapping("/{id}")
    public ApiResponseDTO<FactureDTO> getById(@PathVariable Long id) {
        return ApiResponseDTO.success(factureService.getById(id));
    }

    // ========================================================================
    // TÉLÉCHARGEMENT INTELLIGENT (Stockage + Régénération + Traduction)
    // ========================================================================
    @GetMapping("/{factureId}/download")
    public ResponseEntity<Resource> downloadFacture(
            @PathVariable Long factureId,
            @RequestParam(name = "lang", defaultValue = "fr") String lang // Support langue
    ) {
        try {
            // Appel à la méthode du service qui gère : 
            // 1. La récupération du fichier disque (si dispo et langue FR)
            // 2. OU la régénération dynamique (si fichier perdu ou langue étrangère)
            byte[] pdfBytes = factureService.genererPdf(factureId, lang);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"facture-" + factureId + "_" + lang + ".pdf\"")
                    .body(new ByteArrayResource(pdfBytes));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}