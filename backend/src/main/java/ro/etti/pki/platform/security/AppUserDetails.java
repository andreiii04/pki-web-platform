package ro.etti.pki.platform.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ro.etti.pki.platform.entity.User;

import java.util.Collection;
import java.util.List;

/**
 * {@link UserDetails} adapter over the application's {@link User} entity.
 * <p>
 * Spring Security works with the {@code UserDetails} interface for authentication
 * and authorization. This wrapper exposes the relevant fields (username, password hash,
 * authorities) of the JPA model without coupling the entity to Spring Security.
 * </p>
 *
 * <h2>Role → authority mapping</h2>
 * The role (enum {@link ro.etti.pki.platform.entity.UserRole}) is exposed as a single
 * {@link GrantedAuthority} with the {@code ROLE_} prefix (the Spring Security convention
 * for {@code hasRole("USER")} / {@code hasRole("ADMIN")}).
 */
public class AppUserDetails implements UserDetails {

    private final User user;

    public AppUserDetails(User user) {
        this.user = user;
    }

    /**
     * Returns the original {@link User} entity.
     * Used in controllers via {@code @AuthenticationPrincipal AppUserDetails}
     * to access the id / email / role without an extra DB lookup.
     */
    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
