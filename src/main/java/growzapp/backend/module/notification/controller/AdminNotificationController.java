package growzapp.backend.module.notification.controller;

import growzapp.backend.module.notification.dto.NotificationAdminDTO;
import growzapp.backend.module.notification.repository.NotificationRepository;
import growzapp.backend.module.shared.ApiResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/notifications", "/api/admin/notifications"})
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Notifications", description = "Consultation en lecture seule de toutes les notifications envoyées sur la plateforme")
public class AdminNotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    @Operation(
        summary = "Lister toutes les notifications (paginé)",
        description = "Retourne la liste paginée de toutes les notifications envoyées, tous destinataires confondus. Réservé aux administrateurs.",
        tags = {"Admin - Notifications"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Page de notifications retournée",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "403", description = "Accès refusé — rôle ADMIN requis",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<Page<NotificationAdminDTO>> getAll(
            @Parameter(description = "Numéro de page (commence à 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Nombre d'éléments par page", example = "20")
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ApiResponseDTO.success(notificationRepository.findAllForAdmin(pageable));
    }
}
