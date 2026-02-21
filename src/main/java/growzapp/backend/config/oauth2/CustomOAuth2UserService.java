package growzapp.backend.config.oauth2;

import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.KycStatus;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Pour attribuer le rôle par défaut

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = extractEmail(registrationId, attributes);

        // On cherche l'utilisateur dans ta base PostgreSQL
        User user = userRepository.findByLoginForAuth(email)
                .orElseGet(() -> createSocialUser(email, attributes));

        return new CustomOAuth2User(user, attributes);
    }

    private String extractEmail(String provider, Map<String, Object> attributes) {
        if (provider.equalsIgnoreCase("github") && attributes.get("email") == null) {
            return attributes.get("login") + "@github.com";
        }
        return (String) attributes.get("email");
    }

    private User createSocialUser(String email, Map<String, Object> attributes) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(email);
        user.setNom((String) attributes.get("name"));
        user.setEnabled(true);
        user.setPassword(UUID.randomUUID().toString());// Pas de mot de passe pour les comptes sociaux
        user.setEnabled(true);
        user.setKycStatus(KycStatus.NON_SOUMIS);

        // On lui donne le rôle INVESTISSEUR par défaut (à adapter)
        roleRepository.findByRole("USER").ifPresent(role -> user.setRoles(Collections.singleton(role)));

        return userRepository.save(user);
    }
}