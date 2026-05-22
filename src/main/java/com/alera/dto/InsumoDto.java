package com.alera.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsumoDto {

    private String nombre;
    private String cantidad;

    @Builder.Default
    private String unidad = "gr";

    public boolean isEmpty() {
        return (nombre == null || nombre.isBlank()) && (cantidad == null || cantidad.isBlank());
    }
}
