package com.alera.dto;

import com.alera.model.enums.TipoEquipo;
import com.alera.model.enums.TipoInsumo;
import com.alera.model.enums.TipoItemFactura;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FacturaItemDto {

    private TipoItemFactura tipoItem;
    private String nombre;
    private TipoInsumo tipoInsumo;
    private TipoEquipo tipoEquipo;

    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor a 0")
    private BigDecimal cantidad = BigDecimal.ONE;

    private String unidad;

    @DecimalMin(value = "0.0", message = "El valor unitario no puede ser negativo")
    private BigDecimal valorUnitario = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "El descuento no puede ser negativo")
    private BigDecimal porcentajeDescuento = BigDecimal.ZERO;

    @DecimalMin(value = "0.0", message = "El IVA no puede ser negativo")
    private BigDecimal porcentajeIvaItem = BigDecimal.ZERO;
}
