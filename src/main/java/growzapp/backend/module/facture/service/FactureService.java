package growzapp.backend.module.facture.service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;

import org.hibernate.Hibernate;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.dividende.repository.DividendeRepository;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.facture.dto.FactureDTO;
import growzapp.backend.module.facture.enums.StatutFacture;
import growzapp.backend.module.facture.enums.TypeFacture;
import growzapp.backend.module.facture.mapper.FactureMapper;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.facture.repository.FactureRepository;
import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.repository.ProjetRepository;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
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
    private final NotificationService notificationService;
    private final UserRepository utilisateurRepository;
    private final ProjetRepository projetRepository;

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

            byte[] barcodeBytes = generateBarcode(facture.getNumeroFacture());
            byte[] pdfBytes = facturePdfService.generateDividendeFacture(dividende, barcodeBytes);
            log.info("PDF généré avec succès (taille: {})", pdfBytes.length);

            String fileName = "facture-dividende-" + dividende.getId() + ".pdf";
            String fichierUrl = fileStorageService.saveFacture(pdfBytes, fileName);

            facture.setFichierUrl(fichierUrl);
            factureRepository.save(facture);
            log.info("URL mise à jour en base : {}", fichierUrl);

            emailService.envoyerFactureParEmail(facture, pdfBytes);

            notificationService.notifyFactureEmise(
                    investisseur,
                    "Nouvelle facture disponible",
                    "Votre facture " + facture.getNumeroFacture() + " a été émise suite au versement de dividendes.",
                    facture.getId());

            log.info("Processus terminé avec succès pour dividende {}", dividendeId);

        } catch (Exception e) {
            log.error("ÉCHEC TOTAL de la génération facture + email pour dividende ID {}", dividendeId, e);
        }
    }

    /**
     * Génère la facture (reçu de paiement) pour un achat de statut Premium
     * et notifie le porteur — exécuté en tâche de fond après l'activation du
     * Premium. On ne reçoit que des identifiants (pas les entités JPA) :
     * l'appelant tourne dans une transaction/session Hibernate différente de
     * ce thread @Async, donc réutiliser ses entités directement provoquerait
     * une LazyInitializationException dès qu'un champ non chargé est
     * touché — on recharge tout fraîchement ici.
     */
    @Async
    @Transactional
    public void genererFacturePremium(Long porteurId, Long projetId, double montant, String sourcePaiement) {
        try {
            User porteur = utilisateurRepository.findById(porteurId)
                    .orElseThrow(() -> new EntityNotFoundException("Porteur introuvable : " + porteurId));
            Projet projet = projetRepository.findById(projetId)
                    .orElseThrow(() -> new EntityNotFoundException("Projet introuvable : " + projetId));

            // numeroFacture est NOT NULL en base : il doit être connu AVANT le
            // premier save (contrairement à facture.getId(), qui n'existe
            // qu'après l'insert) — on se base donc sur findMaxId(), pas sur
            // l'id généré de cette facture.
            Long prochainNumero = factureRepository.findMaxId() + 1;

            Facture facture = new Facture();
            facture.setInvestisseur(porteur);
            facture.setType(TypeFacture.PREMIUM);
            facture.setProjet(projet);
            facture.setLibelle("Achat du statut Premium — " + projet.getLibelle());
            facture.setMontantHT(montant);
            facture.setTva(0.0);
            facture.setMontantTTC(montant);
            facture.setDateEmission(LocalDateTime.now());
            facture.setDatePaiement(LocalDateTime.now());
            facture.setStatut(StatutFacture.PAYEE);
            facture.setNumeroFacture(
                    "FAC-" + Year.now().getValue() + "-P" + String.format("%06d", prochainNumero));

            facture = factureRepository.save(facture);

            byte[] barcodeBytes = generateBarcode(facture.getNumeroFacture());
            byte[] pdfBytes = facturePdfService.generatePremiumFacture(facture, projet, porteur, sourcePaiement,
                    barcodeBytes);

            String fileName = "facture-premium-" + facture.getId() + ".pdf";
            String fichierUrl = fileStorageService.saveFacture(pdfBytes, fileName);
            facture.setFichierUrl(fichierUrl);
            factureRepository.save(facture);

            notificationService.notifyFactureEmise(
                    porteur,
                    "⭐ Statut Premium activé",
                    "Votre projet « " + projet.getLibelle() + " » est maintenant Premium. Facture "
                            + facture.getNumeroFacture() + " disponible.",
                    facture.getId());

            log.info("Facture Premium générée : {} pour projet {}", facture.getNumeroFacture(), projet.getId());
        } catch (Exception e) {
            log.error("Échec génération facture Premium pour projet {}", projetId, e);
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
        return genererPdf(factureId, lang, "XOF");
    }

    public byte[] genererPdf(Long factureId, String lang, String devise) throws Exception {
        Facture facture = findById(factureId);

        Locale locale = Locale.FRENCH;
        if ("en".equalsIgnoreCase(lang))
            locale = Locale.ENGLISH;
        else if ("es".equalsIgnoreCase(lang))
            locale = Locale.forLanguageTag("es");

        boolean deviseParDefaut = devise == null || devise.isBlank() || "XOF".equalsIgnoreCase(devise);

        if (locale.equals(Locale.FRENCH) && deviseParDefaut) {
            try {
                return fileStorageService.loadAsBytes(facture.getFichierUrl());
            } catch (Exception e) {
                log.warn("Fichier original introuvable, régénération à la volée en FR.");
            }
        }

        byte[] barcodeBytes = generateBarcode(facture.getNumeroFacture());

        if (facture.getType() == TypeFacture.PREMIUM) {
            return facturePdfService.generatePremiumFacture(
                    facture, facture.getProjet(), facture.getInvestisseur(), null, barcodeBytes, locale, devise);
        }

        return facturePdfService.generateDividendeFacture(facture.getDividende(), barcodeBytes, locale, devise);
    }

    /** Toutes les factures (dividendes + Premium) émises pour un utilisateur. */
    public java.util.List<FactureDTO> getMesFactures(Long userId) {
        return factureMapper.toFactureDtoList(factureRepository.findByInvestisseurIdOrderByDateEmissionDesc(userId));
    }

    // ── Génération du code-barres (encode le numéro de facture) ─────────────
    public byte[] generateBarcode(String text) {
        try {
            Code128Writer writer = new Code128Writer();
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.CODE_128, 400, 120);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer le code-barres", e);
        }
    }
}