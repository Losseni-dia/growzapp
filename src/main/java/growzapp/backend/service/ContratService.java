// src/main/java/growzapp/backend/service/ContratService.java → VERSION FINALE 2025

package growzapp.backend.service;

import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.repository.ContratRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratService {

    private final FileStorageService fileStorageService;
    private final ContratRepository contratRepository;
    private final UserService userService;
    private final ContratPdfService contratPdfService;

    // ========================================================================
    // 1. RECHERCHE & SÉCURITÉ
    // ========================================================================

    public Contrat trouverParNumero(String numero) {
        return contratRepository.findByNumeroContrat(numero)
                .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : " + numero));
    }

    public boolean utilisateurPeutVoirContrat(Contrat contrat) {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null)
            return false;

        Investissement inv = contrat.getInvestissement();

        // Admin peut tout voir
        if (currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getRole()))) {
            return true;
        }

        // Investisseur du contrat
        if (inv.getInvestisseur().getId().equals(currentUser.getId())) {
            return true;
        }

        // Porteur du projet lié
        Projet projet = inv.getProjet();
        if (projet.getPorteur() != null && projet.getPorteur().getId().equals(currentUser.getId())) {
            return true;
        }

        return false;
    }

    public Page<Contrat> rechercherAvecFiltres(
            String search,
            String dateDebut,
            String dateFin,
            String statut,
            Integer montantMin,
            Integer montantMax,
            Pageable pageable) {

        LocalDateTime debut = dateDebut != null && !dateDebut.isBlank()
                ? LocalDateTime.parse(dateDebut + "T00:00:00")
                : null;

        LocalDateTime fin = dateFin != null && !dateFin.isBlank()
                ? LocalDateTime.parse(dateFin + "T23:59:59")
                : null;

        StatutPartInvestissement statutEnum = null;
        if (statut != null && !statut.isBlank()) {
            try {
                statutEnum = StatutPartInvestissement.valueOf(statut);
            } catch (IllegalArgumentException ignored) {
            }
        }

        return contratRepository.rechercherAvecFiltres(
                search, debut, fin, statutEnum, montantMin, montantMax, pageable);
    }

    // ========================================================================
    // 2. GÉNÉRATION DE PDF MULTILINGUE (MÉTHODE AJOUTÉE)
    // ========================================================================

    /**
     * Génère ou récupère le PDF du contrat dans la langue spécifiée.
     */
    public byte[] genererPdf(Contrat contrat, String lang) throws Exception {
        // 1. Déterminer la locale
        Locale locale = Locale.FRENCH; // Défaut
        if ("en".equalsIgnoreCase(lang))
            locale = Locale.ENGLISH;
        else if ("es".equalsIgnoreCase(lang))
            locale = new Locale("es");

        // 2. Si français (original), on tente de charger le fichier stocké pour
        // perf/juridique
        if (locale.equals(Locale.FRENCH)) {
            try {
                return fileStorageService.loadAsBytes(contrat.getFichierUrl());
            } catch (Exception e) {
                // Fallback : si fichier physique perdu, on régénère
                System.err.println("PDF original introuvable, régénération FR...");
            }
        }

        // 3. Génération dynamique (Traduction à la volée OU Fallback)
        byte[] qrCode = generateQrCode(contrat.getLienVerification());

        // Appel à la méthode surchargée du PDF Service qui accepte la Locale
        return contratPdfService.genererContratInvestissement(
                contrat.getInvestissement(),
                contrat.getNumeroContrat(),
                qrCode,
                locale);
    }

    // ========================================================================
    // 3. CRÉATION & SAUVEGARDE (Flux Business)
    // ========================================================================

    // Etape 1 : Création de l'entité Contrat en base (sans fichier)
    public Contrat genererEtSauvegarderContrat(Investissement investissement) {
        long count = contratRepository.count() + 1;
        String numeroContrat = "CTR-" + Year.now().getValue() + "-" + String.format("%06d", count);
        String lienVerification = "https://growzapp.com/verifier-contrat?code=" + numeroContrat;

        Contrat contrat = Contrat.builder()
                .investissement(investissement)
                .numeroContrat(numeroContrat)
                .lienVerification(lienVerification)
                .dateGeneration(LocalDateTime.now())
                .fichierUrl("") // Sera rempli par sauvegarderPdfFinal
                .build();

        return contratRepository.save(contrat);
    }

    // Etape 2 : Sauvegarde physique du PDF généré et mise à jour de l'URL
    public void sauvegarderPdfFinal(Contrat contrat, byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF vide");
        }

        String filename = "contrat-" + contrat.getNumeroContrat() + ".pdf";
        // Sauvegarde via FileStorageService qui retourne l'URL relative (/uploads/...)
        String webUrl = fileStorageService.saveContrat(pdfBytes, filename);

        contrat.setFichierUrl(webUrl);
        contratRepository.saveAndFlush(contrat);
        System.out.println("Contrat sauvegardé avec URL : " + webUrl);
    }

    // ========================================================================
    // 4. UTILITAIRES
    // ========================================================================

    public byte[] generateQrCode(String text) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 300, 300);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Impossible de générer le QR code", e);
        }
    }

    @Transactional
    public void regenererTousLesContratsManquants() {
        contratRepository.findAll().forEach(contrat -> {
            if (contrat.getFichierUrl() == null || contrat.getFichierUrl().isBlank()) {
                try {
                    // Régénération en Français par défaut pour le stockage
                    byte[] pdf = contratPdfService.genererContratInvestissement(
                            contrat.getInvestissement(),
                            contrat.getNumeroContrat(),
                            generateQrCode(contrat.getLienVerification()),
                            Locale.FRENCH);

                    sauvegarderPdfFinal(contrat, pdf);
                    System.out.println("Contrat regénéré : " + contrat.getNumeroContrat());
                } catch (Exception e) {
                    System.err.println("Erreur pour " + contrat.getNumeroContrat() + " : " + e.getMessage());
                }
            }
        });
    }
}