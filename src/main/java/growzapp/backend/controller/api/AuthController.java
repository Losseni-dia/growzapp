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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
    @PostMapping("/register")
    public ApiResponseDTO<UserDTO> register(@Valid @RequestBody UserCreateDTO dto) {
        UserDTO created = userService.registerUser(dto);
        return ApiResponseDTO.success(created)
                .message("Inscription réussie ! Bienvenue sur GrowzApp");
    }

    // PROFIL DE L'UTILISATEUR CONNECTÉ
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<UserDTO> getMyProfile() {
        UserDTO user = userService.getCurrentUserDto();
        return ApiResponseDTO.success(user);
    }

    // MISE À JOUR DU PROFIL (seulement ses propres données)
    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponseDTO<UserDTO> updateMyProfile(@Valid @RequestBody UserUpdateDTO dto) {
        UserDTO updated = userService.updateMyProfile(dto);
        return ApiResponseDTO.success(updated)
                .message("Profil mis à jour avec succès");
    }
}