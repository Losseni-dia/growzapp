package growzapp.backend.module.notification.mapper;

import growzapp.backend.module.notification.dto.NotificationDTO;
import growzapp.backend.module.notification.model.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Empêche toute fuite accidentelle de champ sensible via la relation
 * recipient (User) — sans DTO dédié, le contrôleur renvoyait directement
 * l'entité JPA au client (JAVA-02 de l'audit).
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {
    NotificationDTO toDto(Notification notification);
}