package ro.etti.pki.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ro.etti.pki.platform.entity.User;

import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link User} entity.
 * <p>
 * Spring generates the implementation at runtime, providing the standard
 * CRUD operations ({@code save}, {@code findById}, {@code findAll}, {@code delete}).
 * The custom methods below are translated to SQL through the Spring Data
 * naming convention (derived queries).
 * </p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by username.
     * Used during authentication.
     *
     * @param username username to look up (case-sensitive)
     * @return {@link Optional} with the user, or empty if not found
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks whether a username exists without loading the entity.
     * More efficient than {@code findByUsername(...).isPresent()}, as it only
     * issues an existence query.
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email exists.
     */
    boolean existsByEmail(String email);
}
