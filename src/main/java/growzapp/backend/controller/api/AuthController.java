package growzapp.backend.controller.api;

import growzapp.backend.config.JwtService;
import growzapp.backend.model.dto.AuthDTO.LoginRequest;
import growzapp.backend.model.dto.AuthDTO.LoginResponse;
import growzapp.backend.model.dto.userDTO.UserCreateDTO;
import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.model.dto.userDTO.UserSearchDTO;
import growzapp.backend.model.dto.userDTO.UserUpdateDTO;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.UserService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final DtoConverter dtoConverter; // ← INJECTÉ AUTOMATIQUEMENT GRÂCE À @RequiredArgsConstructor

 @PostMapping("/login")
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    System.out.println("Tentative de login avec : " + request.getLogin());

    // Authentification Spring Security
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword())
    );
    SecurityContextHolder.getContext().setAuthentication(authentication);
    System.out.println("AUTHENTIFICATION RÉUSSIE !");

    // On recharge l'utilisateur avec les rôles déjà chargés (grâce à l'EntityGraph)
    User userEntity = userRepository.findByLoginForAuth(request.getLogin())
            .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé"));

    UserDTO userDTO = dtoConverter.toUserDto(userEntity);

    String token = jwtService.generateToken(userEntity);

    return ResponseEntity.ok(new LoginResponse(token, userDTO));
}


 @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PermitAll
public ResponseEntity<ApiResponseDTO<UserDTO>> register(
        @RequestPart("user") String userJson,
        @RequestPart(value = "image", required = false) MultipartFile image) {
    try {
        ObjectMapper mapper = new ObjectMapper();
        // === LIGNE IMPORTANTE : Ignore les champs qui ne correspondent pas parfaitement ===
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        UserCreateDTO dto = mapper.readValue(userJson, UserCreateDTO.class);

        // Appel du service (on lui passe le fichier Multipart)
        UserDTO created = userService.registerUser(dto, image);

        return ResponseEntity.ok(ApiResponseDTO.success(created)
                .message("Inscription réussie !"));

    } catch (Exception e) {
        // Log l'erreur exacte dans ta console Java pour debug
        e.printStackTrace(); 
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponseDTO.error("Erreur serveur : " + e.getMessage()));
    }
}

    // PROFIL DE L'UTILISATEUR CONNECTÉ
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO userDTO = userService.getUserDtoById(id);
        return ApiResponseDTO.success(userDTO);
    }

    // MISE À JOUR DU PROFIL (seulement ses propres données)
    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated")
    public ResponseEntity<ApiResponseDTO<UserDTO>> updateMyProfile(
            @RequestPart("user") String userJson,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            UserUpdateDTO dto = mapper.readValue(userJson, UserUpdateDTO.class);

            // On appelle le service en passant le fichier MultipartFile directement
            UserDTO updated = userService.updateMyProfile(dto, image);

            return ResponseEntity.ok(ApiResponseDTO.success(updated)
                    .message("Profil mis à jour avec succès"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Erreur : " + e.getMessage()));
        }
    }


   // Dans ton AuthController.java → ajoute cette méthode n’importe où dans la classe

   // AJOUTE ÇA DANS AuthController.java (n’importe où dans la classe)

   @GetMapping("/search")
   @PreAuthorize("isAuthenticated()")
   public ResponseEntity<List<UserSearchDTO>> searchUsers(@RequestParam("term") String term) {

       if (term == null || term.trim().length() < 2) {
           return ResponseEntity.ok(List.of());
       }

       // On cherche avec %term% pour plus de souplesse
       String search = "%" + term.toLowerCase() + "%";

       List<User> users = userRepository.findBySearchTerm(search);

       List<UserSearchDTO> result = users.stream()
               .filter(u -> !u.getLogin().equalsIgnoreCase("admin")) // optionnel
               .limit(10)
               .map(u -> new UserSearchDTO(
                       u.getId(),
                       (u.getPrenom() + " " + u.getNom()).trim(),
                       u.getLogin(),
                       u.getImage()))
               .toList();

       return ResponseEntity.ok(result);
   }


   // === AJOUTE CECI À LA FIN DE TA CLASSE AuthController ===

   @PatchMapping("/me/language")
   @PreAuthorize("isAuthenticated()")
   public ResponseEntity<ApiResponseDTO<String>> updateLanguage(@RequestParam("lang") String lang) {

       // 1. On récupère le login de l'utilisateur connecté
       String currentLogin = SecurityContextHolder.getContext().getAuthentication().getName();

       // 2. On cherche l'utilisateur en base
       // Note: J'utilise findByLoginForAuth car je sais qu'elle existe dans ton code
       // plus haut,
       // mais l'idéal serait un simple findByLogin.
       User user = userRepository.findByLoginForAuth(currentLogin)
               .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

       // 3. On met à jour la langue
       user.setInterfaceLanguage(lang);
       userRepository.save(user);

       System.out.println("Langue mise à jour pour " + currentLogin + " : " + lang);

       return ResponseEntity.ok(ApiResponseDTO.success("Langue mise à jour avec succès"));
   }



}