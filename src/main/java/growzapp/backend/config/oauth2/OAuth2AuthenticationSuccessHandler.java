package growzapp.backend.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import growzapp.backend.config.JwtService;
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

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        // 1. On prépare les claims (rôles) comme dans ta méthode generateToken(User)
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("roles", oAuth2User.getUser().getRoles().stream()
                .map(r -> r.getRole())
                .toList());

        // 2. On génère le token avec la méthode surchargée (String, Map)
        String token = jwtService.generateToken(oAuth2User.getUser().getLogin(), extraClaims);

        // 3. Redirection vers le Frontend
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/oauth2/redirect")
                .queryParam("token", token)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}