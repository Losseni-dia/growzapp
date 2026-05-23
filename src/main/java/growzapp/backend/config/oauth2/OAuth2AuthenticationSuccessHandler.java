package growzapp.backend.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import growzapp.backend.config.JwtService;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        private final JwtService jwtService;
        private final UserRepository userRepository; // Injecté pour recharger le profil complet

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException {

                Object principal = authentication.getPrincipal();
                String login = null;

                // 1. On récupère le login depuis le principal CustomOAuth2User
                if (principal instanceof CustomOAuth2User customUser) {
                        login = customUser.getUser().getLogin();
                } else {
                        // Sécurité au cas où le type ne correspondrait pas
                        throw new IllegalStateException("Le principal n'est pas une instance de CustomOAuth2User.");
                }

                if (login == null) {
                        throw new RuntimeException(
                                        "Impossible de récupérer le login de l'utilisateur après authentification OAuth2.");
                }

                // 2. ÉTAPE CRUCIALE : On recharge l'utilisateur avec TOUTES ses relations
                // (EntityGraph)
                // C'est ce qui règle le problème des infos vides (localité, contact, etc.)
                User userEntity = userRepository.findWithProfileByLogin(login)
                                .orElseThrow(() -> new RuntimeException(
                                                "Utilisateur introuvable en base de données lors du succès OAuth2."));

                // 3. Préparation des claims pour le JWT
                Map<String, Object> extraClaims = new HashMap<>();
                extraClaims.put("roles", userEntity.getRoles().stream()
                                .map(r -> r.getRole())
                                .toList());

                // Optionnel : tu peux ajouter d'autres infos dans le token si besoin
                // extraClaims.put("name", userEntity.getPrenom() + " " + userEntity.getNom());

                // 4. Génération du token via ton JwtService
                String token = jwtService.generateToken(userEntity.getLogin(), extraClaims);

                // 5. Construction de l'URL de redirection vers ton Frontend
                // (React/Next/Flutter)
                // On passe le token en paramètre d'URL
                String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                                .queryParam("token", token)
                                .build().toUriString();

                // 6. Redirection effective
                getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
}