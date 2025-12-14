// src/main/java/growzapp/backend/service/EmailService.java
package growzapp.backend.service;

import growzapp.backend.model.entite.Dividende;
import growzapp.backend.model.entite.Facture;
import growzapp.backend.model.entite.Investissement;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.YearMonth;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // === FACTURE (déjà existante) ===
    // Dans EmailService.java – SUPPRIME @Async pour les factures
    public void envoyerFactureParEmail(Facture facture, byte[] pdfBytes) { // ← PLUS @Async
        Dividende dividende = facture.getDividende();
        if (dividende == null || dividende.getInvestissement() == null
                || dividende.getInvestissement().getInvestisseur() == null) {
            log.error("Impossible d'envoyer l'email : données manquantes sur la facture {}", facture.getId());
            return;
        }

        String emailInvestisseur = dividende.getInvestissement().getInvestisseur().getEmail();
        String nomInvestisseur = dividende.getInvestissement().getInvestisseur().getPrenom() + " " +
                dividende.getInvestissement().getInvestisseur().getNom();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailInvestisseur);
            helper.setSubject("Votre facture de dividende – GrowzApp");
            helper.setText("""
                    <h2>Bonjour %s,</h2>
                    <p>Nous avons le plaisir de vous verser un dividende !</p>
                    <p><strong>Montant total :</strong> %.2f €</p>
                    <p>Vous trouverez ci-joint votre facture officielle.</p>
                    <br>
                    <p>Merci pour votre confiance dans l’agriculture durable.</p>
                    <p><strong>L’équipe GrowzApp</strong></p>
                    """.formatted(nomInvestisseur, dividende.getMontantTotal()), true);

            helper.addAttachment(
                    "Facture_Dividende_" + dividende.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Email facture envoyé à {}", emailInvestisseur);

        } catch (MessagingException e) {
            log.error("Échec envoi email facture {} : {}", facture.getId(), e.getMessage(), e);
        }
    }

    // === TEST EMAIL (déjà existant) ===
    @Async
    public void envoyerEmailTestDirect(String emailDestinataire) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailDestinataire);
            helper.setSubject("Test réussi – GrowzApp");
            helper.setText("""
                    <h2>Félicitations !</h2>
                    <p>Ton système d’envoi de factures par email fonctionne à 100 %</p>
                    <p>Tu trouveras ci-joint un PDF valide et ouvrable.</p>
                    <br>
                    <p>Prochaine étape : la page « Mes dividendes & factures » de l’investisseur</p>
                    <p>L’équipe GrowzApp</p>
                    """, true);

            String validPdfContent = """
                    %PDF-1.4
                    1 0 obj
                    << /Type /Catalog /Pages 2 0 R >>
                    endobj
                    2 0 obj
                    << /Type /Pages /Kids [3 0 R] /Count 1 >>
                    endobj
                    3 0 obj
                    << /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>
                    endobj
                    4 0 obj
                    << /Length 44 >>
                    stream
                    BT /F1 24 Tf 100 700 Td (Test PDF GrowzApp - Ça marche !) Tj ET
                    endstream
                    endobj
                    xref
                    0 5
                    0000000000 65535 f
                    0000000010 00000 n
                    0000000053 00000 n
                    0000000102 00000 n
                    0000000197 00000 n
                    trailer << /Size 5 /Root 1 0 R >>
                    startxref
                    280
                    %%EOF
                    """;

            byte[] pdfBytes = validPdfContent.getBytes();
            helper.addAttachment("Test_Facture_Valide.pdf", new ByteArrayResource(pdfBytes));

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === NOUVELLE MÉTHODE : CONTRAT ===
    @Async
    public void envoyerContratParEmail(Investissement investissement, byte[] pdfContrat) {
        try {
            // --- EXTRACTION DES DONNÉES ---
            String emailInvestisseur = investissement.getInvestisseur().getEmail();
            String nomInvestisseur = investissement.getInvestisseur().getPrenom() + " "
                    + investissement.getInvestisseur().getNom();
            String projetLibelle = investissement.getProjet().getLibelle();
            String numeroContrat = investissement.getContrat().getNumeroContrat();
            String lienVerification = investissement.getContrat().getLienVerification();
            String emailPorteur = investissement.getProjet().getPorteur().getEmail();

            // --- LOGS DE DÉBUT ---
            System.out.println("EMAIL : Début envoi à " + emailInvestisseur);
            if (emailPorteur != null && !emailPorteur.isBlank()) {
                System.out.println("EMAIL : CC au porteur → " + emailPorteur);
            }

            // --- PRÉPARATION DU MESSAGE ---
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailInvestisseur);
            if (emailPorteur != null && !emailPorteur.isBlank()) {
                helper.setCc(emailPorteur);
            }

            helper.setSubject("Nouveau contrat d'investissement – GrowzApp");

            helper.setText(
                    """
                            <h2 style="color:#28a745;">Bonjour %s,</h2>
                            <p>Félicitations pour votre investissement dans le projet <strong>%s</strong> !</p>
                            <p>Vous trouverez ci-joint votre <strong>contrat officiel</strong> en PDF.</p>
                            <p>Vérifiez son authenticité via le QR code ou ce lien :</p>
                            <p style="text-align: center; margin: 20px 0;">
                                <a href="%s" style="background:#007bff; color:white; padding:12px 28px; text-decoration:none; border-radius:8px; font-weight:bold; font-size:1.1em;">
                                    Vérifier le contrat
                                </a>
                            </p>
                            <hr style="border:0; border-top:1px solid #eee; margin:30px 0;">
                            <p>Merci pour votre confiance dans l’agriculture durable.</p>
                            <p><strong>L’équipe GrowzApp</strong></p>
                            """
                            .formatted(nomInvestisseur, projetLibelle, lienVerification),
                    true // HTML activé
            );

            // --- PIÈCE JOINTE ---
            helper.addAttachment(
                    "Contrat_" + numeroContrat + ".pdf",
                    new ByteArrayResource(pdfContrat));
            System.out.println("EMAIL : Pièce jointe ajoutée → Contrat_" + numeroContrat + ".pdf (" + pdfContrat.length
                    + " octets)");

            // --- ENVOI ---
            mailSender.send(message);
            System.out.println("EMAIL : ENVOYÉ AVEC SUCCÈS à " + emailInvestisseur + " !");

        } catch (Exception e) {
            System.err.println("EMAIL : ÉCHEC D'ENVOI → " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
public void envoyerRapportMensuel(String emailAdmin, YearMonth mois, byte[] pdf) {
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(emailAdmin);
        helper.setSubject("Rapport mensuel des investissements – " + mois);
        helper.setText("""
                <h2>Rapport mensuel généré</h2>
                <p>Voici le rapport complet des investissements validés pour le mois de 
                <strong>%s</strong>.</p>
                <p>Vous trouverez ci-joint le document PDF.</p>
                <br>
                <p>Cordialement,</p>
                <p><strong>GrowzApp Bot</strong></p>
                """.formatted(mois.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH))), true);

        helper.addAttachment(
                "Rapport_Investissements_" + mois + ".pdf",
                new ByteArrayResource(pdf)
        );

        mailSender.send(message);
    } catch (Exception e) {
        e.printStackTrace();
    }
}
}