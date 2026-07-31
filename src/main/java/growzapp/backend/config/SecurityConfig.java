package growzapp.backend.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import growzapp.backend.config.oauth2.CustomOAuth2UserService;
import growzapp.backend.config.oauth2.OAuth2AuthenticationSuccessHandler;
import growzapp.backend.config.ratelimit.AuthRateLimitFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final AuthRateLimitFilter authRateLimitFilter;
        private final CustomOAuth2UserService customOAuth2UserService;
        private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

        @Value("${growzapp.security.csp}")
        private String cspPolicy;

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .cors(Customizer.withDefaults())
                                .csrf(csrf -> csrf.disable())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .headers(headers -> headers
                                                // MED-05 : headers de sécurité HTTP manquants
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000))
                                                .frameOptions(frame -> frame.deny())
                                                .contentTypeOptions(contentType -> {
                                                })
                                                .referrerPolicy(referrer -> referrer
                                                                .policy(org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                // MED-03 : Content Security Policy — mode Report-Only pour
                                                // l'instant (observe sans bloquer), le temps de confirmer
                                                // qu'aucune source légitime n'a été oubliée.
                                                .addHeaderWriter((request, response) -> response.setHeader(
                                                                "Content-Security-Policy",
                                                                cspPolicy)))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                                // PUBLIC
                                                // --- WHITELIST SWAGGER ---
                                                .requestMatchers(
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()
                                                .requestMatchers("/api/auth/**", "/api/auth/register",
                                                                "/api/v1/auth/**", "/api/v1/auth/register")
                                                .permitAll()
                                                .requestMatchers("/login/oauth2/**").permitAll()
                                                .requestMatchers("/api/projets", "/api/projets/**",
                                                                "/api/v1/projets", "/api/v1/projets/**")
                                                .permitAll()
                                                .requestMatchers("/api/localites", "/api/langues", "/api/secteurs",
                                                                "/api/v1/localites", "/api/v1/langues",
                                                                "/api/v1/secteurs")
                                                .permitAll()
                                                .requestMatchers("/api/currencies/**", "/api/v1/currencies/**")
                                                .permitAll() // Autorise l'accès
                                                // public

                                                // FICHIERS PUBLICS
                                                .requestMatchers("/uploads/posters/**").permitAll()
                                                .requestMatchers("/uploads/avatars/**").permitAll()

                                                // BLOQUE TOUT ACCÈS DIRECT AUX DOCUMENTS PRIVÉS

                                                // CONTRATS PUBLICS
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/contrats/public/verifier-securise",
                                                                "/api/v1/contrats/public/verifier-securise")
                                                .permitAll()

                                                // Garder le reste du public
                                                .requestMatchers("/api/contrats/public/verifier/**",
                                                                "/api/v1/contrats/public/verifier/**")
                                                .permitAll()
                                                .requestMatchers("/api/contrats/{numero}", "/api/v1/contrats/{numero}")
                                                .permitAll()
                                                .requestMatchers("/api/contrats/{numero}/download",
                                                                "/api/v1/contrats/{numero}/download")
                                                .permitAll()

                                                .requestMatchers("/api/news/**", "/api/v1/news/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/news/**", "/api/v1/news/**")
                                                .hasAnyRole("ADMIN", "COMMUNICANT")
                                                .requestMatchers(HttpMethod.PUT, "/api/news/**", "/api/v1/news/**")
                                                .hasAnyRole("ADMIN", "COMMUNICANT")
                                                .requestMatchers("/api/webhook/stripe").permitAll()
                                                .requestMatchers("/api/webhook/paydunya").permitAll()
                                                .requestMatchers("/api/webhook/fedapay").permitAll()
                                                .requestMatchers("/api/kyc/webhook/voveid",
                                                                "/api/v1/kyc/webhook/voveid")
                                                .permitAll()
                                                .requestMatchers("/api/contrats/verifier-token",
                                                                "/api/v1/contrats/verifier-token")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/kyc/start-voveid",
                                                                "/api/v1/kyc/start-voveid")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.DELETE, "/api/news/**", "/api/v1/news/**")
                                                .hasAnyRole("ADMIN")

                                                // API DOCUMENTS : authentifié + logique fine dans le controller
                                                .requestMatchers("/api/documents/projet/**",
                                                                "/api/v1/documents/projet/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/documents/*/download",
                                                                "/api/v1/documents/*/download")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/documents/projet/**",
                                                                "/api/v1/documents/projet/**")
                                                .hasAuthority("ROLE_ADMIN")

                                                // KYC : Soumission (Accessible à tout utilisateur connecté)
                                                .requestMatchers("/api/kyc/soumettre", "/api/v1/kyc/soumettre")
                                                .authenticated()
                                                .requestMatchers("/api/kyc/start-voveid", "/api/v1/kyc/start-voveid")
                                                .authenticated()
                                                // KYC : Administration (Uniquement ROLE_ADMIN)
                                                .requestMatchers("/api/kyc/admin/**", "/api/v1/kyc/admin/**")
                                                .hasAuthority("ROLE_ADMIN")

                                                // ENDPOINTS PROTÉGÉS
                                                .requestMatchers("/api/investissements/**",
                                                                "/api/v1/investissements/**")
                                                .authenticated()
                                                .requestMatchers("/api/projets/mes-projets",
                                                                "/api/v1/projets/mes-projets")
                                                .authenticated()
                                                .requestMatchers("/api/wallets/**", "/api/v1/wallets/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/wallets/demander-payout",
                                                                "/api/v1/wallets/demander-payout")
                                                .authenticated()

                                                // ADMIN
                                                .requestMatchers("/api/admin/**", "/api/v1/admin/**")
                                                .hasAuthority("ROLE_ADMIN")

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
                                                                .userService(customOAuth2UserService) // Gère GitHub,
                                                                // Facebook, etc.
                                                                .oidcUserService(this.oidcUserService()) // Gère Google
                                                                                                         // (OIDC)
                                                )
                                                .successHandler(oauth2SuccessHandler))
                                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
                OidcUserService delegate = new OidcUserService();
                return (userRequest) -> {
                        // Le délégué récupère l'utilisateur OIDC (Google) standard
                        OidcUser oidcUser = delegate.loadUser(userRequest);

                        // On appelle ta méthode maintenant publique pour transformer l'OidcUser
                        // en ton CustomOAuth2User (qui contient ton entité User PostgreSQL)
                        return (OidcUser) customOAuth2UserService.processOAuth2User(userRequest, oidcUser);
                };
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