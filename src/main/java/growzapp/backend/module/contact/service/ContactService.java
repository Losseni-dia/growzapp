package growzapp.backend.module.contact.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.contact.dto.ContactMessageCreateDTO;
import growzapp.backend.module.contact.dto.ContactMessageDTO;
import growzapp.backend.module.contact.enums.StatutContact;
import growzapp.backend.module.contact.model.ContactMessage;
import growzapp.backend.module.contact.repository.ContactMessageRepository;
import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContactService {

    private final ContactMessageRepository contactMessageRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Transactional
    public ContactMessage creerMessage(User user, ContactMessageCreateDTO dto) {
        ContactMessage msg = new ContactMessage();
        msg.setUser(user);
        msg.setSujet(dto.sujet());
        msg.setMessage(dto.message());
        ContactMessage saved = contactMessageRepository.save(msg);

        // projetSlug accepte un chemin absolu ("/admin/...") en plus d'un
        // slug de projet — convention déjà utilisée par notifyAdmins() pour
        // pointer précisément vers l'écran d'action concerné.
        notificationService.notifyAdmins(
                "Nouveau message de contact",
                (user.getPrenom() + " " + user.getNom()).trim() + " — " + dto.sujet(),
                "/admin/contact");

        return saved;
    }

    public List<ContactMessage> getMesMessages(Long userId) {
        return contactMessageRepository.findByUserIdOrderByDateEnvoiDesc(userId);
    }

    public List<ContactMessage> getAll(StatutContact statutFiltre) {
        return statutFiltre != null
                ? contactMessageRepository.findByStatutOrderByDateEnvoiDesc(statutFiltre)
                : contactMessageRepository.findAllByOrderByDateEnvoiDesc();
    }

    @Transactional
    public ContactMessage repondre(Long id, String reponse, User admin) {
        ContactMessage msg = contactMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable avec l'ID : " + id));

        msg.setReponse(reponse);
        msg.setStatut(StatutContact.TRAITE);
        msg.setResponduPar(admin.getPrenom() + " " + admin.getNom());
        msg.setDateReponse(LocalDateTime.now());
        ContactMessage saved = contactMessageRepository.save(msg);

        String destinataire = resolveEmail(msg.getUser());
        if (destinataire != null) {
            emailService.envoyerReponseContact(
                    destinataire,
                    msg.getUser().getPrenom() + " " + msg.getUser().getNom(),
                    msg.getSujet(),
                    msg.getMessage(),
                    reponse);
        }

        notificationService.notifyUser(
                msg.getUser(),
                "Réponse à votre message",
                msg.getSujet(),
                null,
                "/mon-espace/contact");

        return saved;
    }

    // Même repli que FichePorteurController.toMap() : certains comptes n'ont
    // qu'un login au format email, sans email renseigné explicitement.
    private String resolveEmail(User user) {
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            return user.getEmail();
        }
        if (user.getLogin() != null && user.getLogin().contains("@")) {
            return user.getLogin();
        }
        return null;
    }

    public ContactMessageDTO toDto(ContactMessage msg) {
        return new ContactMessageDTO(
                msg.getId(),
                msg.getUser().getId(),
                msg.getUser().getPrenom() + " " + msg.getUser().getNom(),
                resolveEmail(msg.getUser()),
                msg.getSujet(),
                msg.getMessage(),
                msg.getStatut().name(),
                msg.getReponse(),
                msg.getResponduPar(),
                msg.getDateEnvoi(),
                msg.getDateReponse());
    }
}
