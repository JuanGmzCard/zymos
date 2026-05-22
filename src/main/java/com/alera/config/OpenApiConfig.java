package com.alera.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Alera API",
        version     = "1.0",
        description = "API REST para integración con apps móviles y herramientas externas. "
                    + "Todos los endpoints requieren autenticación HTTP Basic con las mismas "
                    + "credenciales del sistema web.",
        contact     = @Contact(name = "Alera — Sistema de Trazabilidad Cervecera")
    ),
    security = @SecurityRequirement(name = "basicAuth")
)
@SecurityScheme(
    name        = "basicAuth",
    type        = SecuritySchemeType.HTTP,
    scheme      = "basic",
    in          = SecuritySchemeIn.HEADER,
    description = "Usuario y contraseña del sistema Alera"
)
public class OpenApiConfig {
}