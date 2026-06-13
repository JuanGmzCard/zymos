package com.alera.controller;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomErrorController")
class CustomErrorControllerTest {

    private final CustomErrorController controller = new CustomErrorController();

    private Model modelo(int status) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
        Model model = new ExtendedModelMap();
        controller.handleError(req, model);
        return model;
    }

    @Test
    @DisplayName("Error 401 → titulo 'Sesión expirada'")
    void error401_tituloSesionExpirada() {
        Model m = modelo(401);
        assertThat(m.getAttribute("codigo")).isEqualTo(401);
        assertThat(m.getAttribute("titulo")).isEqualTo("Sesión expirada");
        assertThat(m.getAttribute("descripcion").toString()).containsIgnoringCase("sesión");
    }

    @Test
    @DisplayName("Error 403 → titulo 'Acceso denegado'")
    void error403_tituloAccesoDenegado() {
        Model m = modelo(403);
        assertThat(m.getAttribute("codigo")).isEqualTo(403);
        assertThat(m.getAttribute("titulo")).isEqualTo("Acceso denegado");
    }

    @Test
    @DisplayName("Error 404 → titulo 'Página no encontrada'")
    void error404_tituloPaginaNoEncontrada() {
        Model m = modelo(404);
        assertThat(m.getAttribute("codigo")).isEqualTo(404);
        assertThat(m.getAttribute("titulo")).isEqualTo("Página no encontrada");
    }

    @Test
    @DisplayName("Error 503 → titulo 'Servicio no disponible'")
    void error503_tituloServicioNoDisponible() {
        Model m = modelo(503);
        assertThat(m.getAttribute("codigo")).isEqualTo(503);
        assertThat(m.getAttribute("titulo")).isEqualTo("Servicio no disponible");
    }

    @Test
    @DisplayName("Error 500 → titulo 'Error inesperado'")
    void error500_tituloErrorInesperado() {
        Model m = modelo(500);
        assertThat(m.getAttribute("codigo")).isEqualTo(500);
        assertThat(m.getAttribute("titulo")).isEqualTo("Error inesperado");
    }

    @Test
    @DisplayName("Sin atributo de status → status 500 por defecto")
    void sinStatus_usa500PorDefecto() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        // No se setea ERROR_STATUS_CODE
        Model model = new ExtendedModelMap();
        controller.handleError(req, model);
        assertThat(model.getAttribute("titulo")).isEqualTo("Error inesperado");
    }

    @Test
    @DisplayName("handleError retorna vista 'error/error'")
    void handleError_retornaVistaError() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 404);
        String view = controller.handleError(req, new ExtendedModelMap());
        assertThat(view).isEqualTo("error/error");
    }

    @Test
    @DisplayName("Todos los casos setean 'descripcion' en el modelo")
    void todosLosCasos_seteaDescripcion() {
        for (int status : new int[]{401, 403, 404, 503, 500}) {
            Model m = modelo(status);
            assertThat(m.getAttribute("descripcion"))
                .as("descripcion para status %d", status)
                .isNotNull()
                .isInstanceOf(String.class);
        }
    }
}
