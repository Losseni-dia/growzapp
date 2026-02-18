package growzapp.backend.notification.controller;

import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.entite.User;
import growzapp.backend.notification.model.Notification;
import growzapp.backend.notification.service.NotificationService;
import growzapp.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ApiResponseDTO<List<Notification>> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        // On retourne l'objet standard de ton appli : ApiResponseDTO
        return ApiResponseDTO.success(notifications);
    }

    @GetMapping("/unread-count")
    public ApiResponseDTO<Long> getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        return ApiResponseDTO.success(notificationService.getUnreadCount(currentUser));
    }

    @PatchMapping("/{id}/read")
    public ApiResponseDTO<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        // On précise <Void> avant l'appel de la méthode success
        return ApiResponseDTO.<Void>success(null).message("Notification marquée comme lue");
    }
}