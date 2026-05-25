package com.alera.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title       = "Alera API",
        version     = "1.0",
        description = "API REST para integración con apps móviles y herramientas externas. "
                    + "Soporta autenticación HTTP Basic y Bearer JWT. "
                    + "Para JWT: `POST /api/auth/login` → obtén el token → Authorization: Bearer {token}.",
        contact     = @Contact(name = "Alera — Sistema de Trazabilidad Cervecera")
    ),
    security = {
        @SecurityRequirement(name = "basicAuth"),
        @SecurityRequirement(name = "bearerAuth")
    }
)
@SecuritySchemes({
    @SecurityScheme(
        name        = "basicAuth",
        type        = SecuritySchemeType.HTTP,
        scheme      = "basic",
        in          = SecuritySchemeIn.HEADER,
        description = "Usuario y contraseña del sistema Alera"
    ),
    @SecurityScheme(
        name          = "bearerAuth",
        type          = SecuritySchemeType.HTTP,
        scheme        = "bearer",
        bearerFormat  = "JWT",
        in            = SecuritySchemeIn.HEADER,
        description   = "Token JWT obtenido desde POST /api/auth/login"
    )
})
public class OpenApiConfig {
}