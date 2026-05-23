package com.alera.mapper;

import com.alera.dto.MantenimientoDto;
import com.alera.model.MantenimientoEquipo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MantenimientoMapper {

    @Mapping(target = "id",     ignore = true)
    @Mapping(target = "equipo", ignore = true)
    MantenimientoEquipo toEntity(MantenimientoDto dto);
}
