package growzapp.backend.module.contact.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.contact.dto.ContactMessageCreateDTO;
import growzapp.backend.module.contact.dto.ContactMessageDTO;
import growzapp.backend.module.contact.dto.ContactReplyCreateDTO;
import growzapp.backend.module.contact.model.ContactMessage;
import growzapp.backend.module.contact.service.ContactService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Tag(name = "Contact / Support")
public class ContactController {

    private final ContactService contactService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @PostMapping
    @Operation(summary = "Démarrer un nouveau fil de contact/support")
    public ApiResponseDTO<ContactMessageDTO> envoyer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ContactMessageCreateDTO dto) {
        User user = getCurrentUser(userDetails);
        ContactMessage saved = contactService.creerMessage(user, dto);
        return ApiResponseDTO.success(contactService.toDto(saved));
    }

    @PostMapping("/{id}/repondre")
    @Operation(summary = "Continuer un fil existant (sans nouveau sujet)")
    public ApiResponseDTO<ContactMessageDTO> repondre(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyCreateDTO dto) {
        User user = getCurrentUser(userDetails);
        ContactMessage saved = contactService.ajouterMessageUtilisateur(id, user, dto.message());
        return ApiResponseDTO.success(contactService.toDto(saved));
    }

    @GetMapping("/mes-messages")
    @Operation(summary = "Consulter mes fils de contact/support et les réponses reçues")
    public ApiResponseDTO<List<ContactMessageDTO>> mesMessages(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<ContactMessageDTO> dtos = contactService.getMesMessages(user.getId()).stream()
                .map(contactService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Masquer un fil de mon côté", description = "Le fil disparaît uniquement de ma propre liste — il reste visible et intact côté admin.")
    public ApiResponseDTO<String> masquer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        contactService.masquerPourUtilisateur(id, user);
        return ApiResponseDTO.<String>success(null).message("Message masqué");
    }
}
