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

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth
                                                // PUBLIC – Auth, register, projets publics, etc.
                                                .requestMatchers("/api/auth/**", "/api/auth/register").permitAll()
                                                .requestMatchers("/api/projets", "/api/projets/**").permitAll()
                                                .requestMatchers("/api/localites", "/api/langues", "/api/secteurs")
                                                .permitAll()

                                                // UPLOADS – Posters, contrats PDF, documents…
                                                .requestMatchers("/uploads/**").permitAll() // TOUT le dossier uploads
                                                                                            // est public

                                                // CONTRATS – QR code public + voir/télécharger le PDF (même sans être
                                                // connecté)
                                                .requestMatchers("/api/contrats/public/verifier/**").permitAll()
                                                .requestMatchers("/api/contrats/{numero}").permitAll() // voir le PDF
                                                                                                       // dans le
                                                                                                       // navigateur
                                                .requestMatchers("/api/contrats/{numero}/download").permitAll() // téléchargement
                                                                                                                // direct

                                                // Pages d’erreur + assets React
                                                .requestMatchers("/error", "/static/**", "/assets/**", "/favicon.ico")
                                                .permitAll()

                                                // Endpoints protégés
                                                .requestMatchers("/api/investissements", "/api/investissements/**")
                                                .authenticated()
                                                .requestMatchers("/api/projets/mes-projets").authenticated()
                                                .requestMatchers("/api/wallets/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/wallets/demander-payout")
                                                .authenticated()

                                                // Admin
                                                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")

                                                // Tout le reste → authentifié
                                                .anyRequest().authenticated())

                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))

                                .formLogin(form -> form.disable())
                                .httpBasic(basic -> basic.disable())
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