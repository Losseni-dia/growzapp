package growzapp.backend.module.contact.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.contact.enums.StatutContact;
import growzapp.backend.module.contact.model.ContactMessage;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    List<ContactMessage> findByUserIdOrderByDateEnvoiDesc(Long userId);

    List<ContactMessage> findAllByOrderByDateEnvoiDesc();

    List<ContactMessage> findByStatutOrderByDateEnvoiDesc(StatutContact statut);
}
