package com.alera.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-32-chars!!";

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService(SECRET, 24L);
    }

    private UserDetails usuario(String username, String rol) {
        return new User(username, "pw", List.of(new SimpleGrantedAuthority(rol)));
    }

    @Test
    void generarTokenRetornaStringNoVacio() {
        String token = service.generarToken(usuario("admin", "ROLE_ADMIN"), "default");
        assertThat(token).isNotBlank();
    }

    @Test
    void extraerUsernameRecuperaElSujeto() {
        String token = service.generarToken(usuario("admin", "ROLE_ADMIN"), "default");
        assertThat(service.extraerUsername(token)).isEqualTo("admin");
    }

    @Test
    void extraerTenantRecuperaElTenant() {
        String token = service.generarToken(usuario("admin", "ROLE_ADMIN"), "cerveceria1");
        assertThat(service.extraerTenant(token)).isEqualTo("cerveceria1");
    }

    @Test
    void validarTokenReturnsTrue() {
        String token = service.generarToken(usuario("admin", "ROLE_ADMIN"), "default");
        assertThat(service.validarToken(token)).isTrue();
    }

    @Test
    void validarTokenFirmaInvalidaReturnsFalse() {
        String token = service.generarToken(usuario("admin", "ROLE_ADMIN"), "default");
        String tampered = token.substring(0, token.length() - 4) + "XXXX";
        assertThat(service.validarToken(tampered)).isFalse();
    }

    @Test
    void validarTokenVacioReturnsFalse() {
        assertThat(service.validarToken("")).isFalse();
    }

    @Test
    void validarTokenMalformadoReturnsFalse() {
        assertThat(service.validarToken("not.a.jwt")).isFalse();
    }

    @Test
    void tokenExpiradoReturnsFalse() {
        JwtService shortLived = new JwtService(SECRET, 0L); // TTL 0 horas = expira de inmediato
        String token = shortLived.generarToken(usuario("admin", "ROLE_ADMIN"), "default");
        assertThat(shortLived.validarToken(token)).isFalse();
    }

    @Test
    void getTtlSegundosCoincideConHoras() {
        JwtService s = new JwtService(SECRET, 8L);
        assertThat(s.getTtlSegundos()).isEqualTo(8 * 3600L);
    }
}
