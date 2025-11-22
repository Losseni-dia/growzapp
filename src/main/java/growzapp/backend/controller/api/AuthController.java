package growzapp.backend.controller.api;

import growzapp.backend.config.JwtService;
import growzapp.backend.model.dto.AuthDTO.LoginRequest;
import growzapp.backend.model.dto.AuthDTO.LoginResponse;
import growzapp.backend.model.dto.userDTO.UserCreateDTO;
import growzapp.backend.model.dto.userDTO.UserDTO;
import growzapp.backend.model.dto.userDTO.UserUpdateDTO;
import growzapp.backend.model.dto.commonDTO.ApiResponseDTO;
import growzapp.backend.model.dto.commonDTO.DtoConverter;
import growzapp.backend.model.entite.User;
import growzapp.backend.repository.UserRepository;
import growzapp.backend.service.UserService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getLogin(), request.getPassword()));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
            System.out.println("AUTHENTIFICATION RÉUSSIE !");
        } catch (Exception e) {
            System.out.println("ÉCHEC AUTH : " + e.getMessage());
            throw e;
        }
                

       

        User userEntity = userRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        UserDTO userDTO = dtoConverter.toUserDto(userEntity);

        // ← JUSTE CETTE LIGNE CORRIGÉE
        String token = jwtService.generateToken(userEntity);

        return ResponseEntity.ok(new LoginResponse(token, userDTO));
    }


    // INSCRIPTION PUBLIQUE
  @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PermitAll
public ResponseEntity<ApiResponseDTO<UserDTO>> register(
    @RequestPart("user") String userJson,
    @RequestPart(value = "image", required = false) MultipartFile image
) {
    try {
        // 1. Convertir le JSON string → DTO
        ObjectMapper mapper = new ObjectMapper();
        UserCreateDTO dto = mapper.readValue(userJson, UserCreateDTO.class);

        // 2. Image → base64 (avec limite 5 Mo + gestion propre)
        if (image != null && !image.isEmpty()) {
            if (image.getSize() > 5 * 1024 * 1024) { // 5 Mo max
                return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Image trop volumineuse (max 5 Mo)"));
            }
            byte[] bytes = image.getBytes();
            String base64 = "data:" + image.getContentType() + ";base64,"
                + java.util.Base64.getEncoder().encodeToString(bytes);
            dto.setImage(base64);
        }

        // 3. Inscription
        UserDTO created = userService.registerUser(dto);

        return ResponseEntity.ok(
            ApiResponseDTO.success(created)
                .message("Inscription réussie ! Bienvenue sur GrowzApp")
        );

    } catch (JsonProcessingException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponseDTO.error("Données utilisateur invalides"));
    } catch (IOException e) {
        return ResponseEntity.badRequest()
            .body(ApiResponseDTO.error("Erreur lors du traitement de l'image"));
    } catch (Exception e) {
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponseDTO.error("Erreur serveur"));
    }
}

    // PROFIL DE L'UTILISATEUR CONNECTÉ
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<UserDTO> getMyProfile() {
        UserDTO user = userService.getCurrentUserDto();
        return ApiResponseDTO.success(user);
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

            if (image != null && !image.isEmpty()) {
                if (image.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDTO.error("Image trop volumineuse (max 5 Mo)"));
                }
                byte[] bytes = image.getBytes();
                String base64 = "data:" + image.getContentType() + ";base64,"
                        + java.util.Base64.getEncoder().encodeToString(bytes);
                dto.setImage(base64);
            }

            UserDTO updated = userService.updateMyProfile(dto);
            return ResponseEntity.ok(ApiResponseDTO.success(updated)
                    .message("Profil mis à jour avec succès"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponseDTO.error("Erreur : " + e.getMessage()));
        }
    }
}