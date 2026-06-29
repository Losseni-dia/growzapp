// src/main/java/growzapp/backend/service/EmailService.java
package growzapp.backend.module.email;

import java.time.YearMonth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import growzapp.backend.module.dividende.model.Dividende;
import growzapp.backend.module.facture.model.Facture;
import growzapp.backend.module.investissement.model.Investissement;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}") // On utilise le nom exact de ton application.properties
    private String frontendUrl;

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
                    """.formatted(
                    mois.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH))),
                    true);

            helper.addAttachment(
                    "Rapport_Investissements_" + mois + ".pdf",
                    new ByteArrayResource(pdf));

            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // === MOT DE PASSE OUBLIÉ ===
    @Async
    public void sendPasswordResetMail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // TODO: Ajuste l'URL selon ton environnement (localhost ou domaine réel)
            String resetLink = frontendUrl + "/reset-password?token=" + token;

            helper.setTo(to);
            helper.setSubject("GrowzApp – Réinitialisation de votre mot de passe");

            String htmlContent = """
                    <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #333; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;">
                        <div style="text-align: center; margin-bottom: 20px;">
                            <h1 style="color: #28a745; margin: 0;">GrowzApp</h1>
                            <p style="font-size: 0.9em; color: #666;">Investir dans l'avenir durable</p>
                        </div>
                        <h2 style="color: #333;">Bonjour,</h2>
                        <p>Nous avons reçu une demande de réinitialisation de mot de passe pour votre compte GrowzApp.</p>
                        <p>Cliquez sur le bouton ci-dessous pour définir un nouveau mot de passe. Ce lien est valable pendant <strong>24 heures</strong>.</p>

                        <div style="text-align: center; margin: 35px 0;">
                            <a href="%s" style="background-color: #28a745; color: white; padding: 14px 30px; text-decoration: none; border-radius: 8px; font-weight: bold; font-size: 1.1em; display: inline-block;">
                                Réinitialiser mon mot de passe
                            </a>
                        </div>

                        <p style="font-size: 0.9em; color: #666;">Si vous n'avez pas demandé cette réinitialisation, vous pouvez ignorer cet e-mail en toute sécurité. Votre mot de passe actuel ne sera pas modifié.</p>

                        <hr style="border: none; border-top: 1px solid #eee; margin-top: 30px;">
                        <p style="font-size: 0.8em; color: #999; text-align: center;">
                            Ceci est un message automatique, merci de ne pas y répondre.<br>
                            &copy; 2024 GrowzApp - Agriculture Durable
                        </p>
                    </div>
                    """
                    .formatted(resetLink);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé avec succès");

        } catch (MessagingException e) {
            log.error("Échec de l'envoi de l'email de reset à {} : {}", to, e.getMessage());
        }
    }

    // AJOUTER cette méthode dans EmailService.java
    // juste avant la dernière accolade }

    // === REFUS D'INVESTISSEMENT ===
    @Async
    public void envoyerRefusInvestissement(
            String emailInvestisseur,
            String nomInvestisseur,
            String projetLibelle,
            String montant,
            String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailInvestisseur);
            helper.setSubject("Investissement refusé — " + projetLibelle + " — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;margin:0 0 4px 0;">GrowzApp</h1>
                              <p style="color:#888;font-size:0.85em;margin:0 0 24px 0;">Plateforme d'investissement participatif</p>

                              <h2 style="color:#c62828;">Investissement refusé</h2>

                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Nous vous informons que votre investissement dans le projet <strong>%s</strong> a été refusé par notre équipe.</p>

                              <div style="background:#fff5f5;border-left:4px solid #c62828;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;font-weight:bold;color:#c62828;">Motif du refus :</p>
                                <p style="margin:8px 0 0 0;color:#333;">%s</p>
                              </div>

                              <div style="background:#f1f8e9;border-radius:8px;padding:16px;margin:20px 0;">
                                <p style="margin:0;color:#555;">💰 <strong>%s FCFA</strong> ont été restitués dans votre portefeuille GrowzApp.</p>
                                <p style="margin:8px 0 0 0;color:#555;">Vous pouvez investir dans d'autres projets disponibles sur la plateforme.</p>
                              </div>

                              <p>Si vous avez des questions, n'hésitez pas à nous contacter.</p>
                              <br>
                              <p style="color:#555;">Cordialement,<br><strong>L'équipe GrowzApp</strong></p>

                              <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                              <p style="font-size:0.78em;color:#999;text-align:center;">
                                GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire — contact@growzapp.ci
                              </p>
                            </div>
                            """
                            .formatted(nomInvestisseur, projetLibelle, motif, montant),
                    true);

            mailSender.send(message);
            log.info("Email refus investissement envoyé à {}", emailInvestisseur);

        } catch (Exception e) {
            log.error("Échec envoi email refus investissement à {} : {}", emailInvestisseur, e.getMessage(), e);
        }
    }

    @Async
    public void envoyerVersementPorteur(
            String emailInvestisseur,
            String nomInvestisseur,
            String projetLibelle,
            String montant,
            String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(emailInvestisseur);
            helper.setSubject("Versement effectué — " + projetLibelle + " — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;margin:0 0 4px 0;">GrowzApp</h1>
                              <p style="color:#888;font-size:0.85em;margin:0 0 24px 0;">Plateforme d'investissement participatif</p>

                              <h2 style="color:#1B5E20;">💸 Versement effectué</h2>

                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Nous vous informons qu'un versement a été effectué depuis le wallet du projet <strong>%s</strong> dans lequel vous avez investi.</p>

                              <div style="background:#f1f8e9;border-left:4px solid #1B5E20;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;font-weight:bold;color:#1B5E20;">Détails du versement :</p>
                                <p style="margin:8px 0 0 0;color:#333;">💰 Montant : <strong>%s FCFA</strong></p>
                                <p style="margin:8px 0 0 0;color:#333;">📋 Motif : %s</p>
                              </div>

                              <p style="color:#555;">Ce versement concerne la gestion courante du projet. Pour plus de détails, connectez-vous à votre espace GrowzApp.</p>
                              <br>
                              <p style="color:#555;">Cordialement,<br><strong>L'équipe GrowzApp</strong></p>

                              <hr style="border:none;border-top:1px solid #eee;margin:24px 0;">
                              <p style="font-size:0.78em;color:#999;text-align:center;">
                                GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire — contact@growzapp.ci
                              </p>
                            </div>
                            """
                            .formatted(nomInvestisseur, projetLibelle, montant, motif),
                    true);

            mailSender.send(message);
            log.info("Email versement porteur envoyé à {}", emailInvestisseur);

        } catch (Exception e) {
            log.error("Échec envoi email versement porteur à {} : {}", emailInvestisseur, e.getMessage(), e);
        }
    }

}