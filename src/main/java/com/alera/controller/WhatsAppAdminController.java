package com.alera.controller;

import com.alera.dto.ContactoWhatsAppFormDto;
import com.alera.model.enums.TipoAlertaWhatsApp;
import com.alera.service.ContactoWhatsAppService;
import com.alera.service.WhatsAppService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/whatsapp")
public class WhatsAppAdminController {

    private final ContactoWhatsAppService service;
    private final WhatsAppService whatsAppService;

    public WhatsAppAdminController(ContactoWhatsAppService service, WhatsAppService whatsAppService) {
        this.service = service;
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("contactos", service.listarTodos());
        model.addAttribute("tiposAlerta", TipoAlertaWhatsApp.values());
        model.addAttribute("waConfigurado", whatsAppService.isConfigured());
        return "admin/whatsapp/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("form", new ContactoWhatsAppFormDto());
        model.addAttribute("tiposAlerta", TipoAlertaWhatsApp.values());
        return "admin/whatsapp/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@Valid @ModelAttribute("form") ContactoWhatsAppFormDto dto,
                          BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("tiposAlerta", TipoAlertaWhatsApp.values());
            return "admin/whatsapp/formulario";
        }
        service.guardar(dto);
        flash.addFlashAttribute("mensaje", "Contacto guardado correctamente.");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/whatsapp";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("form", service.toFormDto(service.buscarPorId(id)));
        model.addAttribute("tiposAlerta", TipoAlertaWhatsApp.values());
        return "admin/whatsapp/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("form") ContactoWhatsAppFormDto dto,
                             BindingResult result, Model model, RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("tiposAlerta", TipoAlertaWhatsApp.values());
            return "admin/whatsapp/formulario";
        }
        service.actualizar(id, dto);
        flash.addFlashAttribute("mensaje", "Contacto actualizado.");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/whatsapp";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash) {
        service.eliminar(id);
        flash.addFlashAttribute("mensaje", "Contacto eliminado.");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/whatsapp";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes flash) {
        service.toggleActivo(id);
        flash.addFlashAttribute("mensaje", "Estado actualizado.");
        flash.addFlashAttribute("tipoMensaje", "success");
        return "redirect:/admin/whatsapp";
    }
}
