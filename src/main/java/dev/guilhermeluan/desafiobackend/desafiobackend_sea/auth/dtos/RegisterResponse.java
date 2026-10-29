package dev.guilhermeluan.desafiobackend.desafiobackend_sea.auth.dtos;

import dev.guilhermeluan.desafiobackend.desafiobackend_sea.users.Role;

public record RegisterResponse(Long id, String email, Role role) {
}
