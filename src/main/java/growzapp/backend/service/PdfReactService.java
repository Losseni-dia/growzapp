// src/main/java/growzapp/backend/service/PdfReactService.java

package growzapp.backend.service;

import growzapp.backend.model.entite.Contrat;
import growzapp.backend.model.entite.Investissement;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.google.zxing.client.j2se.MatrixToImageWriter;

import java.io.ByteArrayOutputStream;

@Service
@RequiredArgsConstructor
public class PdfReactService {

    private final ContratPdfService contratPdfService;

    /**
     * Génère le PDF FINAL avec le VRAI numéro de contrat
     */
    /**
     * Génère le PDF FINAL avec le VRAI numéro de contrat
     * Version corrigée : on passe directement le contrat pour éviter les problèmes
     * de lazy loading
     */
    public byte[] genererPdfAvecVraiContrat(Contrat contrat, Investissement inv) {
        try {
            if (contrat == null || contrat.getNumeroContrat() == null) {
                throw new RuntimeException("Le contrat n'a pas encore été généré");
            }

            String numeroContrat = contrat.getNumeroContrat();
            String lienVerification = contrat.getLienVerification();

            byte[] qrPng = generateQrCode(lienVerification);

            return contratPdfService.genererContratInvestissement(
                    inv,
                    numeroContrat,
                    qrPng);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF final", e);
        }
    }

    private byte[] generateQrCode(String url) throws Exception {
        com.google.zxing.qrcode.QRCodeWriter qrWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix matrix = qrWriter.encode(
                url,
                com.google.zxing.BarcodeFormat.QR_CODE,
                300,
                300);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}