package com.alera.controller;

import com.alera.dto.WebhookEndpointFormDto;
import com.alera.model.WebhookEndpoint;
import com.alera.service.WebhookService;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;

@Controller
@RequestMapping("/admin/webhooks")
public class WebhookAdminController {

    private final WebhookService  webhookService;
    private final MessageSource   messageSource;

    public WebhookAdminController(WebhookService webhookService, MessageSource messageSource) {
        this.webhookService = webhookService;
        this.messageSource  = messageSource;
    }

    @GetMapping
    public String lista(Model model) {
        model.addAttribute("webhooks", webhookService.listarTodos());
        model.addAttribute("eventosDisponibles", WebhookService.EVENTOS_DISPONIBLES);
        return "admin/webhooks/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("webhookFormDto",   new WebhookEndpointFormDto());
        model.addAttribute("eventosDisponibles", WebhookService.EVENTOS_DISPONIBLES);
        model.addAttribute("secretGenerado",   WebhookService.generarSecret());
        return "admin/webhooks/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("webhookFormDto") @Valid WebhookEndpointFormDto formDto,
                          BindingResult result,
                          Model model,
                          RedirectAttributes flash,
                          Locale locale) {
        if (formDto.getEventosList() == null || formDto.getEventosList().isEmpty()) {
            result.rejectValue("eventosList", "size.min", msg("webhook.error.eventos", locale));
        }
        if (result.hasErrors()) {
            model.addAttribute("eventosDisponibles", WebhookService.EVENTOS_DISPONIBLES);
            model.addAttribute("secretGenerado", WebhookService.generarSecret());
            return "admin/webhooks/formulario";
        }
        webhookService.guardar(formDto);
        flash.addFlashAttribute("mensaje",      msg("webhook.guardado", locale));
        flash.addFlashAttribute("tipoMensaje",  "success");
        return "redirect:/admin/webhooks";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        WebhookEndpoint ep = webhookService.buscarPorId(id);
        WebhookEndpointFormDto dto = new WebhookEndpointFormDto();
        dto.setId(ep.getId());
        dto.setNombre(ep.getNombre());
        dto.setUrl(ep.getUrl());
        dto.setSecret(ep.getSecret());
        dto.setEventosList(ep.getEventosList());
        dto.setActivo(ep.isActivo());
        model.addAttribute("webhookFormDto",   dto);
        model.addAttribute("eventosDisponibles", WebhookService.EVENTOS_DISPONIBLES);
        return "admin/webhooks/formulario";
    }

    @PostMapping("/actualizar/{id}")
    public String actualizar(@PathVariable Long id,
                             @ModelAttribute("webhookFormDto") @Valid WebhookEndpointFormDto formDto,
                             BindingResult result,
                             Model model,
                             RedirectAttributes flash,
                             Locale locale) {
        if (formDto.getEventosList() == null || formDto.getEventosList().isEmpty()) {
            result.rejectValue("eventosList", "size.min", msg("webhook.error.eventos", locale));
        }
        if (result.hasErrors()) {
            model.addAttribute("eventosDisponibles", WebhookService.EVENTOS_DISPONIBLES);
            return "admin/webhooks/formulario";
        }
        webhookService.actualizar(id, formDto);
        flash.addFlashAttribute("mensaje",      msg("webhook.actualizado", locale));
        flash.addFlashAttribute("tipoMensaje",  "success");
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        webhookService.eliminar(id);
        flash.addFlashAttribute("mensaje",      msg("webhook.eliminado", locale));
        flash.addFlashAttribute("tipoMensaje",  "success");
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/toggle/{id}")
    public String toggle(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        webhookService.toggleActivo(id);
        return "redirect:/admin/webhooks";
    }

    @PostMapping("/probar/{id}")
    public String probar(@PathVariable Long id, RedirectAttributes flash, Locale locale) {
        boolean ok = webhookService.probar(id);
        flash.addFlashAttribute("mensaje",
                msg(ok ? "webhook.probado" : "webhook.error.probar", locale));
        flash.addFlashAttribute("tipoMensaje", ok ? "success" : "danger");
        return "redirect:/admin/webhooks";
    }

    private String msg(String key, Locale locale) {
        return messageSource.getMessage(key, null, key, locale);
    }
}
