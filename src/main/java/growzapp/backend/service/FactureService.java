package growzapp.backend.service;

import com.lowagie.text.DocumentException; // ← IMPORTE ÇA !

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.entite.Dividende;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.model.enumeration.StatutFacture;
import growzapp.backend.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional
public class FactureService {

    private final FacturePdfService facturePdfService;
    private final FileStorageService fileStorageService;
    private final DividendeService dividendeService;
    private final EmailService emailService;
    private final FactureRepository factureRepository;
   
    private final DtoConverter converter;

    @Transactional(rollbackFor = { IOException.class, DocumentException.class })
    public Facture genererEtSauvegarderFacture(Long dividendeId) throws IOException, DocumentException {
        Dividende dividende = dividendeService.findEntityById(dividendeId);

        byte[] pdfBytes = facturePdfService.generateDividendeFacture(dividende);

        String fileName = "facture-dividende-" + dividende.getId() + ".pdf";
        String fichierUrl = fileStorageService.save(pdfBytes, fileName, "application/pdf");

        Facture facture = new Facture();
        facture.setDividende(dividende);
        facture.setInvestisseur(dividende.getInvestissement().getInvestisseur());
        facture.setFichierUrl(fichierUrl);
        facture.setNumeroFacture("FAC-" + Year.now().getValue() + "-" + String.format("%04d", dividende.getId()));
        facture.setMontantTTC(dividende.getMontantTotal());
        facture.setStatut(StatutFacture.EMISE);

        facture = factureRepository.save(facture);

        // ENVOI AUTOMATIQUE PAR EMAIL
        emailService.envoyerFactureParEmail(facture, pdfBytes);

        return facture;
    }

    public FactureDTO getById(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée : " + id));
        return converter.toFactureDto(facture);
    }

    public FactureDTO toFactureDto(Facture facture) {
        return converter.toFactureDto(facture);
    }
}