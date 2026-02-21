package growzapp.backend.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.User;
import growzapp.backend.model.enumeration.KycStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

        Optional<User> findByEmail(String email);

        Optional<User> findByPrenomIgnoreCaseAndNomIgnoreCase(String prenom, String nom);

        boolean existsByLogin(String login);

        boolean existsByEmail(String email);

        @Query("SELECT u FROM User u WHERE " +
                        "LOWER(u.login) LIKE LOWER(:search) OR " +
                        "LOWER(u.email) LIKE LOWER(:search) OR " +
                        "LOWER(u.prenom) LIKE LOWER(:search) OR " +
                        "LOWER(u.nom) LIKE LOWER(:search)")
        List<User> findBySearchTerm(@Param("search") String search);

        Optional<User> findByLogin(String login); // celle-là marche sans @Query

        @EntityGraph(attributePaths = "roles")
        @Query("SELECT u FROM User u WHERE u.login = :login")
        Optional<User> findByLoginForAuth(@Param("login") String login);

        @Query("SELECT new map(" +
                        "u.id as id, " +
                        "u.prenom as prenom, " +
                        "u.nom as nom, " +
                        "u.login as login) " +
                        "FROM User u WHERE u.wallet.id = :walletId")
        Optional<Map<String, Object>> findBasicInfoByWalletId(@Param("walletId") Long walletId);

        // =============================================
        // LES 2 MÉTHODES MAGIQUES (à utiliser pour /me)
        // =============================================

        @EntityGraph(attributePaths = {
                        "projets",
                        "projets.secteur",
                        "projets.siteProjet",
                        "investissements",
                        "investissements.projet",
                        "localite",
                        "langues",
                        "roles"
        })
        Optional<User> findWithProfileByLogin(String login);

        @EntityGraph(attributePaths = {
                        "projets",
                        "projets.secteur",
                        "projets.siteProjet",
                        "investissements",
                        "investissements.projet",
                        "localite",
                        "langues",
                        "roles"
        })
        Optional<User> findWithProfileById(Long id);

        List<User> findByKycStatus(KycStatus status);

        // Dans UserRepository.java
        @Query("SELECT u FROM User u JOIN u.roles r WHERE r.role = :roleName")
        List<User> findByRole(@Param("roleName") String roleName);

        @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
        Optional<User> findByEmailWithRoles(@Param("email") String email);

        @EntityGraph(attributePaths = {
                        "projets", "projets.secteur", "projets.siteProjet",
                        "investissements", "investissements.projet",
                        "localite", "langues", "roles"
        })
        Optional<User> findWithProfileByEmail(String email);
}