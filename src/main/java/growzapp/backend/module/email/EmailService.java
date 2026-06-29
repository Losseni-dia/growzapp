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

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // ── FACTURE DIVIDENDE ─────────────────────────────────────────────────────
    public void envoyerFactureParEmail(Facture facture, byte[] pdfBytes) {
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
                    <p>Merci pour votre confiance dans l'agriculture durable.</p>
                    <p><strong>L'équipe GrowzApp</strong></p>
                    """.formatted(nomInvestisseur, dividende.getMontantTotal()), true);
            helper.addAttachment("Facture_Dividende_" + dividende.getId() + ".pdf",
                    new ByteArrayResource(pdfBytes));
            mailSender.send(message);
            log.info("Email facture envoyé à {}", emailInvestisseur);
        } catch (MessagingException e) {
            log.error("Échec envoi email facture {} : {}", facture.getId(), e.getMessage(), e);
        }
    }

    // ── TEST EMAIL ────────────────────────────────────────────────────────────
    @Async
    public void envoyerEmailTestDirect(String emailDestinataire) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailDestinataire);
            helper.setSubject("Test réussi – GrowzApp");
            helper.setText("""
                    <h2>Félicitations !</h2>
                    <p>Ton système d'envoi de factures par email fonctionne à 100 %</p>
                    <p>L'équipe GrowzApp</p>
                    """, true);
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── CONTRAT INVESTISSEMENT ────────────────────────────────────────────────
    @Async
    public void envoyerContratParEmail(Investissement investissement, byte[] pdfContrat) {
        try {
            String emailInvestisseur = investissement.getInvestisseur().getEmail();
            String nomInvestisseur = investissement.getInvestisseur().getPrenom() + " "
                    + investissement.getInvestisseur().getNom();
            String projetLibelle = investissement.getProjet().getLibelle();
            String numeroContrat = investissement.getContrat().getNumeroContrat();
            String lienVerification = investissement.getContrat().getLienVerification();
            String emailPorteur = investissement.getProjet().getPorteur().getEmail();

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
                            <p style="text-align:center;margin:20px 0;">
                                <a href="%s" style="background:#007bff;color:white;padding:12px 28px;text-decoration:none;border-radius:8px;font-weight:bold;">
                                    Vérifier le contrat
                                </a>
                            </p>
                            <p>Merci pour votre confiance.</p>
                            <p><strong>L'équipe GrowzApp</strong></p>
                            """
                            .formatted(nomInvestisseur, projetLibelle, lienVerification),
                    true);
            helper.addAttachment("Contrat_" + numeroContrat + ".pdf", new ByteArrayResource(pdfContrat));
            mailSender.send(message);
            log.info("Email contrat envoyé à {}", emailInvestisseur);
        } catch (Exception e) {
            log.error("Échec envoi email contrat : {}", e.getMessage(), e);
        }
    }

    // ── RAPPORT MENSUEL ───────────────────────────────────────────────────────
    @Async
    public void envoyerRapportMensuel(String emailAdmin, YearMonth mois, byte[] pdf) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailAdmin);
            helper.setSubject("Rapport mensuel des investissements – " + mois);
            helper.setText("""
                    <h2>Rapport mensuel généré</h2>
                    <p>Voici le rapport complet des investissements validés pour le mois de <strong>%s</strong>.</p>
                    <p>Vous trouverez ci-joint le document PDF.</p>
                    <p><strong>GrowzApp Bot</strong></p>
                    """.formatted(mois.format(
                    java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.FRENCH))),
                    true);
            helper.addAttachment("Rapport_Investissements_" + mois + ".pdf", new ByteArrayResource(pdf));
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── MOT DE PASSE OUBLIÉ ───────────────────────────────────────────────────
    @Async
    public void sendPasswordResetMail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            String resetLink = frontendUrl + "/reset-password?token=" + token;
            helper.setTo(to);
            helper.setSubject("GrowzApp – Réinitialisation de votre mot de passe");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;color:#333;max-width:600px;margin:auto;border:1px solid #eee;padding:20px;border-radius:10px;">
                                <h1 style="color:#28a745;">GrowzApp</h1>
                                <h2>Bonjour,</h2>
                                <p>Nous avons reçu une demande de réinitialisation de mot de passe.</p>
                                <div style="text-align:center;margin:35px 0;">
                                    <a href="%s" style="background:#28a745;color:white;padding:14px 30px;text-decoration:none;border-radius:8px;font-weight:bold;">
                                        Réinitialiser mon mot de passe
                                    </a>
                                </div>
                                <p style="font-size:0.9em;color:#666;">Ce lien est valable 24 heures.</p>
                                <p style="font-size:0.8em;color:#999;text-align:center;">&copy; 2024 GrowzApp</p>
                            </div>
                            """
                            .formatted(resetLink),
                    true);
            mailSender.send(message);
            log.info("Email de réinitialisation envoyé avec succès");
        } catch (MessagingException e) {
            log.error("Échec envoi email reset à {} : {}", to, e.getMessage());
        }
    }

    // ── REFUS INVESTISSEMENT ──────────────────────────────────────────────────
    @Async
    public void envoyerRefusInvestissement(
            String emailInvestisseur, String nomInvestisseur,
            String projetLibelle, String montant, String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailInvestisseur);
            helper.setSubject("Investissement refusé — " + projetLibelle + " — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;">GrowzApp</h1>
                              <h2 style="color:#c62828;">Investissement refusé</h2>
                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Votre investissement dans le projet <strong>%s</strong> a été refusé.</p>
                              <div style="background:#fff5f5;border-left:4px solid #c62828;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;font-weight:bold;color:#c62828;">Motif du refus :</p>
                                <p style="margin:8px 0 0 0;color:#333;">%s</p>
                              </div>
                              <div style="background:#f1f8e9;border-radius:8px;padding:16px;margin:20px 0;">
                                <p style="margin:0;color:#555;">💰 <strong>%s FCFA</strong> ont été restitués dans votre portefeuille GrowzApp.</p>
                              </div>
                              <p>Cordialement,<br><strong>L'équipe GrowzApp</strong></p>
                              <p style="font-size:0.78em;color:#999;text-align:center;">GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire</p>
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

    // ── VERSEMENT PORTEUR ─────────────────────────────────────────────────────
    @Async
    public void envoyerVersementPorteur(
            String emailInvestisseur, String nomInvestisseur,
            String projetLibelle, String montant, String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(emailInvestisseur);
            helper.setSubject("Versement effectué — " + projetLibelle + " — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;">GrowzApp</h1>
                              <h2 style="color:#1B5E20;">💸 Versement effectué</h2>
                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Un versement a été effectué depuis le wallet du projet <strong>%s</strong>.</p>
                              <div style="background:#f1f8e9;border-left:4px solid #1B5E20;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;color:#333;">💰 Montant : <strong>%s FCFA</strong></p>
                                <p style="margin:8px 0 0 0;color:#333;">📋 Motif : %s</p>
                              </div>
                              <p>Cordialement,<br><strong>L'équipe GrowzApp</strong></p>
                              <p style="font-size:0.78em;color:#999;text-align:center;">GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire</p>
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

    // ── KYC VALIDÉ ────────────────────────────────────────────────────────────
    @Async
    public void envoyerKycValide(String email, String nomComplet) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("✅ Votre identité a été validée — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;">GrowzApp</h1>
                              <h2 style="color:#1B5E20;">✅ Identité vérifiée !</h2>
                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Votre dossier KYC a été examiné et <strong>validé</strong> par notre équipe.</p>
                              <div style="background:#f1f8e9;border-left:4px solid #1B5E20;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;color:#333;">🎉 Vous pouvez maintenant <strong>investir</strong> sur GrowzApp.</p>
                              </div>
                              <p>Cordialement,<br><strong>L'équipe GrowzApp</strong></p>
                              <p style="font-size:0.78em;color:#999;text-align:center;">GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire</p>
                            </div>
                            """
                            .formatted(nomComplet),
                    true);
            mailSender.send(message);
            log.info("Email KYC validé envoyé à {}", email);
        } catch (Exception e) {
            log.error("Échec envoi email KYC validé à {} : {}", email, e.getMessage());
        }
    }

    // ── KYC REFUSÉ ────────────────────────────────────────────────────────────
    @Async
    public void envoyerKycRefuse(String email, String nomComplet, String motif) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("❌ Dossier KYC refusé — GrowzApp");
            helper.setText(
                    """
                            <div style="font-family:'Segoe UI',sans-serif;max-width:600px;margin:auto;border:1px solid #eee;padding:24px;border-radius:12px;">
                              <h1 style="color:#1B5E20;">GrowzApp</h1>
                              <h2 style="color:#c62828;">❌ Dossier KYC refusé</h2>
                              <p>Bonjour <strong>%s</strong>,</p>
                              <p>Votre dossier KYC n'a pas pu être validé.</p>
                              <div style="background:#fff5f5;border-left:4px solid #c62828;padding:16px;border-radius:0 8px 8px 0;margin:20px 0;">
                                <p style="margin:0;font-weight:bold;color:#c62828;">Motif du refus :</p>
                                <p style="margin:8px 0 0 0;color:#333;">%s</p>
                              </div>
                              <p>Vous pouvez soumettre un nouveau dossier depuis votre profil.</p>
                              <p>Cordialement,<br><strong>L'équipe GrowzApp</strong></p>
                              <p style="font-size:0.78em;color:#999;text-align:center;">GrowzApp S.A.R.L — Abidjan, Côte d'Ivoire</p>
                            </div>
                            """
                            .formatted(nomComplet, motif),
                    true);
            mailSender.send(message);
            log.info("Email KYC refusé envoyé à {}", email);
        } catch (Exception e) {
            log.error("Échec envoi email KYC refusé à {} : {}", email, e.getMessage());
        }
    }
}