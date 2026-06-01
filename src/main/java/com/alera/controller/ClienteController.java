package com.alera.controller;

import com.alera.dto.ClienteFormDto;
import com.alera.model.enums.ListaPrecio;
import com.alera.model.enums.RegimenTributario;
import com.alera.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.alera.model.Cliente;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public String lista(@RequestParam(required = false) String nombre,
                        @RequestParam(required = false) Boolean activo,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        Page<Cliente> pagina = service.listarPaginado(nombre, activo, page);
        model.addAttribute("clientes",     pagina.getContent());
        model.addAttribute("paginaActual", page);
        model.addAttribute("totalPaginas", pagina.getTotalPages());
        model.addAttribute("nombre",       nombre);
        model.addAttribute("activo",       activo);
        model.addAttribute("totalClientes", pagina.getTotalElements());
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMIN')")
    public String nuevo(Model model) {
        model.addAttribute("cliente",    new ClienteFormDto());
        model.addAttribute("listasPrecio",   ListaPrecio.values());
        model.addAttribute("regimenes",  RegimenTributario.values());
        return "clientes/formulario";
    }

    @PostMapping("/guardar")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMIN')")
    public String guardar(@Valid @ModelAttribute("cliente") ClienteFormDto dto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("listasPrecio",  ListaPrecio.values());
            model.addAttribute("regimenes", RegimenTributario.values());
            return "clientes/formulario";
        }
        try {
            service.guardar(dto);
            flash.addFlashAttribute("mensaje",     "Cliente creado correctamente.");
            flash.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensaje",     e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/editar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMIN')")
    public String editar(@PathVariable Long id, Model model, RedirectAttributes flash) {
        return service.buscarPorId(id).map(c -> {
            ClienteFormDto dto = toFormDto(c);
            model.addAttribute("cliente",    dto);
            model.addAttribute("listasPrecio",   ListaPrecio.values());
            model.addAttribute("regimenes",  RegimenTributario.values());
            return "clientes/formulario";
        }).orElseGet(() -> {
            flash.addFlashAttribute("mensaje",     "Cliente no encontrado.");
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/actualizar/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMIN')")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("cliente") ClienteFormDto dto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("listasPrecio",  ListaPrecio.values());
            model.addAttribute("regimenes", RegimenTributario.values());
            return "clientes/formulario";
        }
        try {
            service.actualizar(id, dto);
            flash.addFlashAttribute("mensaje",     "Cliente actualizado correctamente.");
            flash.addFlashAttribute("tipoMensaje", "success");
        } catch (RuntimeException e) {
            flash.addFlashAttribute("mensaje",     e.getMessage());
            flash.addFlashAttribute("tipoMensaje", "danger");
        }
        return "redirect:/clientes";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Model model, RedirectAttributes flash) {
        return service.buscarPorId(id).map(c -> {
            model.addAttribute("cliente", c);
            return "clientes/detalle";
        }).orElseGet(() -> {
            flash.addFlashAttribute("mensaje",     "Cliente no encontrado.");
            flash.addFlashAttribute("tipoMensaje", "danger");
            return "redirect:/clientes";
        });
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN','FACTURACION','SUPERADMIN')")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        service.toggleActivo(id);
        flash.addFlashAttribute("mensaje",     "Estado del cliente actualizado.");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/clientes";
    }

    @GetMapping(value = "/suggest", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<Map<String, Object>> suggest(@RequestParam(defaultValue = "") String q) {
        return service.suggest(q);
    }

    private ClienteFormDto toFormDto(Cliente c) {
        ClienteFormDto dto = new ClienteFormDto();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setRazonSocial(c.getRazonSocial());
        dto.setNit(c.getNit());
        dto.setRegimenTributario(c.getRegimenTributario());
        dto.setEmail(c.getEmail());
        dto.setTelefono(c.getTelefono());
        dto.setDireccionDespacho(c.getDireccionDespacho());
        dto.setCiudad(c.getCiudad());
        dto.setDepartamento(c.getDepartamento());
        dto.setListaPrecio(c.getListaPrecio());
        dto.setActivo(c.isActivo());
        dto.setNotas(c.getNotas());
        return dto;
    }
}
