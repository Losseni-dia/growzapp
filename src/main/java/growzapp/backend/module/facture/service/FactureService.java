package growzapp.backend.module.facture.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;

import org.hibernate.Hibernate;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.facture.mapper.FactureMapper;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.facture.repository.FactureRepository;
import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
public class FactureService {

    private final FacturePdfService facturePdfService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final FactureRepository factureRepository;
    private final DividendeRepository dividendeRepository;
    private final FactureMapper factureMapper;

    @Async
    @Transactional
    public void genererFactureEtEnvoyerEmailAsync(Long dividendeId) {
        log.info("Début tâche asynchrone : génération facture + email pour dividende ID {}", dividendeId);

        try {
            Dividende dividende = dividendeRepository.findById(dividendeId)
                    .orElseThrow(() -> new RuntimeException("Dividende introuvable"));

            Investissement investissement = dividende.getInvestissement();
            if (investissement == null || investissement.getInvestisseur() == null) {
                log.warn("Données incomplètes pour dividende {} → abandon", dividendeId);
                return;
            }
            User investisseur = investissement.getInvestisseur();

            if (investissement.getProjet() != null) {
                Hibernate.initialize(investissement.getProjet());
                Projet realProjet = (Projet) Hibernate.unproxy(investissement.getProjet());
                investissement.setProjet(realProjet);
                log.info("Projet déballé avec succès : {}", realProjet.getLibelle());
            }

            Facture facture = new Facture();
            facture.setInvestisseur(investisseur);
            facture.setNumeroFacture("FAC-" + Year.now().getValue() + "-" + String.format("%06d", dividende.getId()));
            facture.setMontantHT(dividende.getMontantTotal());
            facture.setTva(0.0);
            facture.setMontantTTC(dividende.getMontantTotal());
            facture.setDateEmission(LocalDateTime.now());
            facture.setDatePaiement(dividende.getDatePaiement());
            facture.setStatut(StatutFacture.PAYEE);
            facture.setDividende(dividende);
            dividende.setFacture(facture);

            facture = factureRepository.save(facture);
            factureRepository.flush();

            dividende.setFacture(facture);

            byte[] pdfBytes = facturePdfService.generateDividendeFacture(dividende);
            log.info("PDF généré avec succès (taille: {})", pdfBytes.length);

            String fileName = "facture-dividende-" + dividende.getId() + ".pdf";
            String fichierUrl = fileStorageService.saveFacture(pdfBytes, fileName);

            facture.setFichierUrl(fichierUrl);
            factureRepository.save(facture);
            log.info("URL mise à jour en base : {}", fichierUrl);

            emailService.envoyerFactureParEmail(facture, pdfBytes);

            log.info("Processus terminé avec succès pour dividende {}", dividendeId);

        } catch (Exception e) {
            log.error("ÉCHEC TOTAL de la génération facture + email pour dividende ID {}", dividendeId, e);
        }
    }

    public Facture findById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Facture non trouvée : " + id));
    }

    public FactureDTO getById(Long id) {
        return factureMapper.toFactureDto(findById(id));
    }

    public byte[] genererPdf(Long factureId, String lang) throws Exception {
        Facture facture = findById(factureId);

        Locale locale = Locale.FRENCH;
        if ("en".equalsIgnoreCase(lang))
            locale = Locale.ENGLISH;
        else if ("es".equalsIgnoreCase(lang))
            locale = new Locale("es");

        if (locale.equals(Locale.FRENCH)) {
            try {
                return fileStorageService.loadAsBytes(facture.getFichierUrl());
            } catch (Exception e) {
                log.warn("Fichier original introuvable, régénération à la volée en FR.");
            }
        }

        return facturePdfService.generateDividendeFacture(facture.getDividende(), locale);
    }
}
