package com.alera.dto;

import com.alera.model.enums.TipoItemFactura;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FacturaFormDto — validaciones Bean Validation")
class FacturaFormDtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private FacturaFormDto dtoValido() {
        FacturaFormDto dto = new FacturaFormDto();
        dto.setNumeroFactura("FAC-001");
        dto.setProveedor("Proveedor SA");
        dto.setFechaFactura(LocalDate.now());
        return dto;
    }

    private FacturaItemDto itemValido() {
        FacturaItemDto item = new FacturaItemDto();
        item.setTipoItem(TipoItemFactura.INSUMO);
        item.setNombre("Malta Pilsen");
        item.setCantidad(new BigDecimal("10.000"));
        item.setValorUnitario(new BigDecimal("5000.00"));
        item.setPorcentajeDescuento(BigDecimal.ZERO);
        item.setPorcentajeIvaItem(new BigDecimal("19"));
        return item;
    }

    @Test
    @DisplayName("DTO válido no produce violaciones")
    void dto_valido_sinViolaciones() {
        FacturaFormDto dto = dtoValido();
        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("numeroFactura nulo produce violación")
    void numeroFactura_nulo_violacion() {
        FacturaFormDto dto = dtoValido();
        dto.setNumeroFactura(null);
        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("numeroFactura"));
    }

    @Test
    @DisplayName("numeroFactura en blanco produce violación")
    void numeroFactura_blanco_violacion() {
        FacturaFormDto dto = dtoValido();
        dto.setNumeroFactura("   ");
        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("numeroFactura"));
    }

    @Test
    @DisplayName("fechaFactura nula produce violación")
    void fechaFactura_nula_violacion() {
        FacturaFormDto dto = dtoValido();
        dto.setFechaFactura(null);
        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("fechaFactura"));
    }

    @Test
    @DisplayName("proveedor nulo produce violación")
    void proveedor_nulo_violacion() {
        FacturaFormDto dto = dtoValido();
        dto.setProveedor(null);
        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("proveedor"));
    }

    @Test
    @DisplayName("ítem con tipoItem nulo produce violación en cascada")
    void item_tipoItem_nulo_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setTipoItem(null);
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("tipoItem"));
    }

    @Test
    @DisplayName("ítem con nombre en blanco produce violación en cascada")
    void item_nombre_blanco_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setNombre("");
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("nombre"));
    }

    @Test
    @DisplayName("ítem con cantidad cero produce violación en cascada")
    void item_cantidad_cero_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setCantidad(BigDecimal.ZERO);
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("cantidad"));
    }

    @Test
    @DisplayName("ítem con valorUnitario negativo produce violación en cascada")
    void item_valorUnitario_negativo_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setValorUnitario(new BigDecimal("-100"));
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("valorUnitario"));
    }

    @Test
    @DisplayName("ítem con porcentajeDescuento negativo produce violación en cascada")
    void item_porcentajeDescuento_negativo_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setPorcentajeDescuento(new BigDecimal("-1"));
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("porcentajeDescuento"));
    }

    @Test
    @DisplayName("ítem con porcentajeIvaItem negativo produce violación en cascada")
    void item_porcentajeIva_negativo_violacion() {
        FacturaFormDto dto = dtoValido();
        FacturaItemDto item = itemValido();
        item.setPorcentajeIvaItem(new BigDecimal("-5"));
        dto.setItems(List.of(item));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().contains("porcentajeIvaItem"));
    }

    @Test
    @DisplayName("lista de ítems vacía no produce violaciones")
    void items_listaVacia_sinViolaciones() {
        FacturaFormDto dto = dtoValido();
        dto.setItems(List.of());

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("ítem válido en la lista no produce violaciones")
    void item_valido_sinViolaciones() {
        FacturaFormDto dto = dtoValido();
        dto.setItems(List.of(itemValido()));

        Set<ConstraintViolation<FacturaFormDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }
}
