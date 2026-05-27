package com.alera.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object statusAttr = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int status = statusAttr != null ? (int) statusAttr : 500;

        switch (status) {
            case 403 -> {
                model.addAttribute("codigo", 403);
                model.addAttribute("titulo", "Acceso denegado");
                model.addAttribute("descripcion", "No tienes permisos para acceder a esta sección.");
            }
            case 404 -> {
                model.addAttribute("codigo", 404);
                model.addAttribute("titulo", "Página no encontrada");
                model.addAttribute("descripcion", "La página que buscas no existe o fue movida.");
            }
            case 503 -> {
                model.addAttribute("codigo", 503);
                model.addAttribute("titulo", "Servicio no disponible");
                model.addAttribute("descripcion", "Este tenant no está activo. Contacta al administrador.");
            }
            default -> {
                log.error("Error HTTP {} en {}", status, request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI));
                model.addAttribute("codigo", status);
                model.addAttribute("titulo", "Error inesperado");
                model.addAttribute("descripcion", "Ocurrió un error inesperado. Por favor intenta de nuevo.");
            }
        }

        return "error/error";
    }
}
