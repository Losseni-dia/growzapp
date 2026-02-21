package growzapp.backend.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import growzapp.backend.config.JwtService;
import growzapp.backend.model.entite.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

        private final JwtService jwtService;

        @Override
        public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                        Authentication authentication) throws IOException {

                Object principal = authentication.getPrincipal();
                User userEntity = null;

                // Extraction de l'entité User selon le type de principal
                if (principal instanceof CustomOAuth2User customUser) {
                        userEntity = customUser.getUser();
                } else if (principal instanceof OidcUser oidcUser) {
                        // Si tu utilises Google, Spring crée souvent un OidcUser.
                        // Si ton CustomOAuth2UserService est bien configuré,
                        // il devrait déjà retourner un CustomOAuth2User.
                        // Mais par sécurité, on gère le cas ici.
                        throw new IllegalStateException("L'utilisateur n'est pas du type attendu CustomOAuth2User. " +
                                        "Vérifiez votre CustomOAuth2UserService.");
                }

                if (userEntity == null) {
                        throw new RuntimeException("Impossible de récupérer l'utilisateur après authentification.");
                }

                // 1. Préparation des claims
                Map<String, Object> extraClaims = new HashMap<>();
                extraClaims.put("roles", userEntity.getRoles().stream()
                                .map(r -> r.getRole())
                                .toList());

                // 2. Génération du token
                String token = jwtService.generateToken(userEntity.getLogin(), extraClaims);

                // 3. Redirection vers le Frontend
                String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                                .queryParam("token", token)
                                .build().toUriString();

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
}