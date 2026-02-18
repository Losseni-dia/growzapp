package growzapp.backend.notification.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.Projet;
import growzapp.backend.model.entite.User;
import growzapp.backend.notification.model.Notification;
import growzapp.backend.notification.repository.NotificationRepository;
import growzapp.backend.repository.UserRepository;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    // --- LOGIQUE D'ENVOI ---

    public void notifyProjectOwner(User owner, String title, String content) {
        Notification notif = new Notification();
        notif.setRecipient(owner);
        notif.setTitle(title);
        notif.setContent(content);
        notificationRepository.save(notif);
    }

    // NotificationService.java

    // Pour tous les utilisateurs sans exception (Diffusion globale)
    public void notifyAllUsers(String title, String content) {
        List<User> allUsers = userRepository.findAll(); // On récupère tout le monde
        allUsers.forEach(user -> {
            Notification notif = new Notification();
            notif.setRecipient(user);
            notif.setTitle(title);
            notif.setContent(content);
            notificationRepository.save(notif);
        });
    }

    // NotificationService.java

    /**
     * Notifie tous les investisseurs qui ont déjà misé sur ce projet
     * qu'un nouvel investissement vient d'avoir lieu.
     */
    public void notifyExistingInvestors(Projet project, Double newAmount) {
        // On récupère tous les investissements du projet
        // Puis on extrait les utilisateurs uniques (pour ne pas notifier 2 fois la même
        // personne)
        project.getInvestissements().stream()
                .map(Investissement::getUser) // On récupère l'investisseur
                .distinct() // On évite les doublons si quelqu'un a investi plusieurs fois
                .forEach(user -> {
                    Notification notif = new Notification();
                    notif.setRecipient(user);
                    notif.setTitle("Le projet avance !");
                    notif.setContent("Un nouvel investissement de " + newAmount + "€ vient d'être réalisé sur "
                            + project.getNom() + ".");
                    notificationRepository.save(notif);
                });
    }

    // --- LOGIQUE DE LECTURE (Pour le Controller) ---

    public List<Notification> getNotificationsForUser(User user) {
        // Utilise la méthode du repository pour trier par date décroissante
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