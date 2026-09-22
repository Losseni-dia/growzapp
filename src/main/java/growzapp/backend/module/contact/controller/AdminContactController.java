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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.contact.dto.ContactMessageDTO;
import growzapp.backend.module.contact.dto.ContactReponseDTO;
import growzapp.backend.module.contact.enums.StatutContact;
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
@RequestMapping("/api/admin/contact")
@RequiredArgsConstructor
@Tag(name = "Admin - Contact / Support")
public class AdminContactController {

    private final ContactService contactService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lister les messages de contact/support", description = "Filtrable par statut (NOUVEAU, TRAITE) — sans filtre, retourne tout, du plus récent au plus ancien.")
    public ApiResponseDTO<List<ContactMessageDTO>> getAll(
            @RequestParam(required = false) StatutContact statut) {
        List<ContactMessageDTO> dtos = contactService.getAll(statut).stream()
                .map(contactService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/repondre")
    @Operation(summary = "Répondre à un message de contact/support", description = "Enregistre la réponse, marque le message comme TRAITE et envoie la réponse par email à l'auteur du message.")
    public ApiResponseDTO<ContactMessageDTO> repondre(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody ContactReponseDTO dto) {
        User admin = userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        ContactMessage saved = contactService.repondre(id, dto.reponse(), admin);
        return ApiResponseDTO.success(contactService.toDto(saved));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Masquer un fil de mon côté (admin)", description = "Le fil disparaît uniquement de la liste admin — il reste visible et intact côté utilisateur.")
    public ApiResponseDTO<String> masquer(@PathVariable Long id) {
        contactService.masquerPourAdmin(id);
        return ApiResponseDTO.<String>success(null).message("Message masqué");
    }
}
