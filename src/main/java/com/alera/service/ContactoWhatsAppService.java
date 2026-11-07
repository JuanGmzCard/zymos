package com.alera.service;

import com.alera.dto.ContactoWhatsAppFormDto;
import com.alera.model.ContactoWhatsApp;
import com.alera.model.ContactoWhatsAppSuscripcion;
import com.alera.repository.ContactoWhatsAppRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ContactoWhatsAppService {

    private final ContactoWhatsAppRepository repo;

    public ContactoWhatsAppService(ContactoWhatsAppRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<ContactoWhatsApp> listarTodos() {
        return repo.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public ContactoWhatsApp buscarPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contacto no encontrado: " + id));
    }

    public void guardar(ContactoWhatsAppFormDto dto) {
        ContactoWhatsApp c = new ContactoWhatsApp();
        mapearDto(dto, c);
        repo.save(c);
    }

    public void actualizar(Long id, ContactoWhatsAppFormDto dto) {
        ContactoWhatsApp c = buscarPorId(id);
        mapearDto(dto, c);
        repo.save(c);
    }

    public void eliminar(Long id) {
        repo.deleteById(id);
    }

    public void toggleActivo(Long id) {
        ContactoWhatsApp c = buscarPorId(id);
        c.setActivo(!c.isActivo());
        repo.save(c);
    }

    public ContactoWhatsAppFormDto toFormDto(ContactoWhatsApp c) {
        ContactoWhatsAppFormDto dto = new ContactoWhatsAppFormDto();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setTelefono(c.getTelefono());
        dto.setActivo(c.isActivo());
        dto.setTiposSuscritos(c.getTiposSuscritos());
        return dto;
    }

    private void mapearDto(ContactoWhatsAppFormDto dto, ContactoWhatsApp c) {
        c.setNombre(dto.getNombre());
        c.setTelefono(dto.getTelefono());
        c.setActivo(dto.isActivo());
        c.getSuscripciones().clear();
        if (dto.getTiposSuscritos() != null) {
            dto.getTiposSuscritos().forEach(tipo -> {
                ContactoWhatsAppSuscripcion s = new ContactoWhatsAppSuscripcion();
                s.setContacto(c);
                s.setTipo(tipo);
                c.getSuscripciones().add(s);
            });
        }
    }
}
