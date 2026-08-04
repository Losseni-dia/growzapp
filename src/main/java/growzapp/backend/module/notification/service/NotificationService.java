package growzapp.backend.module.notification.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.investissement.model.Investissement;
import growzapp.backend.module.notification.model.Notification;
import growzapp.backend.module.notification.repository.NotificationRepository;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    // ── Helper pour créer une notification avec slug ──────────────────────────
    private Notification buildNotif(User recipient, String title, String content,
            Long projetId, String projetSlug) {
        return buildNotif(recipient, title, content, projetId, projetSlug, null);
    }

    private Notification buildNotif(User recipient, String title, String content,
            Long projetId, String projetSlug, String motif) {
        Notification notif = new Notification();
        notif.setRecipient(recipient);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setProjetId(projetId);
        notif.setProjetSlug(projetSlug);
        notif.setMotif(motif);
        return notif;
    }

    // ── Notifie un utilisateur spécifique avec slug ───────────────────────────
    public void notifyProjectOwner(User owner, String title, String content, Long projetId) {
        // Compatibilité sans slug (ancien code)
        Notification notif = buildNotif(owner, title, content, projetId, null);
        notificationRepository.save(notif);
    }

    // Nouvelle surcharge avec slug
    public void notifyUser(User user, String title, String content, Long projetId, String projetSlug) {
        notifyUser(user, title, content, projetId, projetSlug, null);
    }

    public void notifyUser(User user, String title, String content, Long projetId, String projetSlug, String motif) {
        Notification notif = buildNotif(user, title, content, projetId, projetSlug, motif);
        notificationRepository.save(notif);
    }

    // ── Notification globale ──────────────────────────────────────────────────
    public void notifyAllUsers(String title, String content, Long projetId) {
        notifyAllUsersWithSlug(title, content, projetId, null);
    }

    public void notifyAllUsersWithSlug(String title, String content, Long projetId, String projetSlug) {
        List<User> allUsers = userRepository.findAll();
        log.info("notifyAllUsersWithSlug : {} utilisateurs trouvés pour la diffusion « {} »",
                allUsers.size(), title);

        int succes = 0;
        for (User user : allUsers) {
            try {
                Notification notif = buildNotif(user, title, content, projetId, projetSlug);
                notificationRepository.save(notif);
                succes++;
            } catch (Exception e) {
                // Un enregistrement en échec (ex: donnée corrompue sur un seul
                // utilisateur) ne doit jamais interrompre la diffusion aux
                // autres, ni faire échouer la transaction appelante (ex:
                // validation du projet elle-même).
                log.error("notifyAllUsersWithSlug : échec pour l'utilisateur {} — {}",
                        user.getId(), e.getMessage(), e);
            }
        }
        log.info("notifyAllUsersWithSlug : {}/{} notifications enregistrées avec succès",
                succes, allUsers.size());
    }

    // ── Notifie les investisseurs existants d'un projet ───────────────────────
    public void notifyExistingInvestors(Projet project, BigDecimal newAmount, User currentInvestor) {
        if (project.getInvestissements() == null)
            return;

        project.getInvestissements().stream()
                .map(Investissement::getInvestisseur)
                .distinct()
                .filter(user -> !user.getId().equals(currentInvestor.getId()))
                .forEach(user -> {
                    Notification notif = buildNotif(
                            user,
                            "Le projet avance !",
                            "Un nouvel investissement de " + newAmount
                                    + " FCFA vient d'être réalisé sur " + project.getLibelle() + ".",
                            project.getId(),
                            project.getSlug());
                    notificationRepository.save(notif);
                });
    }

    // ── Notifie un utilisateur qu'une facture a été émise ─────────────────────
    public void notifyFactureEmise(User user, String title, String content, Long factureId) {
        Notification notif = new Notification();
        notif.setRecipient(user);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setFactureId(factureId);
        notificationRepository.save(notif);
    }

    // ── Lecture ───────────────────────────────────────────────────────────────
    public List<Notification> getNotificationsForUser(User user) {
        return notificationRepository.findByRecipientOrderByDateDesc(user);
    }

    public long getUnreadCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByRecipientAndIsReadFalse(user);
        unread.forEach(notif -> notif.setRead(true));
        notificationRepository.saveAll(unread);
    }
}