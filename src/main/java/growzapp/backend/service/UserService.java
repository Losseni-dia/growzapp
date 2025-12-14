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

import org.hibernate.Hibernate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    // Utilisation du même chemin que ta config de ressources statiques
    private final String AVATAR_DIR = System.getProperty("user.dir") + "/uploads/avatars/";

    private String saveImageLocally(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        // Le dossier est déjà créé par StaticResourceConfig, mais on garde une sécurité
        Path uploadPath = Paths.get(AVATAR_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Nom unique pour éviter les conflits
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        // Sauvegarde physique
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @Transactional
    public UserDTO updateMyProfile(UserUpdateDTO dto, MultipartFile imageFile) throws IOException {
        User current = getCurrentUser();

        // 1. Unicité Login/Email
        if (dto.getLogin() != null && !dto.getLogin().equals(current.getLogin())
                && userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (dto.getEmail() != null && !dto.getEmail().equals(current.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // 2. Champs simples
        if (dto.getPrenom() != null)
            current.setPrenom(dto.getPrenom());
        if (dto.getNom() != null)
            current.setNom(dto.getNom());
        if (dto.getEmail() != null)
            current.setEmail(dto.getEmail());
        if (dto.getContact() != null)
            current.setContact(dto.getContact());
        if (dto.getSexe() != null)
            current.setSexe(dto.getSexe());

        // 3. Password
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            current.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        // 4. GESTION DE L'IMAGE (Optimisée)
        if (imageFile != null && !imageFile.isEmpty()) {
            // Supprimer l'ancienne si ce n'est pas une URL DiceBear
            if (current.getImage() != null && !current.getImage().startsWith("http")) {
                try {
                    Files.deleteIfExists(Paths.get(AVATAR_DIR + current.getImage()));
                } catch (IOException e) {
                    System.err.println("Erreur suppression ancien avatar: " + e.getMessage());
                }
            }
            // Enregistrer le nom du fichier
            current.setImage(saveImageLocally(imageFile));
        }

        // 5. Relations
        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            current.setLocalite(entityManager.getReference(Localite.class, dto.getLocalite().id()));
        }

        if (dto.getLangues() != null) {
            List<Langue> nouvelles = dto.getLangues().stream()
                    .map(l -> entityManager.getReference(Langue.class, l.getId()))
                    .toList();
            current.getLangues().clear();
            current.getLangues().addAll(nouvelles);
        }

        return converter.toUserDto(userRepository.save(current));
    }

  




   public User getCurrentUser() {
       Authentication auth = SecurityContextHolder.getContext().getAuthentication();

       if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
           throw new UsernameNotFoundException("Utilisateur non authentifié");
       }

       return userRepository.findByLoginForAuth(auth.getName())
               .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + auth.getName()));
   }

   /**
    * Version avec Authentication en paramètre (pour les contrôleurs qui reçoivent
    * déjà l'objet)
    * → même comportement : exception si non trouvé
    */
   public User getCurrentUser(Authentication authentication) {
       if (authentication == null || !authentication.isAuthenticated()
               || "anonymousUser".equals(authentication.getPrincipal())) {
           throw new UsernameNotFoundException("Utilisateur non authentifié");
       }

       return userRepository.findByLoginForAuth(authentication.getName())
               .orElseThrow(
                       () -> new UsernameNotFoundException("Utilisateur introuvable : " + authentication.getName()));
   }

   /**
    * Version soft qui retourne null si pas connecté (rarement utile, par ex. dans
    * les filtres ou logs)
    */
   public User getCurrentUserOrNull() {
       try {
           return getCurrentUser(); // réutilise la version stricte, on attrape juste l'exception
       } catch (UsernameNotFoundException e) {
           return null;
       }
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


    @Transactional
    public UserDTO registerUser(UserCreateDTO dto, MultipartFile imageFile) throws IOException {
        // 1. Vérifications d'unicité
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // 2. Création de l'entité User
        User user = new User();
        user.setLogin(dto.getLogin());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPrenom(dto.getPrenom());
        user.setNom(dto.getNom());
        user.setSexe(dto.getSexe());
        user.setEmail(dto.getEmail());
        user.setContact(dto.getContact());
        user.setEnabled(true);
        user.setInterfaceLanguage("fr");

        // 3. Image
        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImage(saveImageLocally(imageFile));
        }

        // 4. CRÉATION DU WALLET (Obligatoire à l'inscription)
        // On utilise le Builder de ta classe Wallet
        Wallet wallet = Wallet.builder()
                .user(user) // Liaison bidirectionnelle
                .soldeDisponible(java.math.BigDecimal.ZERO)
                .soldeBloque(java.math.BigDecimal.ZERO)
                .soldeRetirable(java.math.BigDecimal.ZERO)
                .walletType(growzapp.backend.model.enumeration.WalletType.USER)
                .build();

        // On lie le wallet à l'utilisateur
        user.setWallet(wallet);

        // 5. Relations (Localité et Langues)
        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            user.setLocalite(entityManager.getReference(Localite.class, dto.getLocalite().id()));
        }

        if (dto.getLangues() != null && !dto.getLangues().isEmpty()) {
            List<Langue> langues = dto.getLangues().stream()
                    .map(l -> entityManager.getReference(Langue.class, l.getId()))
                    .toList();
            user.setLangues(langues);
        }

        // 6. Rôles
        user.setRoles(resolveRoles(List.of("USER")));

        // 7. SAUVEGARDE UNIQUE
        // Grâce au CascadeType.ALL dans User.java, le Wallet est sauvé automatiquement
        User savedUser = userRepository.save(user);

        return converter.toUserDto(savedUser);
    }



// === Récupérer le DTO de l'utilisateur connecté ===
@Transactional(readOnly = true)
public UserDTO getCurrentUserDto() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
        throw new UsernameNotFoundException("Utilisateur non authentifié");
    }

    String login = auth.getName();

    User user = userRepository.findByLoginForAuth(login) // LA SEULE CHOSE QUI COMPTE
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + login));

    return converter.toUserDto(user);
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