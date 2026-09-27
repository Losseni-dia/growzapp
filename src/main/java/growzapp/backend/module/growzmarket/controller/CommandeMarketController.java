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
import growzapp.backend.module.growzmarket.dto.CommandeMarketLigneCreateDTO;
import growzapp.backend.module.growzmarket.dto.LitigeMessageCreateDTO;
import growzapp.backend.module.growzmarket.model.CommandeMarket;
import growzapp.backend.module.growzmarket.service.CommandeMarketService;
import growzapp.backend.module.paiement.common.PaymentProviderRouter;
import growzapp.backend.module.paiement.common.PaymentProviderService;
import growzapp.backend.module.paiement.stripe.StripeDepositService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.enums.SourcePaiement;
import growzapp.backend.module.wallet.enums.StatutTransaction;
import growzapp.backend.module.wallet.enums.TypeTransaction;
import growzapp.backend.module.wallet.enums.WalletType;
import growzapp.backend.module.wallet.model.Transaction;
import growzapp.backend.module.wallet.model.Wallet;
import growzapp.backend.module.wallet.repository.TransactionRepository;
import growzapp.backend.module.wallet.repository.WalletRepository;
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
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final StripeDepositService stripeDepositService;
    private final PaymentProviderRouter paymentProviderRouter;

    // "articleId:quantite,articleId:quantite" — format compact transmis en
    // aller-retour via les métadonnées du fournisseur de paiement (limite de
    // taille stricte côté Stripe), reconstruit à la confirmation webhook.
    private String encoderLignesCompact(CommandeMarketCreateDTO dto) {
        StringBuilder sb = new StringBuilder();
        for (CommandeMarketLigneCreateDTO l : dto.lignes()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(l.articleId()).append(":").append(l.quantite());
        }
        return sb.toString();
    }

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
        return ApiResponseDTO.success(commandeMarketService.toDto(saved, acheteur.getId(), false));
    }

    @PostMapping("/carte")
    @Operation(summary = "Acheter sur GrowzMarket par carte bancaire (Stripe)", description = "Crée une session Stripe Checkout — la commande n'est réellement créée qu'à la confirmation du paiement par webhook.")
    public ApiResponseDTO<java.util.Map<String, String>> acheterParCarte(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommandeMarketCreateDTO dto) {
        User acheteur = getCurrentUser(userDetails);
        var apercu = commandeMarketService.previsualiserCommande(acheteur, dto);
        String redirectUrl = stripeDepositService.createCommandeMarketSession(
                acheteur.getId(), apercu.projetVendeur().getId(), apercu.projetVendeur().getLibelle(),
                encoderLignesCompact(dto), dto.confirmationLieuRetrait(), apercu.total());
        return ApiResponseDTO.success(java.util.Map.of("redirectUrl", redirectUrl));
    }

    @PostMapping("/mobile")
    @Operation(summary = "Acheter sur GrowzMarket par Mobile Money", description = "Crée une session de paiement Mobile Money (FedaPay, bascule PayDunya) — la commande n'est réellement créée qu'à la confirmation du paiement par webhook.")
    public ApiResponseDTO<java.util.Map<String, String>> acheterParMobile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CommandeMarketCreateDTO dto) {
        User acheteur = getCurrentUser(userDetails);
        var apercu = commandeMarketService.previsualiserCommande(acheteur, dto);

        PaymentProviderService.PaymentSessionResponse response = paymentProviderRouter.creerSessionCommandeMarket(
                apercu.total(), acheteur.getId(), apercu.projetVendeur().getId(), apercu.projetVendeur().getLibelle(),
                encoderLignesCompact(dto), dto.confirmationLieuRetrait());

        Wallet wallet = walletRepository.findByUserId(acheteur.getId())
                .orElseThrow(() -> new RuntimeException("Wallet introuvable"));

        Transaction tx = Transaction.builder()
                .walletId(wallet.getId())
                .walletType(WalletType.USER)
                .montant(apercu.total())
                .type(TypeTransaction.VENTE_MARKET)
                .statut(StatutTransaction.EN_ATTENTE_PAIEMENT)
                .description("Achat GrowzMarket Mobile Money — " + apercu.projetVendeur().getLibelle())
                .createdAt(java.time.LocalDateTime.now())
                .referenceExterne(response.sessionToken())
                .referenceType("COMMANDE_MARKET_INITIATION")
                .referenceId(apercu.projetVendeur().getId())
                .sourcePaiement(SourcePaiement.MOBILE_MONEY)
                .build();
        transactionRepository.save(tx);

        return ApiResponseDTO.success(java.util.Map.of("redirectUrl", response.redirectUrl()));
    }

    @GetMapping("/mes-achats")
    @Operation(summary = "Lister mes achats GrowzMarket")
    public ApiResponseDTO<List<CommandeMarketDTO>> mesAchats(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<CommandeMarketDTO> dtos = commandeMarketService.getMesAchats(user.getId()).stream()
                .map(c -> commandeMarketService.toDto(c, user.getId(), false))
                .toList();
        return ApiResponseDTO.success(dtos);
    }

    @GetMapping("/mes-ventes")
    @Operation(summary = "Lister les ventes reçues sur mes projets (porteur)")
    public ApiResponseDTO<List<CommandeMarketDTO>> mesVentes(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        List<CommandeMarketDTO> dtos = commandeMarketService.getMesVentes(user.getId()).stream()
                .map(c -> commandeMarketService.toDto(c, user.getId(), false))
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
        return ApiResponseDTO.success(commandeMarketService.toDto(saved, user.getId(), false));
    }

    @PostMapping("/{id}/valider-retrait")
    @Operation(summary = "Valider le retrait d'une commande (porteur-vendeur)", description = "L'acheteur montre le numéro de sa commande sur place — c'est le vendeur qui clôture, pas l'acheteur lui-même.")
    public ApiResponseDTO<CommandeMarketDTO> validerRetrait(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.validerRetrait(id, user);
        return ApiResponseDTO.success(commandeMarketService.toDto(saved, user.getId(), false));
    }

    @PostMapping("/{id}/litige")
    @Operation(summary = "Ouvrir un litige (acheteur ou porteur-vendeur)")
    public ApiResponseDTO<CommandeMarketDTO> ouvrirLitige(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody MotifDTO dto) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.ouvrirLitige(id, user, dto.motif());
        return ApiResponseDTO.success(commandeMarketService.toDto(saved, user.getId(), false));
    }

    @PostMapping("/{id}/litige/messages")
    @Operation(summary = "Ajouter un message à un litige en cours (acheteur ou vendeur)", description = "Uniquement possible tant que la commande est au statut LITIGE, avant clôture/arbitrage.")
    public ApiResponseDTO<CommandeMarketDTO> ajouterMessageLitige(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody LitigeMessageCreateDTO dto) {
        User user = getCurrentUser(userDetails);
        CommandeMarket saved = commandeMarketService.ajouterMessageLitige(id, user, dto.message(), false, null);
        return ApiResponseDTO.success(commandeMarketService.toDto(saved, user.getId(), false));
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
