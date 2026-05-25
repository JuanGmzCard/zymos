package com.alera.dto;

import lombok.Getter;

@Getter
public class AuthResponse {

    private final String token;
    private final String tipo     = "Bearer";
    private final long   expiresIn;
    private final String username;
    private final String rol;

    public AuthResponse(String token, long expiresIn, String username, String rol) {
        this.token     = token;
        this.expiresIn = expiresIn;
        this.username  = username;
        this.rol       = rol;
    }
}
