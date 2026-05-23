package growzapp.backend.module.notification.controller;

import growzapp.backend.module.notification.model.Notification;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Notifications", description = "Notifications en temps réel de l'utilisateur connecté")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Mes notifications", description = "Retourne toutes les notifications de l'utilisateur connecté, triées du plus récent au plus ancien.", tags = {"Notifications"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des notifications",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<List<Notification>> getMyNotifications() {
        User currentUser = userService.getCurrentUser();
        List<Notification> notifications = notificationService.getNotificationsForUser(currentUser);
        return ApiResponseDTO.success(notifications);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Nombre de notifications non lues", tags = {"Notifications"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Compteur de notifications non lues",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Long> getUnreadCount() {
        User currentUser = userService.getCurrentUser();
        return ApiResponseDTO.success(notificationService.getUnreadCount(currentUser));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue", tags = {"Notifications"})
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification marquée comme lue",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Non authentifié",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Notification introuvable",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Void> markAsRead(
            @Parameter(description = "Identifiant de la notification", example = "42", required = true)
            @PathVariable Long id) {
        notificationService.markAsRead(id);
        return ApiResponseDTO.<Void>success(null).message("Notification marquée comme lue");
    }
}
