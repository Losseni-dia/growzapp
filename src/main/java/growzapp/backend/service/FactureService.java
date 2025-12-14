package growzapp.backend.service;

import com.lowagie.text.DocumentException;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.factureDTO.FactureDTO;
import growzapp.backend.model.entite.Dividende;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet; // N'oublie pas cet import
import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.StatutFacture;
import growzapp.backend.repository.DividendeRepository;
import growzapp.backend.repository.FactureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate; // IMPORT CRUCIAL POUR LE DÉBALLAGE
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// N'oublie pas l'import
import java.util.Locale;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Lazy))
public class FactureService {

    private final FacturePdfService facturePdfService;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final FactureRepository factureRepository;
    private final DividendeRepository dividendeRepository;
    private final DtoConverter converter;

    /**
     * Génération asynchrone de la facture + envoi email
     */
    @Async
    @Transactional
    public void genererFactureEtEnvoyerEmailAsync(Long dividendeId) {
        log.info("Début tâche asynchrone : génération facture + email pour dividende ID {}", dividendeId);

        try {
            // 1. Recharger le dividende
            Dividende dividende = dividendeRepository.findById(dividendeId)
                    .orElseThrow(() -> new RuntimeException("Dividende introuvable"));

            Investissement investissement = dividende.getInvestissement();
            if (investissement == null || investissement.getInvestisseur() == null) {
                log.warn("Données incomplètes pour dividende {} → abandon", dividendeId);
                return;
            }
            User investisseur = investissement.getInvestisseur();

            // =================================================================
            // 🛑 LE FIX ULTIME POUR LE PROJET "FANTÔME" (PROXY)
            // =================================================================
            if (investissement.getProjet() != null) {
                // 1. On force le chargement SQL
                Hibernate.initialize(investissement.getProjet());

                // 2. On "déballe" l'objet : on retire la couche Proxy d'Hibernate
                // Cela extrait le vrai objet Projet que Thymeleaf pourra lire sans erreur
                Projet realProjet = (Projet) Hibernate.unproxy(investissement.getProjet());

                // 3. On remplace le proxy par le vrai objet dans l'investissement en mémoire
                investissement.setProjet(realProjet);

                // J'utilise getLibelle() car tu as confirmé que c'est le bon nom
                log.info("Projet déballé avec succès : {}", realProjet.getLibelle());
            }
            // =================================================================

            // 2. Créer la Facture
            Facture facture = new Facture();
            facture.setInvestisseur(investisseur);
            facture.setNumeroFacture("FAC-" + Year.now().getValue() + "-" + String.format("%06d", dividende.getId()));

            // Montants
            facture.setMontantHT(dividende.getMontantTotal());
            facture.setTva(0.0);
            facture.setMontantTTC(dividende.getMontantTotal());
            facture.setDateEmission(LocalDateTime.now());
            facture.setDatePaiement(dividende.getDatePaiement());
            facture.setStatut(StatutFacture.PAYEE);

            // Liaison initiale
            facture.setDividende(dividende);
            dividende.setFacture(facture);

            // === SAUVEGARDE INITIALE (Évite TransientObjectException) ===
            facture = factureRepository.save(facture);
            factureRepository.flush();

            // === RE-LIAISON DE SÉCURITÉ ===
            // On s'assure que l'objet dividende contient bien la facture SAUVEGARDÉE
            dividende.setFacture(facture);

            // 3. Générer le PDF (Maintenant le projet est "déballé", Thymeleaf va réussir)
            byte[] pdfBytes = facturePdfService.generateDividendeFacture(dividende);
            log.info("PDF généré avec succès (taille: {})", pdfBytes.length);

            // 4. Sauvegarder le fichier et Mettre à jour l'URL
            String fileName = "facture-dividende-" + dividende.getId() + ".pdf";
            String fichierUrl = fileStorageService.saveFacture(pdfBytes, fileName);

            facture.setFichierUrl(fichierUrl);
            factureRepository.save(facture); // UPDATE FINAL
            log.info("URL mise à jour en base : {}", fichierUrl);

            // 5. Email
            emailService.envoyerFactureParEmail(facture, pdfBytes);

            log.info("Processus terminé avec succès pour dividende {}", dividendeId);

        } catch (Exception e) {
            log.error("ÉCHEC TOTAL de la génération facture + email pour dividende ID {}", dividendeId, e);
        }
    }

    // === Méthodes utilitaires ===
    public FactureDTO getById(Long id) {
        return factureRepository.findById(id)
                .map(converter::toFactureDto)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée : " + id));
    }

    public FactureDTO toFactureDto(Facture facture) {
        return converter.toFactureDto(facture);
    }

    public Facture findEntityById(Long id) {
        return factureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Facture non trouvée : " + id));
    }


    // Dans FactureService.java

 

    // ... autres méthodes existantes ...

    /**
     * Génère ou récupère le PDF de la facture dans la langue spécifiée.
     */
    public byte[] genererPdf(Long factureId, String lang) throws Exception {
        Facture facture = findEntityById(factureId);
        
        // 1. Définition de la Locale
        Locale locale = Locale.FRENCH; // Par défaut
        if ("en".equalsIgnoreCase(lang)) locale = Locale.ENGLISH;
        else if ("es".equalsIgnoreCase(lang)) locale = new Locale("es");

        // 2. Si c'est en FR (langue originale), on essaie de charger le fichier stocké pour la performance
        if (locale.equals(Locale.FRENCH)) {
            try {
                // On suppose que getFichierUrl retourne le chemin relatif (ex: /uploads/factures/...)
                return fileStorageService.loadAsBytes(facture.getFichierUrl());
            } catch (Exception e) {
                log.warn("Fichier original introuvable, régénération à la volée en FR.");
            }
        }

        // 3. Génération dynamique (EN, ES ou fallback FR)
        // Il faut s'assurer que les objets liés sont chargés (Projet, Investisseur)
        // Hibernate.initialize... si nécessaire, mais normalement findById le gère si EAGER ou transaction
        
        return facturePdfService.generateDividendeFacture(facture.getDividende(), locale);
    }
}