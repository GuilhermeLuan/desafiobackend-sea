package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;
import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CoverageGuard {
    private final UserRepository userRepository;

    public CoverageGuard(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Coverage coverageOf(String email, Role role) {
        if (role == Role.CLIENT) {
            return new Coverage(Set.of(), false);
        }

        if (role == Role.ADMIN) {
            return new Coverage(Set.of(), true);
        }

        Set<String> coverageUfs = userRepository.findCoverageUfsByEmail(email);

        return new Coverage(coverageUfs, false);
    }

    public void assertCovers(String email, Role role, String uf) {
        Coverage coverage = coverageOf(email, role);

        if (!coverage.covers(uf)) {
            throw new CoverageAccessDeniedException("User does not have coverage for the specified scope");
        }
    }
}
