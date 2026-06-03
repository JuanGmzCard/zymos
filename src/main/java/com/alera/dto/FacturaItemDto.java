package com.alera.dto;

import com.alera.model.enums.TipoItemFactura;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FacturaItemDto {

    @NotNull(message = "El tipo de ítem es obligatorio")
    private TipoItemFactura tipoItem;

    @NotBlank(message = "El nombre del ítem es obligatorio")
    private String nombre;
    private String tipoInsumo;
    private String tipoEquipo;

    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad;

    private String unidad;

    @DecimalMin(value = "0.0", message = "El valor unitario no puede ser negativo")
    private BigDecimal valorUnitario;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal porcentajeDescuento;

    @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
    private BigDecimal porcentajeIvaItem;
}
