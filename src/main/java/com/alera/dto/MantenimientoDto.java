package com.alera.dto;

import com.alera.model.enums.TipoMantenimiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
public class MantenimientoDto {

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate fecha;

    @NotNull(message = "El tipo es obligatorio")
    private TipoMantenimiento tipo;

    private String descripcion;
    private String tecnico;

    private BigDecimal costo;
    private LocalDate proximoMantenimiento;
}
