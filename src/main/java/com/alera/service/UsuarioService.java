package com.alera.service;

import com.alera.model.Usuario;
import com.alera.model.enums.RolUsuario;
import com.alera.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario u = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
        if (!u.isActivo()) throw new UsernameNotFoundException("Usuario inactivo: " + username);
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listarTodos() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorId(Long id) {
        return repo.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggest(String q) {
        if (q == null || q.isBlank() || q.trim().length() < 2) return List.of();
        String lower = q.trim().toLowerCase();
        return repo.findAllByOrderByCreatedAtDesc().stream()
            .filter(u -> u.getUsername().toLowerCase().contains(lower))
            .limit(6)
            .map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", u.getUsername());
                m.put("rol",      u.getRol() != null ? u.getRol().getDisplayName() : "");
                m.put("activo",   u.isActivo());
                m.put("anchor",   "usuario-" + u.getId());
                return m;
            }).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscarPorUsername(String username) {
        return repo.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean existeUsername(String username) {
        return repo.existsByUsername(username);
    }

    // Devuelve true si el usuario con ese id tiene el mismo username que el parámetro.
    // Usado para evitar que un admin se elimine o cambie su propio rol.
    @Transactional(readOnly = true)
    public boolean esElMismoUsuario(Long id, String username) {
        return repo.findById(id)
                .map(u -> u.getUsername().equals(username))
                .orElse(false);
    }

    public void guardar(String username, String password, RolUsuario rol) {
        Usuario u = new Usuario();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setRol(rol != null ? rol : RolUsuario.ADMIN);
        repo.save(u);
    }

    public void toggleActivo(Long id) {
        repo.findById(id).ifPresent(u -> {
            u.setActivo(!u.isActivo());
            repo.save(u);
        });
    }

    public void cambiarPassword(Long id, String newPassword) {
        repo.findById(id).ifPresent(u -> {
            u.setPassword(encoder.encode(newPassword));
            repo.save(u);
        });
    }

    public void cambiarRol(Long id, RolUsuario nuevoRol) {
        repo.findById(id).ifPresent(u -> {
            u.setRol(nuevoRol);
            repo.save(u);
        });
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }
}