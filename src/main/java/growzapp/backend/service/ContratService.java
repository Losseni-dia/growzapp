// src/main/java/growzapp/backend/service/ContratService.java
package growzapp.backend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import growzapp.backend.repository.ContratRepository;
import lombok.RequiredArgsConstructor;
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

    private final ContratPdfService contratPdfService;
    private final FileStorageService fileStorageService;
    private final ContratRepository contratRepository;

    public Contrat genererEtSauvegarderContrat(Investissement investissement)
            throws IOException, com.lowagie.text.DocumentException, WriterException {

        // 1. Numéro unique
        long count = contratRepository.count() + 1;
        String numeroContrat = "CTR-" + Year.now().getValue() + "-" + String.format("%06d", count);

        // 2. Lien public
        String lienVerification = "https://growzapp.com/verifier-contrat?code=" + numeroContrat;

        // 3. QR Code
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(lienVerification, BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", qrOut);
        byte[] qrPng = qrOut.toByteArray();

        // 4. Générer PDF
        byte[] pdf = contratPdfService.genererContratInvestissement(investissement, numeroContrat, qrPng);

        // 5. Sauvegarder fichier
        String fileName = "contrat-" + numeroContrat + ".pdf";
        String url = fileStorageService.save(pdf, fileName, "application/pdf");

        // 6. Sauvegarder en base
        Contrat contrat = Contrat.builder()
                .investissement(investissement)
                .numeroContrat(numeroContrat)
                .fichierUrl(url)
                .lienVerification(lienVerification)
                .dateGeneration(LocalDateTime.now())
                .build();

        return contratRepository.save(contrat);
    }

    public byte[] genererPdfDepuisContrat(Contrat contrat)
            throws IOException, com.lowagie.text.DocumentException, WriterException {

        Investissement inv = contrat.getInvestissement();

        // Recréer QR code
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix matrix = qrWriter.encode(contrat.getLienVerification(), BarcodeFormat.QR_CODE, 300, 300);
        ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", qrOut);
        byte[] qrPng = qrOut.toByteArray();

        return contratPdfService.genererContratInvestissement(
                inv, contrat.getNumeroContrat(), qrPng
        );
    }
}