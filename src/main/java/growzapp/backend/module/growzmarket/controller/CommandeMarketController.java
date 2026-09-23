package growzapp.backend.module.growzmarket.controller;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import growzapp.backend.module.fournisseur.dto.MotifDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketCreateDTO;
import growzapp.backend.module.growzmarket.dto.CommandeMarketDTO;
import growzapp.backend.module.growzmarket.model.CommandeMarket;
import growzapp.backend.module.growzmarket.service.CommandeMarketService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/market/commandes")
@RequiredArgsConstructor
@Tag(name = "GrowzMarket - Commandes")
public class CommandeMarketController {

    private final CommandeMarketService commandeMarketService;
    private final UserRepository userRepository;

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByLoginForAuth(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    private boolean estAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getRole()));
    }

    @PostMapping
    @Operation(summary = "Acheter sur GrowzMarket", description = "Paiement immédiat par wallet — nécessite d'avoir coché la confirmation du point de retrait.")
    public ApiResponseDTO<CommandeMarketDTO> acheter(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommandeMarketCreateDTO dto) {
        User acheteur = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.creerCommande(acheteur, dto);
        return ApiResponseDTO.success(commandeMarketService.toDto(saved));
    }

    @GetMapping("/mes-achats")
    @Operation(summary = "Lister mes achats GrowzMarket")
    public ApiResponseDTO<List<CommandeMarketDTO>> mesAchats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<CommandeMarketDTO> dtos = commandeMarketService.getMesAchats(user.getId()).stream()
                .map(commandeMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/mes-ventes")
    @Operation(summary = "Lister les ventes reçues sur mes projets (porteur)")
    public ApiResponseDTO<List<CommandeMarketDTO>> mesVentes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<CommandeMarketDTO> dtos = commandeMarketService.getMesVentes(user.getId()).stream()
                .map(commandeMarketService::toDto)
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @PostMapping("/{id}/preparer")
    @Operation(summary = "Marquer une commande prête au retrait (porteur)")
    public ApiResponseDTO<CommandeMarketDTO> preparer(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.marquerPrete(id, user);
        return ApiResponseDTO.success(commandeMarketService.toDto(saved));
    }

    @PostMapping("/{id}/valider-retrait")
    @Operation(summary = "Valider le retrait d'une commande (porteur-vendeur)", description = "L'acheteur montre le numéro de sa commande sur place — c'est le vendeur qui clôture, pas l'acheteur lui-même.")
    public ApiResponseDTO<CommandeMarketDTO> validerRetrait(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.validerRetrait(id, user);
        return ApiResponseDTO.success(commandeMarketService.toDto(saved));
    }

    @PostMapping("/{id}/litige")
    @Operation(summary = "Ouvrir un litige (acheteur ou porteur-vendeur)")
    public ApiResponseDTO<CommandeMarketDTO> ouvrirLitige(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody MotifDTO dto) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.ouvrirLitige(id, user, dto.motif());
        return ApiResponseDTO.success(commandeMarketService.toDto(saved));
    }

    @GetMapping("/{id}/facture")
    @Operation(summary = "Télécharger la facture d'une commande GrowzMarket")
    public ResponseEntity<ByteArrayResource> getFacture(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        CommandeMarket commande = commandeMarketService.getCommandeAvecAutorisation(id, user, estAdmin(user));

        if (commande.getFactureUrl() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] data = commandeMarketService.chargerFacture(commande);
        org.springframework.http.ContentDisposition contentDisposition = org.springframework.http.ContentDisposition
                .attachment()
                .filename("Facture GrowzMarket - Commande #" + commande.getId() + ".pdf",
                        java.nio.charset.StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(new ByteArrayResource(data));
    }
}
