package ro.etti.pki.platform.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.etti.pki.platform.entity.User;
import ro.etti.pki.platform.repository.UserRepository;

/**
 * {@link UserDetailsService} implementation required by Spring Security for
 * username + password authentication.
 * <p>
 * Used by:
 * <ul>
 *   <li>{@code DaoAuthenticationProvider} during login, to load the user and
 *       check the password (BCrypt) against the stored hash.</li>
 *   <li>{@code JwtAuthenticationFilter} on every authenticated request, to
 *       rehydrate the principal from the DB using the username from the JWT.</li>
 * </ul>
 * </p>
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user by username and wraps it in an {@link AppUserDetails}.
     *
     * @param username username to look up (case-sensitive)
     * @return {@link UserDetails} adapter over the {@link User} entity
     * @throws UsernameNotFoundException if the username does not exist in the DB
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
        return new AppUserDetails(user);
    }
}
