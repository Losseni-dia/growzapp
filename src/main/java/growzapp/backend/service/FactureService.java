package growzapp.backend.service;

import com.lowagie.text.DocumentException;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.entite.Dividende;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.model.enumeration.StatutFacture;
import growzapp.backend.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Year;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy)) // LIGNE MAGIQUE POUR EVITER LES PROBLEMES DE DEPENDANCE CIRCULAIRE
@Transactional
public class FactureService {

    private final FacturePdfService facturePdfService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final FactureRepository factureRepository;
    private final DtoConverter converter;

    
    private final DividendeService dividendeService;

    @Transactional(rollbackFor =  IOException.class)
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

        emailService.envoyerFactureParEmail(facture, pdfBytes);

        return facture;
    }

    public FactureDTO getById(Long id) {
        return factureRepository.findById(id)
                .map(converter::toFactureDto)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée : " + id));
    }

    public FactureDTO toFactureDto(Facture facture) {
        return converter.toFactureDto(facture);
    }
}