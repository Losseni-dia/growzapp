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

    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        // 1. On récupère la valeur initiale
        String emailCandidate = oAuth2UserInfo.getEmail();

        // 2. Logique GitHub (si besoin de changer la valeur)
        if (emailCandidate == null && registrationId.equalsIgnoreCase("github")) {
            emailCandidate = oAuth2User.getAttributes().get("login") + "@github.com";
        }

        if (emailCandidate == null) {
            throw new RuntimeException("Email non trouvé chez le fournisseur.");
        }

        // 3. ON CRÉE UNE VARIABLE FINALE ICI
        // C'est cette variable que la lambda va "capturer"
        final String finalEmail = emailCandidate;

        // CHANGEMENT ICI : On utilise la méthode qui FETCH les rôles
        return userRepository.findByEmailWithRoles(finalEmail)
                .map(existingUser -> {
                    // Plus besoin de .size() car FETCH a déjà chargé la collection
                    // pendant que la session était ouverte !
                    return new CustomOAuth2User(existingUser, oAuth2User.getAttributes());
                })
                .orElseGet(() -> {
                    User newUser = createSocialUser(oAuth2UserInfo, finalEmail);
                    // Pour un nouvel utilisateur, les rôles sont déjà chargés par le .save()
                    return new CustomOAuth2User(newUser, oAuth2User.getAttributes());
                });
    }

    // L'aiguillage qui choisit la bonne classe (Google, GitHub, etc.)
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equalsIgnoreCase("github")) {
            // ON UTILISE ENFIN LA CLASSE DÉDIÉE
            return new GithubOAuth2UserInfo(attributes);
        } else {
            throw new RuntimeException("Login avec " + registrationId + " non supporté.");
        }
    }

    private User createSocialUser(OAuth2UserInfo userInfo, String email) {
        User user = new User();
        user.setEmail(email); // On utilise l'email validé
        user.setLogin(email);

        user.setPrenom(userInfo.getFirstName());
        user.setNom(userInfo.getLastName());
        user.setImage(userInfo.getImageUrl());

        // Sécurité Database (Contraintes Not Null)
        user.setPassword(UUID.randomUUID().toString());
        user.setSexe(null); // Assurez-vous du ALTER TABLE ... DROP NOT NULL en SQL
        user.setEnabled(true);
        user.setKycStatus(KycStatus.NON_SOUMIS);

        roleRepository.findByRole("USER")
                .ifPresent(role -> user.setRoles(Collections.singleton(role)));

        return userRepository.save(user);
    }
}