package growzapp.backend.module.user.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import growzapp.backend.module.referentiel.model.Langue;
import growzapp.backend.module.referentiel.model.Localite;
import growzapp.backend.module.user.dto.UserCreateDTO;
import growzapp.backend.module.user.dto.UserDTO;
import growzapp.backend.module.user.dto.UserUpdateDTO;
import growzapp.backend.module.user.mapper.UserMapper;
import growzapp.backend.module.user.model.Role;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.RoleRepository;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.wallet.model.Wallet;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final EntityManager entityManager;

    private final String AVATAR_DIR = System.getProperty("user.dir") + "/uploads/avatars/";

    private String saveImageLocally(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty())
            return null;

        Path uploadPath = Paths.get(AVATAR_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    @Transactional
    public UserDTO updateMyProfile(UserUpdateDTO dto, MultipartFile imageFile) throws IOException {
        User current = getCurrentUser();

        if (dto.getLogin() != null && !dto.getLogin().equals(current.getLogin())
                && userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (dto.getEmail() != null && !dto.getEmail().equals(current.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        if (dto.getPrenom() != null) current.setPrenom(dto.getPrenom());
        if (dto.getNom() != null) current.setNom(dto.getNom());
        if (dto.getEmail() != null) current.setEmail(dto.getEmail());
        if (dto.getContact() != null) current.setContact(dto.getContact());
        if (dto.getSexe() != null) current.setSexe(dto.getSexe());

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            current.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            if (current.getImage() != null && !current.getImage().startsWith("http")) {
                Files.deleteIfExists(Paths.get(AVATAR_DIR + current.getImage()));
            }
            current.setImage(saveImageLocally(imageFile));
        } else if (dto.getImage() != null && dto.getImage().startsWith("http")) {
            current.setImage(dto.getImage());
        }

        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            current.setLocalite(entityManager.getReference(Localite.class, dto.getLocalite().id()));
        }

        if (dto.getLangues() != null) {
            List<Langue> nouvelles = dto.getLangues().stream()
                    .map(l -> entityManager.getReference(Langue.class, l.id()))
                    .toList();
            current.getLangues().clear();
            current.getLangues().addAll(nouvelles);
        }

        return userMapper.toDto(userRepository.save(current));
    }

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UsernameNotFoundException("Utilisateur non authentifié");
        }

        return userRepository.findByLoginForAuth(auth.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + auth.getName()));
    }

    public User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UsernameNotFoundException("Utilisateur non authentifié");
        }

        return userRepository.findByLoginForAuth(authentication.getName())
                .orElseThrow(
                        () -> new UsernameNotFoundException("Utilisateur introuvable : " + authentication.getName()));
    }

    public User getCurrentUserOrNull() {
        try {
            return getCurrentUser();
        } catch (UsernameNotFoundException e) {
            return null;
        }
    }

    public List<UserDTO> getAllAdmin(String search) {
        if (search != null && !search.isBlank()) {
            String like = "%" + search.toLowerCase() + "%";
            return userRepository.findBySearchTerm(like)
                    .stream()
                    .map(userMapper::toDto)
                    .toList();
        }
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDto)
                .toList();
    }

    public UserDTO getUserDtoById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID : " + id));
        return userMapper.toDto(user);
    }

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
        user.setRoles(resolveRoles(dto.getRoles()));

        user = userRepository.save(user);
        handleAvatar(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDTO updateUserByAdmin(UserDTO dto) {
        User user = userRepository.findById(dto.getId())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (!user.getLogin().equals(dto.getLogin()) && userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (dto.getEmail() != null && !user.getEmail().equals(dto.getEmail())
                && userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        copyDtoToEntity(dto, user);

        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        user.setRoles(resolveRoles(dto.getRoles()));
        handleAvatar(user);
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDTO updateUserRoles(Long userId, List<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setRoles(resolveRoles(roleNames));
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDTO toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        user.setEnabled(!user.isEnabled());
        user = userRepository.save(user);
        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUserById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur non trouvé");
        }
        userRepository.deleteById(id);
    }

    @Transactional
    public UserDTO registerUser(UserCreateDTO dto, MultipartFile imageFile) throws IOException {
        if (userRepository.existsByLogin(dto.getLogin())) {
            throw new IllegalArgumentException("Ce login est déjà utilisé");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
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
        user.setInterfaceLanguage("fr");

        if (imageFile != null && !imageFile.isEmpty()) {
            user.setImage(saveImageLocally(imageFile));
        }

        Wallet wallet = Wallet.builder()
                .user(user)
                .soldeDisponible(java.math.BigDecimal.ZERO)
                .soldeBloque(java.math.BigDecimal.ZERO)
                .soldeRetirable(java.math.BigDecimal.ZERO)
                .walletType(growzapp.backend.module.wallet.enums.WalletType.USER)
                .build();
        user.setWallet(wallet);

        if (dto.getLocalite() != null && dto.getLocalite().id() != null) {
            user.setLocalite(entityManager.getReference(Localite.class, dto.getLocalite().id()));
        }

        if (dto.getLangues() != null && !dto.getLangues().isEmpty()) {
            List<Langue> langues = dto.getLangues().stream()
                    .map(l -> entityManager.getReference(Langue.class, l.id()))
                    .toList();
            user.setLangues(langues);
        }

        user.setRoles(resolveRoles(List.of("USER")));

        User savedUser = userRepository.save(user);
        return userMapper.toDto(savedUser);
    }

    @Transactional(readOnly = true)
    public UserDTO getCurrentUserDto() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new UsernameNotFoundException("Utilisateur non authentifié");
        }

        String login = auth.getName();
        User user = userRepository.findByLoginForAuth(login)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + login));

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserDtoByLogin(String loginOrEmail) {
        User user = userRepository.findWithProfileByLogin(loginOrEmail)
                .orElseGet(() -> userRepository.findWithProfileByEmail(loginOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + loginOrEmail)));

        return userMapper.toDto(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional
    public void updateUser(Long id, User user) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Utilisateur introuvable");
        }
        userRepository.save(user);
    }

    private Set<Role> resolveRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
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
}
