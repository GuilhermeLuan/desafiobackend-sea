package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthenticatedUser implements UserDetails {

    private final String email;
    private final String passwordHash;
    private final Role role;

    public AuthenticatedUser(User user) {
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.role = user.getRole();
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
