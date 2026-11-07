package com.alera.controller;

import com.alera.dto.MensajeChatDto;
import com.alera.model.Tenant;
import com.alera.service.TenantChatService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final TenantChatService chatService;

    public ChatController(TenantChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping
    public String index(Model model, HttpServletRequest request) {
        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        String tenantName = tenant != null && tenant.getName() != null
                ? tenant.getName() : "tu cervecería";
        model.addAttribute("iaConfigurada", chatService.isConfigured());
        model.addAttribute("sugerencias", chatService.getSugerencias(tenantName));
        return "chat/index";
    }

    @PostMapping("/mensaje")
    @ResponseBody
    public MensajeChatDto mensaje(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {

        String pregunta = (String) body.getOrDefault("pregunta", "");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> historialRaw =
                (List<Map<String, Object>>) body.getOrDefault("historial", List.of());

        List<MensajeChatDto> historial = historialRaw.stream()
                .map(m -> new MensajeChatDto(
                        (String) m.getOrDefault("role", "user"),
                        (String) m.getOrDefault("contenido", ""),
                        false))
                .toList();

        Tenant tenant = (Tenant) request.getAttribute("currentTenant");
        String tenantName = tenant != null && tenant.getName() != null
                ? tenant.getName() : "tu cervecería";

        return chatService.chat(pregunta, historial, tenantName);
    }
}
