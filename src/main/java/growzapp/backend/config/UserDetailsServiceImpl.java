package growzapp.backend.config;

import growzapp.backend.module.user.model.User;
import growzapp.backend.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

        private final UserRepository userRepository;

        @Override
        public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
                User user = userRepository.findByLoginForAuth(login)
                                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + login));

                return org.springframework.security.core.userdetails.User
                                .withUsername(user.getLogin())
                                .password(user.getPassword())
                                .authorities(user.getRoles().stream()
                                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()))
                                                .toList())
                                .accountExpired(false)
                                .accountLocked(false)
                                .credentialsExpired(false)
                                .disabled(!user.isEnabled())
                                .build();
        }
}