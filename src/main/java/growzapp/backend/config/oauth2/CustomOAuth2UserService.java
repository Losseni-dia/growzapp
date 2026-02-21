package growzapp.backend.config.oauth2;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.KycStatus;
import growzapp.backend.repository.RoleRepository;
import growzapp.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        return processOAuth2User(userRequest, oAuth2User);
    }

    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // --- C'EST ICI QU'ON UTILISE TES CLASSES ---
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        if (oAuth2UserInfo.getEmail() == null) {
            throw new RuntimeException("Email non trouvé chez le fournisseur.");
        }

        User user = userRepository.findByLoginForAuth(oAuth2UserInfo.getEmail())
                .orElseGet(() -> createSocialUser(oAuth2UserInfo));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    // L'aiguillage qui choisit la bonne classe (Google, GitHub, etc.)
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("github")) {
            // Tu pourrais créer une classe GithubOAuth2UserInfo plus tard
            return new GoogleOAuth2UserInfo(attributes);
        } else {
            throw new RuntimeException("Login avec " + registrationId + " non supporté.");
        }
    }

    private User createSocialUser(OAuth2UserInfo userInfo) {
        User user = new User();
        user.setEmail(userInfo.getEmail());
        user.setLogin(userInfo.getEmail());

        // On utilise enfin tes méthodes getFirstName() et getLastName() !
        user.setPrenom(userInfo.getFirstName());
        user.setNom(userInfo.getLastName());
        user.setImage(userInfo.getImageUrl());

        // Sécurité pour la DB
        user.setPassword(UUID.randomUUID().toString());
        user.setSexe(null);
        user.setEnabled(true);
        user.setKycStatus(KycStatus.NON_SOUMIS);

        roleRepository.findByRole("USER").ifPresent(role -> user.setRoles(Collections.singleton(role)));

        return userRepository.save(user);
    }
}