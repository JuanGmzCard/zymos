package com.alera.repository;

import com.alera.model.FacturaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacturaItemRepository extends JpaRepository<FacturaItem, Long> {

    // Historial de precios — busca por nombre (case-insensitive, trimmed) a través de todas las facturas
    @Query("SELECT fi FROM FacturaItem fi JOIN FETCH fi.factura f " +
           "WHERE LOWER(TRIM(fi.nombre)) = LOWER(TRIM(:nombre)) " +
           "AND fi.cantidad IS NOT NULL AND fi.cantidad > 0 " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, fi.id DESC")
    List<FacturaItem> findHistorialPreciosPorNombre(@Param("nombre") String nombre);

    // Nombres distintos de ítems de factura para el datalist de búsqueda
    @Query("SELECT DISTINCT fi.nombre FROM FacturaItem fi WHERE fi.nombre IS NOT NULL ORDER BY fi.nombre ASC")
    List<String> findNombresDistintos();

    // Últimas compras de una lista de ingredientes — para estimación de costo en recetas
    @Query("SELECT fi FROM FacturaItem fi JOIN FETCH fi.factura f " +
           "WHERE LOWER(TRIM(fi.nombre)) IN :nombres " +
           "AND fi.valorUnitario IS NOT NULL AND fi.valorUnitario > 0 " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, fi.id DESC")
    List<FacturaItem> findUltimosPrecios(@Param("nombres") List<String> nombres);
}