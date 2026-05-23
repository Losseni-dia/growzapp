package growzapp.backend.config.oauth2;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import growzapp.backend.module.kyc.enums.KycStatus;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.RoleRepository;
import growzapp.backend.module.user.repository.UserRepository;
import jakarta.transaction.Transactional;
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



    @Transactional
    public OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        String emailCandidate = oAuth2UserInfo.getEmail();
        if (emailCandidate == null && registrationId.equalsIgnoreCase("github")) {
            emailCandidate = oAuth2User.getAttributes().get("login") + "@github.com";
        }

        if (emailCandidate == null) {
            throw new RuntimeException("Email non trouvé chez le fournisseur.");
        }

        final String finalEmail = emailCandidate;

        return userRepository.findByEmailWithRoles(finalEmail)
                .map(existingUser -> {
                    // FORCE LA MISE À JOUR : C'est ici que ça se joue !
                    existingUser.setPrenom(oAuth2UserInfo.getFirstName());
                    existingUser.setNom(oAuth2UserInfo.getLastName());
                    existingUser.setImage(oAuth2UserInfo.getImageUrl());
                    // On sauvegarde pour que la DB soit à jour
                    User updatedUser = userRepository.save(existingUser);
                    return new CustomOAuth2User(updatedUser, oAuth2User.getAttributes());
                })
                .orElseGet(() -> {
                    User newUser = createSocialUser(oAuth2UserInfo, finalEmail);
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