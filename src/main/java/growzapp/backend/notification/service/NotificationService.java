package growzapp.backend.notification.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public void notifyAllInvestors(String title, String content) {
        // Assure-toi que cette méthode existe dans ton UserRepository
        List<User> investors = userRepository.findByRole("INVESTOR");
        investors.forEach(user -> {
            Notification notif = new Notification();
            notif.setRecipient(user);
            notif.setTitle(title);
            notif.setContent(content);
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