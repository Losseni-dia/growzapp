package growzapp.backend.notification.controller;

import growzapp.backend.model.entite.User;
import growzapp.backend.notification.model.Notification;
import growzapp.backend.notification.service.NotificationService;
import growzapp.backend.service.UserService; // Ton service de gestion d'utilisateurs
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    // Récupère toutes les notifications de l'utilisateur actuel
    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications() {
        User currentUser = userService.getCurrentUser(); // Récupère l'utilisateur via le SecurityContext
        return ResponseEntity.ok(notificationService.getNotificationsForUser(currentUser));
    }

    // Récupère uniquement le compte des non lues (pour optimiser le Header)
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        return ResponseEntity.ok(notificationService.getUnreadCount(currentUser));
    }

    // Marque une notification comme lue au clic
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.noContent().build();
    }
}