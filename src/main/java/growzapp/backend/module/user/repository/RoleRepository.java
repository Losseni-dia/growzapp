package growzapp.backend.module.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import growzapp.backend.module.user.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRole(String role);
    
}
