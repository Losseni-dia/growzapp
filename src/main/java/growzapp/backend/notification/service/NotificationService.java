package growzapp.backend.notification.service;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.model.entite.Investissement;
import growzapp.backend.model.entite.User;
import growzapp.backend.module.projet.model.Projet;
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

    /**
     * Notifie le porteur de projet (avec lien vers le projet)
     */
    public void notifyProjectOwner(User owner, String title, String content, Long projetId) {
        Notification notif = new Notification();
        notif.setRecipient(owner);
        notif.setTitle(title);
        notif.setContent(content);
        notif.setProjetId(projetId); // Ajout de l'ID pour redirection
        notificationRepository.save(notif);
    }

    /**
     * Notification globale (projetId peut être null si c'est une info générale)
     */
    public void notifyAllUsers(String title, String content, Long projetId) {
        List<User> allUsers = userRepository.findAll();
        allUsers.forEach(user -> {
            Notification notif = new Notification();
            notif.setRecipient(user);
            notif.setTitle(title);
            notif.setContent(content);
            notif.setProjetId(projetId); // Ajout de l'ID si fourni
            notificationRepository.save(notif);
        });
    }

    /**
     * Notifie les investisseurs d'un projet spécifique
     */
    public void notifyExistingInvestors(Projet project, BigDecimal newAmount, User currentInvestor) {
        if (project.getInvestissements() == null)
            return;

        project.getInvestissements().stream()
                .map(Investissement::getInvestisseur)
                .distinct()
                .filter(user -> !user.getId().equals(currentInvestor.getId()))
                .forEach(user -> {
                    Notification notif = new Notification();
                    notif.setRecipient(user);
                    notif.setTitle("Le projet avance !");
                    notif.setContent("Un nouvel investissement de " + newAmount + " FCFA vient d'être réalisé sur "
                            + project.getLibelle() + ".");

                    // CRUCIAL : On lie l'ID du projet pour la redirection frontend
                    notif.setProjetId(project.getId());

                    notificationRepository.save(notif);
                });
    }

    // --- LOGIQUE DE LECTURE ---

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