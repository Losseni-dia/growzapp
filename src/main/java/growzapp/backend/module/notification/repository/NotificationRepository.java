package growzapp.backend.module.notification.repository;


import growzapp.backend.module.notification.model.Notification;
import growzapp.backend.module.user.model.User;

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
