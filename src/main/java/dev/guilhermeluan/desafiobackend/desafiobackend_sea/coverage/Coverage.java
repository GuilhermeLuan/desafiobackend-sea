package dev.guilhermeluan.desafiobackend.desafiobackend_sea.coverage;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record Coverage(Set<String> ufs, boolean allUfs) {

    public Coverage {
        ufs = ufs == null
                ? Set.of()
                : ufs.stream()
                .filter(Objects::nonNull)
                .filter(uf -> !uf.isBlank())
                .map(Coverage::normalize)
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Coverage of(Set<String> ufs) {
        return new Coverage(ufs, false);
    }

    public static Coverage unrestricted() {
        return new Coverage(Set.of(), true);
    }

    @Override
    public Set<String> ufs() {
        if (allUfs) {
            throw new IllegalStateException("Unrestricted coverage has no UF list");
        }
        return ufs;
    }

    public boolean covers(String uf) {
        if (uf == null || uf.isBlank()) {
            return false;
        }

        if (allUfs) {
            return true;
        }

        return ufs.contains(normalize(uf));
    }

    public boolean isUnrestricted() {
        return allUfs;
    }

    private static String normalize(String uf) {
        return uf.trim().toUpperCase();
    }
}
