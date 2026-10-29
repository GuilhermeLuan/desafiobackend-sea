package dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;

import java.util.Set;

public record UserResponse(Long id, String email, Role role, Set<String> coverageUfs) {
}
