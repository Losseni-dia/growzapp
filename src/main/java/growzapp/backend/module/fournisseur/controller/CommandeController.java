package growzapp.backend.module.fournisseur.controller;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.fournisseur.dto.CommandeCreateDTO;
import growzapp.backend.module.fournisseur.dto.CommandeDTO;
import growzapp.backend.module.fournisseur.dto.MotifDTO;
import growzapp.backend.module.fournisseur.model.Commande;
import growzapp.backend.module.fournisseur.service.CommandeService;
import growzapp.backend.module.fournisseur.service.FournisseurService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@Tag(name = "Commandes fournisseur")
public class CommandeController {

    private final CommandeService commandeService;
    private final FournisseurService fournisseurService;
    private final UserRepository userRepository;

    private final Path factureRootLocation = Paths.get("uploads/commande-factures")
            .toAbsolutePath()
            .normalize();

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private boolean estAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getRole()));
    }

    @PostMapping
    @Operation(summary = "Créer une commande auprès d'un fournisseur pour l'un de mes projets")
    public ApiResponseDTO<CommandeDTO> creer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommandeCreateDTO dto) {
        User porteur = getCurrentUser(userDetails);
        Commande saved = commandeService.creerCommande(porteur, dto);
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @GetMapping("/mes-commandes")
    @Operation(summary = "Lister les commandes passées pour mes projets (porteur)")
    public ApiResponseDTO<List<CommandeDTO>> mesCommandes(@AuthenticationPrincipal UserDetails userDetails) {
        User porteur = getCurrentUser(userDetails);
        List<CommandeDTO> dtos = commandeService.getMesCommandesPorteur(porteur.getId()).stream()
                .map(commandeService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/recues")
    @Operation(summary = "Lister les commandes reçues (fournisseur)")
    public ApiResponseDTO<List<CommandeDTO>> commandesRecues(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        Long fournisseurId = fournisseurService.getByUserId(user.getId()).getId();
        List<CommandeDTO> dtos = commandeService.getCommandesRecuesFournisseur(fournisseurId).stream()
                .map(commandeService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping(value = "/{id}/livrer", consumes = "multipart/form-data")
    @Operation(summary = "Signaler la livraison d'une commande (fournisseur)", description = "La facture est obligatoire — elle est transmise automatiquement aux investisseurs du projet.")
    public ApiResponseDTO<CommandeDTO> livrer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestPart("facture") MultipartFile facture) {
        User user = getCurrentUser(userDetails);
        Commande saved = commandeService.marquerLivree(id, user, facture);
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @PostMapping("/{id}/confirmer-reception")
    @Operation(summary = "Confirmer la réception d'une commande (porteur)", description = "Libère les fonds séquestrés au bénéfice du fournisseur.")
    public ApiResponseDTO<CommandeDTO> confirmerReception(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User porteur = getCurrentUser(userDetails);
        Commande saved = commandeService.confirmerReception(id, porteur);
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @PostMapping("/{id}/litige")
    @Operation(summary = "Ouvrir un litige sur une commande livrée (porteur)")
    public ApiResponseDTO<CommandeDTO> ouvrirLitige(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody MotifDTO dto) {
        User porteur = getCurrentUser(userDetails);
        Commande saved = commandeService.ouvrirLitige(id, porteur, dto.motif());
        return ApiResponseDTO.success(commandeService.toDto(saved));
    }

    @GetMapping("/{id}/facture")
    @Operation(summary = "Consulter la facture d'une commande", description = "Accessible au porteur du projet, au fournisseur, aux investisseurs validés du projet, et à l'admin — traçabilité complète de l'usage des fonds.")
    public ResponseEntity<Resource> getFacture(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        Commande commande = commandeService.getCommandeAvecAutorisation(id, user, estAdmin(user));

        if (commande.getFactureUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String filename = commande.getFactureUrl().substring(commande.getFactureUrl().lastIndexOf('/') + 1);
            Path filePath = factureRootLocation.resolve(filename).normalize();

            if (!filePath.startsWith(factureRootLocation)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = filename.toLowerCase().endsWith(".pdf")
                    ? MediaType.APPLICATION_PDF
                    : MediaType.IMAGE_JPEG;

            return ResponseEntity.ok().contentType(mediaType).body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
