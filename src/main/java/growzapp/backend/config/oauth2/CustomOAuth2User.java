package growzapp.backend.config.oauth2;

import growzapp.backend.model.entite.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Map;

@Getter
// On ajoute OidcUser ici pour supporter Google/Apple/Azure etc.
public class CustomOAuth2User implements OAuth2User, OidcUser {
    private final User user;
    private final Map<String, Object> attributes;

    public CustomOAuth2User(User user, Map<String, Object> attributes) {
        this.user = user;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRole()))
                .toList();
    }

    @Override
    public String getName() {
        return user.getLogin();
    }

    public String getImageUrl() {
        return user.getImage();
    }

    // --- Méthodes obligatoires pour OidcUser ---

    @Override
    public Map<String, Object> getClaims() {
        return attributes;
    }

    @Override
    public OidcIdToken getIdToken() {
        // Optionnel : tu peux le passer au constructeur si tu en as besoin,
        // sinon null suffit pour la plupart des success handlers.
        return (OidcIdToken) attributes.get("id_token");
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return null;
    }
}