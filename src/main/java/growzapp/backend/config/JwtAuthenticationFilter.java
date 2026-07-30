package growzapp.backend.config;

import growzapp.backend.module.auth.repository.RevokedTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RevokedTokenRepository revokedTokenRepository;

    // --- AJOUT DE CETTE MÉTHODE POUR ÉVITER L'ERREUR 500 SUR SWAGGER ---
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();
        return path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.equals("/swagger-ui.html");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Lit le token soit depuis le header Authorization (ancien
        // comportement, pour compatibilité pendant la migration), soit
        // depuis le cookie HttpOnly auth_token (nouveau comportement,
        // HIGH-03) — priorité au cookie s'il est présent.
        String jwt = null;

        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("auth_token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt == null) {
            final String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                jwt = authHeader.substring(7);
            }
        }

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {

            // Vérifie que le token n'a pas été explicitement révoqué (déconnexion,
            // changement de mot de passe) avant d'accepter sa signature valide
            final String jti = jwtService.extractJti(jwt);
            if (jti != null && revokedTokenRepository.existsByTokenJti(jti)) {
                filterChain.doFilter(request, response);
                return;
            }

            final String userLogin = jwtService.extractUsername(jwt);

            if (userLogin != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userLogin);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            // 2. IMPORTANT : Si le token est invalide, on ne plante pas le serveur (500)
            // On laisse juste la requête continuer. Si la route est protégée,
            // SecurityConfig renverra une 403 propre plus tard.
            logger.error("Erreur lors de l'authentification JWT", e);
        }

        filterChain.doFilter(request, response);
    }
}