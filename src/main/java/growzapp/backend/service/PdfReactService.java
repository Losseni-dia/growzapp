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
     * Génère le PDF FINAL avec le VRAI numéro de contrat.
     * Passe directement le contrat pour éviter les problèmes de lazy loading.
     */
    public byte[] genererPdfAvecVraiContrat(Contrat contrat, Investissement inv) {
        try {
            if (contrat == null || contrat.getNumeroContrat() == null) {
                throw new RuntimeException("Le contrat n'a pas encore été généré (null)");
            }

            String numeroContrat = contrat.getNumeroContrat();
            String lienVerification = contrat.getLienVerification();

            // Génération du QR Code
            byte[] qrPng = generateQrCode(lienVerification);

            // Appel au service PDF
            return contratPdfService.genererContratInvestissement(
                    inv,
                    numeroContrat,
                    qrPng);

        } catch (Exception e) {
            // Le logger est important ici pour voir la cause réelle (comme le
            // SAXParseException)
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la génération du PDF final : " + e.getMessage(), e);
        }
    }

    private byte[] generateQrCode(String url) throws Exception {
        // Encodage QR Code
        com.google.zxing.qrcode.QRCodeWriter qrWriter = new com.google.zxing.qrcode.QRCodeWriter();
        com.google.zxing.common.BitMatrix matrix = qrWriter.encode(
                url, // L'URL ici peut contenir des '&', le QR s'en fiche.
                com.google.zxing.BarcodeFormat.QR_CODE,
                300,
                300);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}