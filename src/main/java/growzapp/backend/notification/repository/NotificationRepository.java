package growzapp.backend.notification.repository;


import growzapp.backend.model.entite.User;
import growzapp.backend.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    // Récupère uniquement les notifications non lues pour le badge de la cloche
    List<Notification> findByRecipientAndIsReadFalseOrderByDateDesc(User recipient);

    // Compte le nombre de notifications non lues (très utile pour le badge du
    // Header)

    List<Notification> findByRecipientOrderByDateDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);
}
