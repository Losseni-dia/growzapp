package growzapp.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import growzapp.backend.model.entite.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    

    Optional<User> findByEmail(String email);


    // CORRIGÉ : retourne User, pas Projet
    Optional<User> findByPrenomIgnoreCaseAndNomIgnoreCase(String prenom, String nom);

    Optional<User> findByLogin(String login);

    boolean existsByLogin(String login);

    boolean existsByEmail(String email);


    // src/main/java/growzapp/backend/repository/UserRepository.java
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.login) LIKE LOWER(:search) OR " +
            "LOWER(u.email) LIKE LOWER(:search) OR " +
            "LOWER(u.prenom) LIKE LOWER(:search) OR " +
            "LOWER(u.nom) LIKE LOWER(:search)")
    List<User> findBySearchTerm(@Param("search") String search); // ← List, pas Page !

}