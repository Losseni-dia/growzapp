package growzapp.backend.module.contrat.service;

import growzapp.backend.module.contrat.model.Contrat;
import growzapp.backend.module.contrat.repository.ContratRepository;
import growzapp.backend.module.files.FileStorageService;
import growzapp.backend.module.investissement.enums.StatutPartInvestissement;
import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
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

        if (currentUser.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getRole()))) {
            return true;
        }

        if (inv.getInvestisseur().getId().equals(currentUser.getId())) {
            return true;
        }

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
    // 2. GÉNÉRATION DE PDF MULTILINGUE
    // ========================================================================

    public byte[] genererPdf(Contrat contrat, String lang) throws Exception {
        Locale locale = Locale.FRENCH;
        if ("en".equalsIgnoreCase(lang))
            locale = Locale.ENGLISH;
        else if ("es".equalsIgnoreCase(lang))
            locale = new Locale("es");

        if (locale.equals(Locale.FRENCH)) {
            try {
                return fileStorageService.loadAsBytes(contrat.getFichierUrl());
            } catch (Exception e) {
                System.err.println("PDF original introuvable, régénération FR...");
            }
        }

        byte[] qrCode = generateQrCode(contrat.getLienVerification());

        return contratPdfService.genererContratInvestissement(
                contrat.getInvestissement(),
                contrat.getNumeroContrat(),
                qrCode,
                locale);
    }

    // ========================================================================
    // 3. CRÉATION & SAUVEGARDE
    // ========================================================================

    public Contrat genererEtSauvegarderContrat(Investissement investissement) {
        long count = contratRepository.count() + 1;
        String numeroContrat = "CTR-" + Year.now().getValue() + "-" + String.format("%06d", count);
        String lienVerification = "https://growzapp.com/verifier-contrat?code=" + numeroContrat;

        Contrat contrat = Contrat.builder()
                .investissement(investissement)
                .numeroContrat(numeroContrat)
                .lienVerification(lienVerification)
                .dateGeneration(LocalDateTime.now())
                .fichierUrl("")
                .build();

        return contratRepository.save(contrat);
    }

    public void sauvegarderPdfFinal(Contrat contrat, byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF vide");
        }

        String filename = "contrat-" + contrat.getNumeroContrat() + ".pdf";
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
