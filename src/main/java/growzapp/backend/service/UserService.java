// growzapp/backend/service/UserService.java
package growzapp.backend.service;

import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.dto.userDTO.UserCreateDTO;
import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.model.dto.userDTO.UserUpdateDTO;
import growzapp.backend.model.entite.*;
import growzapp.backend.repository.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // ← AJOUTE ÇA (tu dois l'avoir ou le créer)
    private final PasswordEncoder passwordEncoder;
    private final DtoConverter converter;
    private final EntityManager entityManager;
   // private final LangueRepository langueRepository; // Optionnel, si tu veux gérer les langues

    // ==================================================================
    // 1. Utilisateur connecté
    // ==================================================================
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByLogin(auth.getName()).orElse(null);
    }

    // ==================================================================
    // 2. Liste paginée + recherche (ADMIN)
    // ==================================================================
      public List<UserDTO> getAllAdmin(String search) {
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            return userRepository.findBySearchTerm(like)
                    .stream()
                    .map(converter::toUserDto)
                    .toList();
        }
        return userRepository.findAll()
                .stream()
                .map(converter::toUserDto)
                .toList();
    }

    // ==================================================================
    // 3. Récupérer un user par ID
    // ==================================================================
    public UserDTO getUserDtoById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));
        return toDto(user);
    }

    // ==================================================================
    // 4. Création par ADMIN (avec rôles + mot de passe obligatoire)
    // ==================================================================
    @Transactional
    public UserDTO createUserByAdmin(UserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire lors de la création par l'admin");
        }
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (dto.getEmail() != null && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        User user = new User();
        copyDtoToEntity(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEnabled(true);

        // Gestion des rôles
        user.setRoles(resolveRoles(dto.getRoles()));

        user = userRepository.save(user);
        handleAvatar(user);
        return toDto(user);
    }

    // ==================================================================
    // 5. Mise à jour complète par ADMIN
    // ==================================================================
    @Transactional
    public UserDTO updateUserByAdmin(UserDTO dto) {
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Login et email : on vérifie l'unicité sauf si c'est le même
        if (!user.getLogin().equals(dto.getLogin()) && userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (dto.getEmail() != null && !user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        copyDtoToEntity(dto, user);

        // Mot de passe optionnel
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // Rôles
        user.setRoles(resolveRoles(dto.getRoles()));

        handleAvatar(user);
        user = userRepository.save(user);
        return toDto(user);
    }

    // ==================================================================
    // 6. Modifier UNIQUEMENT les rôles (super pratique depuis le front)
    // ==================================================================
    @Transactional
    public UserDTO updateUserRoles(Long userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setRoles(resolveRoles(roleNames));
        user = userRepository.save(user);
        return toDto(user);
    }

    // ==================================================================
    // 7. Activer / Désactiver un compte
    // ==================================================================
    @Transactional
    public UserDTO toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setEnabled(!user.isEnabled());
        user = userRepository.save(user);
        return toDto(user);
    }

    // ==================================================================
    // 8. Suppression définitive
    // ==================================================================
    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        userRepository.deleteById(id);
    }

    // ==================================================================
    // Helpers privés
    // ==================================================================
    private Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            // Par défaut, on donne ROLE_USER
            roleNames = List.of("USER");
        }

        return roleNames.stream()
                .map(name -> {
                    String normalized = name.toUpperCase().startsWith("ROLE_") ? name.toUpperCase()
                            : "ROLE_" + name.toUpperCase();
                    return roleRepository.findByRole(normalized.replace("ROLE_", ""))
                            .orElseGet(() -> {
                                Role newRole = new Role();
                                newRole.setRole(normalized.replace("ROLE_", ""));
                                return roleRepository.save(newRole);
                            });
                })
                .collect(Collectors.toSet());
    }

    private void copyDtoToEntity(UserDTO dto, User user) {
        user.setLogin(dto.getLogin());
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setEmail(dto.getEmail());
        user.setContact(dto.getContact());
        user.setSexe(dto.getSexe());

        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            Localite loc = new Localite();
            loc.setId(dto.getLocalite().id());
            // Tu peux fetcher la vraie entité si besoin : localiteRepository.findById(...)
            user.setLocalite(loc);
        } else {
            user.setLocalite(null);
        }
    }

    private void handleAvatar(User user) {
        if (user.getImage() == null || user.getImage().isBlank()) {
            user.setImage("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getLogin());
        }
    }


    // === INSCRIPTION PUBLIQUE (rôle USER par défaut) ===
    @Transactional
    public UserDTO registerUser(UserCreateDTO dto) {
        // === Vérifications (inchangées) ===
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        User user = new User();
        user.setLogin(dto.getLogin());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setSexe(dto.getSexe());
        user.setEmail(dto.getEmail());
        user.setContact(dto.getContact());
        user.setEnabled(true);

        // Photo
        if (dto.getImage() != null && !dto.getImage().isBlank()) {
            user.setImage(dto.getImage());
        } else {
            handleAvatar(user);
        }

        // LOCALITÉ – ON UTILISE entityManager.getReference() → ÇA MARCHE À 100%
        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            Localite localite = entityManager.getReference(Localite.class, dto.getLocalite().id());
            user.setLocalite(localite);
        }

        // LANGUES – ON UTILISE getReference() POUR CHAQUE ID
        if (dto.getLangues() != null && !dto.getLangues().isEmpty()) {
            List<Langue> langues = dto.getLangues().stream()
                    .filter(l -> l.getId() != null)
                    .map(l -> entityManager.getReference(Langue.class, l.getId()))
                    .toList();
            user.setLangues(langues);
        }

        // Rôles
        user.setRoles(resolveRoles(List.of("USER")));

        user = userRepository.save(user);
        return toDto(user);
    }



// === Récupérer le DTO de l'utilisateur connecté ===
public UserDTO getCurrentUserDto() {
    User user = getCurrentUser();
    if (user == null) throw new RuntimeException("Utilisateur non trouvé");
    return toDto(user);
}

// === Mise à jour du profil par l'utilisateur (pas de rôles) ===
@Transactional
public UserDTO updateMyProfile(UserUpdateDTO dto) {
    User current = getCurrentUser();
    if (current == null)
        throw new RuntimeException("Non authentifié");

    // === Unicité login/email (seulement si modifié) ===
    if (dto.getLogin() != null && !dto.getLogin().equals(current.getLogin())
            && userRepository.existsByLogin(dto.getLogin())) {
        throw new IllegalArgumentException("Ce login est déjà utilisé");
    }
    if (dto.getEmail() != null && !dto.getEmail().equals(current.getEmail())
            && userRepository.existsByEmail(dto.getEmail())) {
        throw new IllegalArgumentException("Cet email est déjà utilisé");
    }

    // === Champs simples : on ne met à jour que si la valeur est fournie ===
    if (dto.getLogin() != null && !dto.getLogin().isBlank())
        current.setLogin(dto.getLogin());
    if (dto.getPrenom() != null && !dto.getPrenom().isBlank())
        current.setPrenom(dto.getPrenom());
    if (dto.getNom() != null && !dto.getNom().isBlank())
        current.setNom(dto.getNom());
    if (dto.getEmail() != null && !dto.getEmail().isBlank())
        current.setEmail(dto.getEmail());
    if (dto.getContact() != null)
        current.setContact(dto.getContact().isBlank() ? null : dto.getContact());
    if (dto.getSexe() != null)
        current.setSexe(dto.getSexe());

    // === Mot de passe ===
    if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
        current.setPassword(passwordEncoder.encode(dto.getPassword()));
    }

    // === Photo (si upload via multipart) ===
    if (dto.getImage() != null && !dto.getImage().isBlank()) {
        current.setImage(dto.getImage());
    }

    // === Localité ===
    if (dto.getLocalite() != null && dto.getLocalite().id() != null && dto.getLocalite().id() > 0) {
        current.setLocalite(entityManager.getReference(Localite.class, dto.getLocalite().id()));
    }

    // === Langues ===
    if (dto.getLangues() != null) { // ← C'EST LA CLÉ : on teste juste "présent", pas "non vide" !
        List<Langue> nouvelles = dto.getLangues().stream()
                .filter(l -> l.getId() != null && l.getId() > 0)
                .map(l -> entityManager.getReference(Langue.class, l.getId()))
                .toList();

        //current.getLangues().clear();
        current.getLangues().addAll(nouvelles);
    }
    // Sinon → on garde les langues actuelles (ne rien faire)

    current = userRepository.save(current);
    return toDto(current);
}

private UserDTO toDto(User user) {
    return converter.toUserDto(user); // Centralisé, propre, plus de doublon
}



}