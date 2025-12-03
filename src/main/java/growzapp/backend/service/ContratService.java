// src/main/java/growzapp/backend/service/ContratService.java → VERSION FINALE 2025

package growzapp.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import growzapp.backend.model.entite.*;
import growzapp.backend.model.enumeration.StatutPartInvestissement;
import growzapp.backend.repository.ContratRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
@Transactional
public class ContratService {

    private final FileStorageService fileStorageService;
    private final ContratRepository contratRepository;
    private final UserService userService;



    // Recherche
    public Contrat trouverParNumero(String numero) {
        return contratRepository.findByNumeroContrat(numero)
                .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé : " + numero));
    }

    // Sécurité
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



    public Contrat genererEtSauvegarderContrat(Investissement investissement, byte[] pdfReact) throws IOException {
        long count = contratRepository.count() + 1;
        String numeroContrat = "CTR-" + Year.now().getValue() + "-" + String.format("%06d", count);
        String lienVerification = "https://growzapp.com/verifier-contrat?code=" + numeroContrat;

        String fileName = "contrat-" + numeroContrat + ".pdf";
        String url = fileStorageService.save(pdfReact, fileName, "application/pdf");

        Contrat contrat = Contrat.builder()
                .investissement(investissement)
                .numeroContrat(numeroContrat)
                .fichierUrl(url)
                .lienVerification(lienVerification)
                .dateGeneration(LocalDateTime.now())
                .build();

        return contratRepository.save(contrat);
    }


    // ===================================================================
    // Méthode pour sauvegarder le PDF final (C'EST CELLE QUI MANQUAIT)
    // ===================================================================
    public void sauvegarderPdfFinal(Contrat contrat, byte[] pdfBytes) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF vide");
        }

        String fileName = "contrat-" + contrat.getNumeroContrat() + ".pdf";
        String url = fileStorageService.save(pdfBytes, fileName, "application/pdf");

        contrat.setFichierUrl(url);
        contratRepository.saveAndFlush(contrat); // flush ici aussi
    }



    public Contrat genererEtSauvegarderContrat(Investissement investissement) {
        long count = contratRepository.count() + 1;
        String numeroContrat = "CTR-" + Year.now().getValue() + "-" + String.format("%06d", count);
        String lienVerification = "https://growzapp.com/verifier-contrat?code=" + numeroContrat;

        Contrat contrat = Contrat.builder()
                .investissement(investissement)
                .numeroContrat(numeroContrat)
                .lienVerification(lienVerification)
                .dateGeneration(LocalDateTime.now())
                .fichierUrl("") // On mettra l'URL après génération du vrai PDF
                .build();

        return contratRepository.save(contrat);
    }

    
}