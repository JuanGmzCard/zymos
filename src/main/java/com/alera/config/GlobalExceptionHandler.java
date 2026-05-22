package com.alera.config;

import com.alera.exception.EquipoEnUsoException;
import com.alera.exception.LoteNoEncontradoException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 404 — ruta o recurso no encontrado
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        model.addAttribute("codigo", 404);
        model.addAttribute("titulo", "Página no encontrada");
        model.addAttribute("descripcion", "La página que buscas no existe o fue movida.");
        return "error/error";
    }

    // 404 — entidad de base de datos no encontrada
    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleEntityNotFound(EntityNotFoundException ex, Model model) {
        log.warn("Entidad no encontrada: {}", ex.getMessage());
        model.addAttribute("codigo", 404);
        model.addAttribute("titulo", "Registro no encontrado");
        model.addAttribute("descripcion", "El registro que intentas acceder no existe o fue eliminado.");
        return "error/error";
    }

    // Equipo en uso — error de negocio con redirección amigable
    @ExceptionHandler(EquipoEnUsoException.class)
    public String handleEquipoEnUso(EquipoEnUsoException ex, Model model) {
        log.warn("Intento de eliminar equipo en uso: {}", ex.getMessage());
        model.addAttribute("codigo", "⚠");
        model.addAttribute("titulo", "Equipo en uso");
        model.addAttribute("descripcion", ex.getMessage());
        model.addAttribute("volverUrl", "/equipos");
        model.addAttribute("volverTexto", "Volver a Equipos");
        return "error/error";
    }

    // Lote no encontrado — error de negocio 404
    @ExceptionHandler(LoteNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleLoteNoEncontrado(LoteNoEncontradoException ex, Model model) {
        log.warn("Lote no encontrado: {}", ex.getMessage());
        model.addAttribute("codigo", 404);
        model.addAttribute("titulo", "Lote no encontrado");
        model.addAttribute("descripcion", ex.getMessage());
        model.addAttribute("volverUrl", "/");
        model.addAttribute("volverTexto", "Volver a Lotes");
        return "error/error";
    }

    // RuntimeException genérica — último recurso
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRuntime(RuntimeException ex, Model model) {
        log.error("Error de ejecución: {}", ex.getMessage());
        model.addAttribute("codigo", 400);
        model.addAttribute("titulo", "Error en la operación");
        model.addAttribute("descripcion", ex.getMessage());
        return "error/error";
    }

    // 500 — cualquier otra excepción no manejada
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Error interno del servidor", ex);
        model.addAttribute("codigo", 500);
        model.addAttribute("titulo", "Error interno del servidor");
        model.addAttribute("descripcion", "Ocurrió un error inesperado. Por favor intenta de nuevo o contacta al administrador.");
        return "error/error";
    }
}
