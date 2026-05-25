package com.alera.service;

import com.alera.model.SuperAdmin;
import com.alera.model.Usuario;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.SuperAdminRepository;
import com.alera.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService")
class UsuarioServiceTest {

    @Mock UsuarioRepository    repo;
    @Mock PasswordEncoder      encoder;
    @Mock SuperAdminRepository superAdminRepo;

    @InjectMocks
    UsuarioService service;

    // ── Helpers ───────────────────────────────────────────────────────

    private Usuario usuario(Long id, String username, RolUsuario rol, boolean activo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setUsername(username);
        u.setPassword("$2a$10$hashed");
        u.setRol(rol);
        u.setActivo(activo);
        return u;
    }

    // ── loadUserByUsername ────────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername retorna UserDetails con username, password y rol correcto")
    void loadUserByUsername_retornaUserDetails() {
        when(superAdminRepo.findByUsernameAndActivoTrue("admin")).thenReturn(Optional.empty());
        when(repo.findByUsername("admin")).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));

        var details = service.loadUserByUsername("admin");

        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashed");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername lanza UsernameNotFoundException si no existe el usuario")
    void loadUserByUsername_noExiste_lanzaExcepcion() {
        when(superAdminRepo.findByUsernameAndActivoTrue("fantasma")).thenReturn(Optional.empty());
        when(repo.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("fantasma"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma");
    }

    @Test
    @DisplayName("loadUserByUsername lanza UsernameNotFoundException si el usuario está inactivo")
    void loadUserByUsername_inactivo_lanzaExcepcion() {
        when(superAdminRepo.findByUsernameAndActivoTrue("inactivo")).thenReturn(Optional.empty());
        when(repo.findByUsername("inactivo")).thenReturn(Optional.of(
                usuario(2L, "inactivo", RolUsuario.INVENTARIO, false)));

        assertThatThrownBy(() -> service.loadUserByUsername("inactivo"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    @DisplayName("loadUserByUsername mapea correctamente cada rol al authority ROLE_X")
    void loadUserByUsername_mapeoDeRoles() {
        for (RolUsuario rol : RolUsuario.values()) {
            when(superAdminRepo.findByUsernameAndActivoTrue(rol.name())).thenReturn(Optional.empty());
            when(repo.findByUsername(rol.name())).thenReturn(Optional.of(
                    usuario(99L, rol.name(), rol, true)));

            var details = service.loadUserByUsername(rol.name());

            assertThat(details.getAuthorities().iterator().next().getAuthority())
                    .isEqualTo("ROLE_" + rol.name());
        }
    }

    // ── guardar ───────────────────────────────────────────────────────

    @Test
    @DisplayName("guardar codifica la contraseña con BCrypt antes de persistir")
    void guardar_codificaPassword() {
        when(encoder.encode("secreto")).thenReturn("$2a$encoded");

        service.guardar("nuevo", "secreto", RolUsuario.INVENTARIO);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$encoded");
    }

    @Test
    @DisplayName("guardar asigna el rol especificado al usuario")
    void guardar_asignaRolEspecificado() {
        service.guardar("factu", "pass", RolUsuario.FACTURACION);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(RolUsuario.FACTURACION);
        assertThat(captor.getValue().getUsername()).isEqualTo("factu");
    }

    @Test
    @DisplayName("guardar usa ADMIN como rol por defecto cuando rol es null")
    void guardar_rolNull_usaAdminPorDefecto() {
        service.guardar("adminDefault", "pass", null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(RolUsuario.ADMIN);
    }

    // ── toggleActivo ──────────────────────────────────────────────────

    @Test
    @DisplayName("toggleActivo desactiva un usuario activo")
    void toggleActivo_activoQuedaInactivo() {
        when(repo.findById(1L)).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));

        service.toggleActivo(1L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isFalse();
    }

    @Test
    @DisplayName("toggleActivo reactiva un usuario inactivo")
    void toggleActivo_inactivoQuedaActivo() {
        when(repo.findById(2L)).thenReturn(Optional.of(
                usuario(2L, "inv", RolUsuario.INVENTARIO, false)));

        service.toggleActivo(2L);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().isActivo()).isTrue();
    }

    @Test
    @DisplayName("toggleActivo no hace nada si el usuario no existe")
    void toggleActivo_noExiste_noHaceNada() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.toggleActivo(99L);

        verify(repo, never()).save(any());
    }

    // ── cambiarPassword ───────────────────────────────────────────────

    @Test
    @DisplayName("cambiarPassword codifica y guarda la nueva contraseña")
    void cambiarPassword_codificaYGuarda() {
        when(repo.findById(1L)).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));
        when(encoder.encode("nueva123")).thenReturn("$2a$nuevo");

        service.cambiarPassword(1L, "nueva123");

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$nuevo");
    }

    @Test
    @DisplayName("cambiarPassword no hace nada si el usuario no existe")
    void cambiarPassword_noExiste_noHaceNada() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.cambiarPassword(99L, "cualquier");

        verify(repo, never()).save(any());
        verify(encoder, never()).encode(any());
    }

    // ── cambiarRol ────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiarRol actualiza el rol del usuario")
    void cambiarRol_actualizaRol() {
        when(repo.findById(1L)).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));

        service.cambiarRol(1L, RolUsuario.FACTURACION);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getRol()).isEqualTo(RolUsuario.FACTURACION);
    }

    @Test
    @DisplayName("cambiarRol no hace nada si el usuario no existe")
    void cambiarRol_noExiste_noHaceNada() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        service.cambiarRol(99L, RolUsuario.INVENTARIO);

        verify(repo, never()).save(any());
    }

    // ── eliminar ──────────────────────────────────────────────────────

    @Test
    @DisplayName("eliminar delega a deleteById")
    void eliminar_delegaADeleteById() {
        service.eliminar(5L);

        verify(repo).deleteById(5L);
    }

    // ── existeUsername ────────────────────────────────────────────────

    @Test
    @DisplayName("existeUsername retorna true cuando el username ya existe")
    void existeUsername_existente_retornaTrue() {
        when(repo.existsByUsername("admin")).thenReturn(true);

        assertThat(service.existeUsername("admin")).isTrue();
    }

    @Test
    @DisplayName("existeUsername retorna false cuando el username no existe")
    void existeUsername_noExistente_retornaFalse() {
        when(repo.existsByUsername("nuevo")).thenReturn(false);

        assertThat(service.existeUsername("nuevo")).isFalse();
    }

    // ── esElMismoUsuario ──────────────────────────────────────────────

    @Test
    @DisplayName("esElMismoUsuario retorna true si el id corresponde al username")
    void esElMismoUsuario_mismoUsuario_retornaTrue() {
        when(repo.findById(1L)).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));

        assertThat(service.esElMismoUsuario(1L, "admin")).isTrue();
    }

    @Test
    @DisplayName("esElMismoUsuario retorna false si el id pertenece a otro usuario")
    void esElMismoUsuario_otroUsuario_retornaFalse() {
        when(repo.findById(1L)).thenReturn(Optional.of(
                usuario(1L, "admin", RolUsuario.ADMIN, true)));

        assertThat(service.esElMismoUsuario(1L, "inventario")).isFalse();
    }

    @Test
    @DisplayName("esElMismoUsuario retorna false si el usuario no existe")
    void esElMismoUsuario_noExiste_retornaFalse() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThat(service.esElMismoUsuario(99L, "admin")).isFalse();
    }

    // ── suggest ───────────────────────────────────────────────────────

    @Test
    @DisplayName("suggest retorna vacío si la query es nula")
    void suggest_queryNula_retornaVacio() {
        assertThat(service.suggest(null)).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest retorna vacío si la query tiene menos de 2 caracteres")
    void suggest_queryCorta_retornaVacio() {
        assertThat(service.suggest("a")).isEmpty();
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("suggest filtra usuarios por username case-insensitive")
    void suggest_filtraCorrectamente() {
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                usuario(1L, "admin",      RolUsuario.ADMIN,       true),
                usuario(2L, "inventario", RolUsuario.INVENTARIO,  true),
                usuario(3L, "adminProd",  RolUsuario.FACTURACION, false)
        ));

        List<Map<String, Object>> resultado = service.suggest("adm");

        assertThat(resultado).hasSize(2);
        assertThat(resultado).extracting("username")
                .containsExactlyInAnyOrder("admin", "adminProd");
    }

    @Test
    @DisplayName("suggest retorna máximo 6 resultados")
    void suggest_limiteSeisResultados() {
        List<Usuario> muchos = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(i -> usuario((long) i, "user" + i, RolUsuario.ADMIN, true))
                .toList();
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(muchos);

        assertThat(service.suggest("user")).hasSize(6);
    }

    @Test
    @DisplayName("suggest incluye username, rol, activo y anchor en cada resultado")
    void suggest_estructuraDelMapa() {
        when(repo.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                usuario(7L, "adminTest", RolUsuario.ADMIN, true)));

        Map<String, Object> item = service.suggest("admin").get(0);

        assertThat(item).containsKeys("username", "rol", "activo", "anchor");
        assertThat(item.get("username")).isEqualTo("adminTest");
        assertThat(item.get("rol")).isEqualTo("Administrador");
        assertThat(item.get("activo")).isEqualTo(true);
        assertThat(item.get("anchor")).isEqualTo("usuario-7");
    }
}
