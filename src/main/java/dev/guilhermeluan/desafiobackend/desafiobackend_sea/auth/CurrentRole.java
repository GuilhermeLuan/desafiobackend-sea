package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class CurrentRole {

    private CurrentRole() {
    }

    /** O papel do usuário autenticado, a partir da authority "ROLE_X" que o filtro JWT colocou. */
    public static Role of(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> Role.valueOf(authority.substring("ROLE_".length())))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Authenticated user has no role"));
    }
}
