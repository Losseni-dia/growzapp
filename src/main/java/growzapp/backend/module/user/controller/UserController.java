package growzapp.backend.module.user.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import growzapp.backend.config.JwtService;
import growzapp.backend.module.email.EmailSenderService;
import growzapp.backend.module.shared.ApiResponseDTO;
import growzapp.backend.module.user.dto.UserCreateDTO;
import growzapp.backend.module.user.dto.UserDTO;
import growzapp.backend.module.user.dto.UserSearchDTO;
import growzapp.backend.module.user.dto.UserUpdateDTO;
import growzapp.backend.module.user.dto.auth.LoginRequest;
import growzapp.backend.module.user.dto.auth.LoginResponse;
import growzapp.backend.module.user.mapper.UserMapper;
import growzapp.backend.module.user.model.PasswordResetToken;
import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import growzapp.backend.module.user.service.PasswordResetTokenService;
import growzapp.backend.module.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Authentication", description = "Inscription, connexion, récupération de profil et gestion du compte utilisateur")
public class UserController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordResetTokenService tokenService;
    private final EmailSenderService emailService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    @Operation(
        summary = "Connexion utilisateur",
        description = "Authentifie un utilisateur avec son login et mot de passe. Retourne un token JWT Bearer à utiliser dans toutes les requêtes sécurisées.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Connexion réussie — token JWT retourné",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = LoginResponse.class))),
        @ApiResponse(responseCode = "401", description = "Identifiants incorrects",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User userEntity = userRepository.findByLoginForAuth(request.getLogin())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

        UserDTO userDTO = userMapper.toDto(userEntity);
        String token = jwtService.generateToken(userEntity);

        return ResponseEntity.ok(new LoginResponse(token, userDTO));
    }

    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PermitAll
    @Operation(
        summary = "Inscription d'un nouvel utilisateur",
        description = "Crée un nouveau compte utilisateur. Le champ 'user' doit contenir un JSON sérialisé de type UserCreateDTO. Le champ 'image' est optionnel (photo de profil).",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Inscription réussie",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Erreur serveur interne",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<UserDTO>> register(
            @Parameter(description = "Données de l'utilisateur au format JSON (UserCreateDTO sérialisé)",
                schema = @Schema(implementation = UserCreateDTO.class))
            @RequestPart("user") String userJson,
            @Parameter(description = "Photo de profil (optionnel)",
                schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            UserCreateDTO dto = mapper.readValue(userJson, UserCreateDTO.class);
            UserDTO created = userService.registerUser(dto, image);

            return ResponseEntity.ok(ApiResponseDTO.success(created)
                    .message("Inscription réussie !"));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("Erreur serveur : " + e.getMessage()));
        }
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Récupérer mon profil",
        description = "Retourne le profil complet de l'utilisateur actuellement authentifié (JWT ou OAuth2).",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil retourné avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ApiResponseDTO<UserDTO> getMyProfile(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("Non authentifié");
        }

        String login;
        Object principal = authentication.getPrincipal();

        if (principal instanceof OAuth2User oAuth2User) {
            login = oAuth2User.getName();
        } else if (principal instanceof UserDetails userDetails) {
            login = userDetails.getUsername();
        } else {
            login = principal.toString();
        }

        UserDTO userDTO = userService.getUserDtoByLogin(login);
        return ApiResponseDTO.success(userDTO);
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Mettre à jour mon profil",
        description = "Met à jour le profil de l'utilisateur connecté. Le champ 'user' doit contenir un JSON de type UserUpdateDTO. Le champ 'image' est optionnel.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profil mis à jour avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Données invalides",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<UserDTO>> updateMyProfile(
            @Parameter(description = "Données de mise à jour au format JSON (UserUpdateDTO sérialisé)",
                schema = @Schema(implementation = UserUpdateDTO.class))
            @RequestPart("user") String userJson,
            @Parameter(description = "Nouvelle photo de profil (optionnel)",
                schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            UserUpdateDTO dto = mapper.readValue(userJson, UserUpdateDTO.class);

            UserDTO updated = userService.updateMyProfile(dto, image);

            return ResponseEntity.ok(ApiResponseDTO.success(updated)
                    .message("Profil mis à jour avec succès"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Erreur : " + e.getMessage()));
        }
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Rechercher des utilisateurs",
        description = "Recherche des utilisateurs par nom, prénom ou login. Retourne au maximum 10 résultats. Le terme de recherche doit faire au minimum 2 caractères.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Liste des utilisateurs correspondants",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = UserSearchDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<List<UserSearchDTO>> searchUsers(
            @Parameter(description = "Terme de recherche (minimum 2 caractères)", example = "john", required = true)
            @RequestParam("term") String term) {

        if (term == null || term.trim().length() < 2) {
            return ResponseEntity.ok(List.of());
        }

        String search = "%" + term.toLowerCase() + "%";
        List<User> users = userRepository.findBySearchTerm(search);

        List<UserSearchDTO> result = users.stream()
                .filter(u -> !u.getLogin().equalsIgnoreCase("admin"))
                .limit(10)
                .map(u -> new UserSearchDTO(
                        u.getId(),
                        (u.getPrenom() + " " + u.getNom()).trim(),
                        u.getLogin(),
                        u.getImage()))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/me/language")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
        summary = "Changer la langue de l'interface",
        description = "Met à jour la langue d'interface préférée de l'utilisateur connecté.",
        tags = {"Authentication"}
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Langue mise à jour avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token JWT manquant ou invalide",
            content = @Content(schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> updateLanguage(
            @Parameter(description = "Code de la langue cible (ex: fr, en, es)", example = "fr", required = true)
            @RequestParam("lang") String lang) {

        String currentLogin = SecurityContextHolder.getContext().getAuthentication().getName();

        User user = userRepository.findByLoginForAuth(currentLogin)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

        user.setInterfaceLanguage(lang);
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponseDTO.success("Langue mise à jour avec succès"));
    }

    @PostMapping("/forgot-password")
    @Operation(
        summary = "Demander la réinitialisation du mot de passe",
        description = "Envoie un lien de réinitialisation à l'adresse email fournie. Pour des raisons de sécurité, la réponse est identique que l'email existe ou non.",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Email du compte à réinitialiser",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"email\": \"john.doe@example.com\"}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Email de réinitialisation envoyé (si le compte existe)",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<?>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        User user = userService.findByEmail(email);

        if (user != null) {
            PasswordResetToken token = tokenService.createTokenForUser(user);
            emailService.sendPasswordResetMail(user.getEmail(), token.getToken());
        }

        return ResponseEntity.ok(
                ApiResponseDTO.success(null)
                        .message("Si un compte existe avec cet email, un lien a été envoyé."));
    }

    @PostMapping("/reset-password")
    @Operation(
        summary = "Réinitialiser le mot de passe",
        description = "Applique le nouveau mot de passe en échange d'un token de réinitialisation valide.",
        tags = {"Authentication"},
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Token de réinitialisation et nouveau mot de passe",
            content = @Content(mediaType = "application/json",
                schema = @Schema(example = "{\"token\": \"abc123xyz\", \"password\": \"NouveauMdp456!\"}")))
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Mot de passe réinitialisé avec succès",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Token invalide ou expiré",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ApiResponseDTO.class)))
    })
    public ResponseEntity<ApiResponseDTO<String>> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String password = request.get("password");

        User user = tokenService.validatePasswordResetToken(token);

        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error("Token invalide ou expiré"));
        }

        user.setPassword(passwordEncoder.encode(password));
        userService.updateUser(user.getId(), user);
        tokenService.deleteToken(token);

        return ResponseEntity.ok(
                ApiResponseDTO.<String>success(null)
                        .message("Mot de passe réinitialisé avec succès."));
    }
}
