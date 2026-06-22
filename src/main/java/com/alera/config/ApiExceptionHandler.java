package com.alera.config;

import com.alera.exception.LoteNoEncontradoException;
import io.sentry.Sentry;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Maneja excepciones lanzadas desde @RestController y las convierte en JSON.
 * Tiene prioridad sobre GlobalExceptionHandler (que retorna vistas HTML).
 */
@RestControllerAdvice(annotations = RestController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleNotFound(Exception ex) {
        log.warn("API 404: {}", ex.getMessage());
        return error(404, "Recurso no encontrado");
    }

    @ExceptionHandler({EntityNotFoundException.class, LoteNoEncontradoException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> handleEntityNotFound(Exception ex) {
        log.warn("API 404 entidad: {}", ex.getMessage());
        return error(404, ex.getMessage() != null ? ex.getMessage() : "Registro no encontrado");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("API 400: {}", ex.getMessage());
        return error(400, ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleRuntime(RuntimeException ex) {
        log.error("API 400 runtime: {}", ex.getMessage());
        return error(400, ex.getMessage() != null ? ex.getMessage() : "Error en la operación");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleGeneral(Exception ex) {
        log.error("API 500: error interno", ex);
        Sentry.captureException(ex);
        return error(500, "Error interno del servidor");
    }

    private Map<String, Object> error(int status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status);
        body.put("error",     message);
        body.put("timestamp", Instant.now().toString());
        return body;
    }
}
