package com.alera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class ContactoWhatsAppFormDto {

    private Long id;

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @NotBlank
    @Pattern(regexp = "^\\+[1-9]\\d{6,14}$", message = "Formato E.164 requerido: +573001234567")
    private String telefono;

    private boolean activo = true;

    private List<String> tiposSuscritos = new ArrayList<>();
}
