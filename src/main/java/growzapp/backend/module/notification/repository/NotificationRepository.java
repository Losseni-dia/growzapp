package growzapp.backend.module.notification.repository;


import growzapp.backend.module.notification.dto.NotificationAdminDTO;
import growzapp.backend.module.notification.model.Notification;
import growzapp.backend.module.user.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
                SELECT NEW growzapp.backend.module.notification.dto.NotificationAdminDTO(
                    n.id, n.title, n.content, n.date, n.isRead, n.motif,
                    CONCAT(u.prenom, ' ', u.nom), u.email)
                FROM Notification n
                JOIN n.recipient u
                WHERE (CAST(:search AS string) IS NULL OR :search = '' OR
                       LOWER(u.prenom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                       LOWER(u.nom) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                       LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR
                       LOWER(n.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
                  AND (:isRead IS NULL OR n.isRead = :isRead)
                ORDER BY n.date DESC
            """)
    Page<NotificationAdminDTO> findForAdmin(
            @Param("search") String search,
            @Param("isRead") Boolean isRead,
            Pageable pageable);


    // Récupère uniquement les notifications non lues pour le badge de la cloche
    List<Notification> findByRecipientAndIsReadFalseOrderByDateDesc(User recipient);

    // Compte le nombre de notifications non lues (très utile pour le badge du
    // Header)

    List<Notification> findByRecipientOrderByDateDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    List<Notification> findByRecipientAndIsReadFalse(User recipient);
}
