package growzapp.backend.module.contact.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import growzapp.backend.module.contact.dto.ContactMessageCreateDTO;
import growzapp.backend.module.contact.dto.ContactMessageDTO;
import growzapp.backend.module.contact.dto.ContactReplyDTO;
import growzapp.backend.module.contact.enums.StatutContact;
import growzapp.backend.module.contact.model.ContactMessage;
import growzapp.backend.module.contact.model.ContactReply;
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

    private ContactMessage getThreadOrThrow(Long id) {
        return contactMessageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Message introuvable avec l'ID : " + id));
    }

    private void ensureProprietaire(ContactMessage msg, User user) {
        if (!msg.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Ce fil de discussion ne vous appartient pas.");
        }
    }

    // ── Continuer le fil, côté utilisateur (pas de nouveau sujet) ────────────
    @Transactional
    public ContactMessage ajouterMessageUtilisateur(Long id, User user, String contenu) {
        ContactMessage msg = getThreadOrThrow(id);
        ensureProprietaire(msg, user);

        ContactReply reply = new ContactReply();
        reply.setContactMessage(msg);
        reply.setAuteur(user);
        reply.setAdmin(false);
        reply.setContenu(contenu);
        msg.getReponses().add(reply);

        // Un nouveau message de l'utilisateur relance le fil côté admin :
        // redevient "à traiter" et réapparaît si l'admin l'avait masqué.
        msg.setStatut(StatutContact.NOUVEAU);
        msg.setHiddenForAdmin(false);
        ContactMessage saved = contactMessageRepository.save(msg);

        notificationService.notifyAdmins(
                "Nouveau message de contact",
                (user.getPrenom() + " " + user.getNom()).trim() + " — " + msg.getSujet(),
                "/admin/contact");

        return saved;
    }

    // ── Répondre, côté admin ──────────────────────────────────────────────────
    @Transactional
    public ContactMessage repondre(Long id, String contenu, User admin) {
        ContactMessage msg = getThreadOrThrow(id);

        ContactReply reply = new ContactReply();
        reply.setContactMessage(msg);
        reply.setAuteur(admin);
        reply.setAdmin(true);
        reply.setContenu(contenu);
        msg.getReponses().add(reply);

        msg.setStatut(StatutContact.TRAITE);
        // Une réponse de l'admin doit redevenir visible pour l'utilisateur,
        // même s'il avait masqué ce fil de son côté auparavant.
        msg.setHiddenForUser(false);
        ContactMessage saved = contactMessageRepository.save(msg);

        String destinataire = resolveEmail(msg.getUser());
        if (destinataire != null) {
            emailService.envoyerReponseContact(
                    destinataire,
                    msg.getUser().getPrenom() + " " + msg.getUser().getNom(),
                    msg.getSujet(),
                    msg.getMessage(),
                    contenu);
        }

        notificationService.notifyUser(
                msg.getUser(),
                "Réponse à votre message",
                msg.getSujet(),
                null,
                "/mon-espace/contact");

        return saved;
    }

    // ── Masquage indépendant par côté (jamais une vraie suppression) ─────────
    @Transactional
    public void masquerPourUtilisateur(Long id, User user) {
        ContactMessage msg = getThreadOrThrow(id);
        ensureProprietaire(msg, user);
        msg.setHiddenForUser(true);
        contactMessageRepository.save(msg);
    }

    @Transactional
    public void masquerPourAdmin(Long id) {
        ContactMessage msg = getThreadOrThrow(id);
        msg.setHiddenForAdmin(true);
        contactMessageRepository.save(msg);
    }

    public List<ContactMessage> getMesMessages(Long userId) {
        return contactMessageRepository.findByUserIdAndHiddenForUserFalseOrderByDateEnvoiDesc(userId);
    }

    public List<ContactMessage> getAll(StatutContact statutFiltre) {
        return statutFiltre != null
                ? contactMessageRepository.findByStatutAndHiddenForAdminFalseOrderByDateEnvoiDesc(statutFiltre)
                : contactMessageRepository.findByHiddenForAdminFalseOrderByDateEnvoiDesc();
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
        List<ContactReplyDTO> reponses = msg.getReponses().stream()
                .map(r -> new ContactReplyDTO(
                        r.getId(),
                        r.getAuteur().getId(),
                        (r.getAuteur().getPrenom() + " " + r.getAuteur().getNom()).trim(),
                        r.isAdmin(),
                        r.getContenu(),
                        r.getDateEnvoi()))
                .toList();

        return new ContactMessageDTO(
                msg.getId(),
                msg.getUser().getId(),
                (msg.getUser().getPrenom() + " " + msg.getUser().getNom()).trim(),
                resolveEmail(msg.getUser()),
                msg.getSujet(),
                msg.getMessage(),
                msg.getStatut().name(),
                msg.getDateEnvoi(),
                reponses);
    }
}
