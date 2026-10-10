package growzapp.backend.module.projet.service;

import growzapp.backend.module.email.EmailService;
import growzapp.backend.module.investissement.service.InvestissementService;
import growzapp.backend.module.notification.service.NotificationService;
import growzapp.backend.module.projet.dto.InvestisseurSimpleDTO;
import growzapp.backend.module.projet.dto.ProjetMessageDTO;
import growzapp.backend.module.projet.model.Projet;
import growzapp.backend.module.projet.model.ProjetMessage;
import growzapp.backend.module.projet.repository.ProjetMessageRepository;
import growzapp.backend.module.user.model.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Canal de messagerie par projet entre admin/communicant et investisseurs,
// porteur strictement exclu (confirmé explicitement) — admin peut diffuser
// à tous les investisseurs du projet ou cibler un sous-ensemble.
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ProjetMessageService {

    private final ProjetMessageRepository projetMessageRepository;
    private final ProjetService projetService;
    private final InvestissementService investissementService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public List<ProjetMessageDTO> listerPourAdmin(Long projetId) {
        return projetMessageRepository.findByProjetIdOrderByDateEnvoiAsc(projetId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<InvestisseurSimpleDTO> listerDestinataires(Long projetId) {
        return investissementService.getInvestisseursDistinctsDuProjet(projetId).stream()
                .map(u -> new InvestisseurSimpleDTO(u.getId(), (u.getPrenom() + " " + u.getNom()).trim()))
                .toList();
    }

    // Visibilité calculée, pas stockée : un investisseur voit un message
    // s'il en est l'auteur, ou si l'auteur est ADMIN et (destinataireIds
    // vide OU son id y figure).
    public List<ProjetMessageDTO> listerPourInvestisseur(Long projetId, Long investisseurId) {
        boolean aInvesti = investissementService.getInvestisseursDistinctsDuProjet(projetId).stream()
                .anyMatch(u -> u.getId().equals(investisseurId));
        if (!aInvesti) {
            throw new AccessDeniedException("Vous n'avez jamais investi dans ce projet.");
        }

        return projetMessageRepository.findByProjetIdOrderByDateEnvoiAsc(projetId).stream()
                .filter(m -> {
                    if (m.getAuteur().getId().equals(investisseurId)) {
                        return true;
                    }
                    if (!"ADMIN".equals(m.getRole())) {
                        return false;
                    }
                    return m.getDestinataireIds().isEmpty() || m.getDestinataireIds().contains(investisseurId);
                })
                .map(this::toDto)
                .toList();
    }

    public ProjetMessageDTO envoyerParAdmin(Long projetId, User auteur, String contenu,
            String destinataireType, List<Long> destinataireIdsBrut) {
        Projet projet = projetService.getById(projetId);

        Set<Long> destinataireIds = new HashSet<>();
        if ("CIBLES".equals(destinataireType)) {
            if (destinataireIdsBrut == null || destinataireIdsBrut.isEmpty()) {
                throw new IllegalArgumentException("Au moins un destinataire est requis pour un envoi ciblé.");
            }
            destinataireIds.addAll(destinataireIdsBrut);
        }

        ProjetMessage message = new ProjetMessage();
        message.setProjet(projet);
        message.setAuteur(auteur);
        message.setRole("ADMIN");
        message.setContenu(contenu);
        message.setDestinataireIds(destinataireIds);
        ProjetMessage saved = projetMessageRepository.save(message);

        List<User> tousLesInvestisseurs = investissementService.getInvestisseursDistinctsDuProjet(projetId);
        List<User> destinataires = destinataireIds.isEmpty()
                ? tousLesInvestisseurs
                : tousLesInvestisseurs.stream().filter(u -> destinataireIds.contains(u.getId())).toList();

        // Chemin absolu plutôt que le slug du projet : un clic sur la
        // notification doit ouvrir directement le fil de discussion pour
        // que l'investisseur puisse répondre, pas la fiche projet.
        String cheminConversation = "/projets/" + projetId + "/messages";
        String titre = "💬 Nouveau message — " + projet.getLibelle();
        if (destinataireIds.isEmpty()) {
            notificationService.notifyInvestorsOfProject(projet, titre, contenu, cheminConversation);
        } else {
            notificationService.notifySelectedInvestors(destinataires, titre, contenu, projet.getId(), cheminConversation);
        }
        for (User investisseur : destinataires) {
            emailService.envoyerMessageProjet(
                    investisseur.getEmail(),
                    (investisseur.getPrenom() + " " + investisseur.getNom()).trim(),
                    projet.getLibelle(),
                    (auteur.getPrenom() + " " + auteur.getNom()).trim(),
                    contenu,
                    true);
        }

        log.info("ProjetMessageService.envoyerParAdmin : message #{} envoyé sur le projet {} à {} investisseur(s)",
                saved.getId(), projetId, destinataires.size());

        return toDto(saved);
    }

    public ProjetMessageDTO envoyerParInvestisseur(Long projetId, User investisseur, String contenu) {
        boolean aInvesti = investissementService.getInvestisseursDistinctsDuProjet(projetId).stream()
                .anyMatch(u -> u.getId().equals(investisseur.getId()));
        if (!aInvesti) {
            throw new AccessDeniedException("Vous n'avez jamais investi dans ce projet.");
        }

        Projet projet = projetService.getById(projetId);

        ProjetMessage message = new ProjetMessage();
        message.setProjet(projet);
        message.setAuteur(investisseur);
        message.setRole("INVESTISSEUR");
        message.setContenu(contenu);
        message.setDestinataireIds(new HashSet<>());
        ProjetMessage saved = projetMessageRepository.save(message);

        String nomInvestisseur = (investisseur.getPrenom() + " " + investisseur.getNom()).trim();
        notificationService.notifyAdmins(
                "💬 Message investisseur — " + projet.getLibelle(),
                nomInvestisseur + " a écrit sur le projet « " + projet.getLibelle() + " ».",
                "/admin/projets/" + projetId + "#messages-investisseurs");

        log.info("ProjetMessageService.envoyerParInvestisseur : message #{} envoyé sur le projet {} par l'investisseur {}",
                saved.getId(), projetId, investisseur.getId());

        return toDto(saved);
    }

    private ProjetMessageDTO toDto(ProjetMessage m) {
        return new ProjetMessageDTO(
                m.getId(),
                m.getAuteur().getId(),
                (m.getAuteur().getPrenom() + " " + m.getAuteur().getNom()).trim(),
                m.getRole(),
                m.getContenu(),
                m.getDestinataireIds() != null ? m.getDestinataireIds() : Set.of(),
                m.getDateEnvoi());
    }
}
