package ru.merezh.gateway.security.dto;

public record JwtUserDto(
        long id,
        String role
) {
}
