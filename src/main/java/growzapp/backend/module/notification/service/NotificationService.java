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
        allUsers.forEach(user -> {
            Notification notif = buildNotif(user, title, content, projetId, projetSlug);
            notificationRepository.save(notif);
        });
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
}