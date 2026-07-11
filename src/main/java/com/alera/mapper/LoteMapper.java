package com.alera.mapper;

import com.alera.dto.InsumoDto;
import com.alera.dto.LoteFormDto;
import com.alera.model.Ingrediente;
import com.alera.model.LoteCerveza;
import com.alera.model.LoteItemFactura;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface LoteMapper {

    @Mapping(source = "equipoFermentador.id", target = "equipoFermentadorId")
    @Mapping(source = "receta.id",            target = "recetaId")
    @Mapping(source = "receta2.id",           target = "receta2Id")
    @Mapping(source = "receta3.id",           target = "receta3Id")
    @Mapping(source = "receta4.id",           target = "receta4Id")
    @Mapping(target = "itemsIds",             ignore = true)
    @Mapping(target = "itemsCantidades",      ignore = true)
    @Mapping(source = "maltas",      target = "maltas",        qualifiedByName = "ingredientesAInsumos")
    @Mapping(source = "lupulos",     target = "lupulos",       qualifiedByName = "ingredientesAInsumos")
    @Mapping(source = "levaduras",   target = "levaduras",     qualifiedByName = "ingredientesAInsumos")
    @Mapping(source = "clarificantes", target = "clarificantes", qualifiedByName = "ingredientesAInsumos")
    LoteFormDto toLoteFormDto(LoteCerveza lote);

    @AfterMapping
    default void mapInstrumentoMedicion(LoteCerveza lote, @MappingTarget LoteFormDto dto) {
        dto.setInstrumentoMedicion(lote.getOgBrix() != null ? "BRIX" : "SG");
    }

    @AfterMapping
    default void mapItemsFactura(LoteCerveza lote, @MappingTarget LoteFormDto dto) {
        List<Long> ids = new ArrayList<>();
        List<BigDecimal> cantidades = new ArrayList<>();
        if (lote.getItemsFactura() != null) {
            for (LoteItemFactura lif : lote.getItemsFactura()) {
                ids.add(lif.getItem().getId());
                cantidades.add(lif.getCantidadAsignada());
            }
        }
        dto.setItemsIds(ids);
        dto.setItemsCantidades(cantidades);
    }

    // Convierte List<Ingrediente> → List<InsumoDto> parseando "5000 gr" → {cantidad, unidad}
    // Garantiza al menos una fila vacía para que el formulario siempre tenga un campo visible.
    @Named("ingredientesAInsumos")
    default List<InsumoDto> ingredientesAInsumos(List<Ingrediente> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            List<InsumoDto> vacia = new ArrayList<>();
            vacia.add(new InsumoDto());
            return vacia;
        }
        return ingredientes.stream().map(i -> {
            InsumoDto dto = new InsumoDto();
            dto.setNombre(i.getNombre());
            String cant = i.getCantidad();
            if (cant != null && cant.contains(" ")) {
                String[] parts = cant.split("\\s+", 2);
                dto.setCantidad(parts[0]);
                dto.setUnidad(parts[1]);
            } else {
                dto.setCantidad(cant);
                dto.setUnidad("gr");
            }
            return dto;
        }).collect(Collectors.toList());
    }
}