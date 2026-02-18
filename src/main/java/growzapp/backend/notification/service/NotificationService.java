package growzapp.backend.notification.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import growzapp.backend.model.entite.User;
import growzapp.backend.notification.model.Notification;
import growzapp.backend.repository.UserRepository;

@Service
public class NotificationService {
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;

    // Pour le porteur de projet (Un seul destinataire)
    public void notifyProjectOwner(User owner, String title, String content) {
        Notification notif = new Notification();
        notif.setRecipient(owner);
        notif.setTitle(title);
        notif.setContent(content);
        notificationRepository.save(notif);
    }

    // Pour tous les investisseurs (Diffusion large)
    public void notifyAllInvestors(String title, String content) {
        List<User> investors = userRepository.findByRole("INVESTOR");
        investors.forEach(user -> {
            Notification notif = new Notification();
            notif.setRecipient(user);
            notif.setTitle(title);
            notif.setContent(content);
            notificationRepository.save(notif);
        });
    }
}