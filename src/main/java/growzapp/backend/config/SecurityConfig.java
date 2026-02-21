package growzapp.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import growzapp.backend.config.oauth2.CustomOAuth2UserService;
import growzapp.backend.config.oauth2.OAuth2AuthenticationSuccessHandler;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                // PUBLIC
                                                .requestMatchers("/api/auth/**", "/api/auth/register").permitAll()
                                                .requestMatchers("/api/projets", "/api/projets/**").permitAll()
                                                .requestMatchers("/api/localites", "/api/langues", "/api/secteurs")
                                                .permitAll()
                                                .requestMatchers("/api/currencies/**").permitAll() // Autorise l'accès
                                                                                                   // public

                                                // FICHIERS PUBLICS
                                                .requestMatchers("/uploads/posters/**").permitAll()
                                                .requestMatchers("/uploads/avatars/**").permitAll()
                                                .requestMatchers("/uploads/contrats/**").permitAll()

                                                // BLOQUE TOUT ACCÈS DIRECT AUX DOCUMENTS PRIVÉS
                                                .requestMatchers("/files/documents/**").denyAll()

                                                // CONTRATS PUBLICS
                                                // MODIFICATION ICI : On autorise le POST pour la vérification sécurisée
                                                .requestMatchers(HttpMethod.POST,"/api/contrats/public/verifier-securise").permitAll()

                                                // Garder le reste du public
                                                .requestMatchers("/api/contrats/public/verifier/**").permitAll()
                                                .requestMatchers("/api/contrats/{numero}").permitAll()
                                                .requestMatchers("/api/contrats/{numero}/download").permitAll()


                                                .requestMatchers("/api/news/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/news/**")
                                                .hasAnyRole("ADMIN", "COMMUNICANT")
                                                .requestMatchers(HttpMethod.PUT, "/api/news/**")
                                                .hasAnyRole("ADMIN", "COMMUNICANT")
                                                .requestMatchers(HttpMethod.DELETE, "/api/news/**").hasAnyRole("ADMIN") // Seul
                                                                                                                        // l'admin
                                                                                                                        // supprime

                                                // API DOCUMENTS : authentifié + logique fine dans le controller
                                                .requestMatchers("/api/documents/projet/**").authenticated() // ← liste
                                                .requestMatchers(HttpMethod.GET, "/api/documents/*/download")
                                                .authenticated() // ← download
                                                .requestMatchers(HttpMethod.POST, "/api/documents/projet/**")
                                                .hasAuthority("ROLE_ADMIN") // ← upload

                                                // KYC : Soumission (Accessible à tout utilisateur connecté)
                                                .requestMatchers("/api/kyc/soumettre").authenticated()
                                                // KYC : Administration (Uniquement ROLE_ADMIN)
                                                .requestMatchers("/api/kyc/admin/**").hasAuthority("ROLE_ADMIN")

                                                // ENDPOINTS PROTÉGÉS
                                                .requestMatchers("/api/investissements/**").authenticated()
                                                .requestMatchers("/api/projets/mes-projets").authenticated()
                                                .requestMatchers("/api/wallets/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/wallets/demander-payout")
                                                .authenticated()

                                                // ADMIN
                                                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                                                // ASSETS + ERREUR
                                                .requestMatchers("/error", "/static/**", "/assets/**", "/favicon.ico")
                                                .permitAll()

                                                // TOUT LE RESTE → authentifié
                                                .anyRequest().authenticated())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
                                .oauth2Login(oauth2 -> oauth2
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService))
                                                .successHandler(oauth2SuccessHandler))
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(12);
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
                return config.getAuthenticationManager();
        }
}