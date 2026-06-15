package com.alera.repository;

import com.alera.model.FacturaItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    // Ítems puntuales por id — para precargar los ya asignados a un lote en el formulario
    @Query("SELECT fi FROM FacturaItem fi JOIN FETCH fi.factura f WHERE fi.id IN :ids")
    List<FacturaItem> findByIdIn(@Param("ids") List<Long> ids);

    // Ítems por nombre exacto (case-insensitive, trimmed) — para auto-sugerir costos al cargar una receta
    @Query("SELECT fi FROM FacturaItem fi JOIN FETCH fi.factura f " +
           "WHERE LOWER(TRIM(fi.nombre)) IN :nombres " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, fi.id DESC")
    List<FacturaItem> findByNombresIn(@Param("nombres") List<String> nombres);

    // Búsqueda paginada para el buscador AJAX de "Costos de Producción" del formulario de lote
    @Query("SELECT fi FROM FacturaItem fi JOIN FETCH fi.factura f " +
           "WHERE (:q = '' OR LOWER(fi.nombre) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "   OR LOWER(COALESCE(f.numeroFactura,'')) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "   OR LOWER(COALESCE(f.proveedor,''))    LIKE LOWER(CONCAT('%',:q,'%'))) " +
           "AND (:tipo = '' OR fi.tipoInsumo = :tipo) " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, fi.id DESC")
    Page<FacturaItem> search(@Param("q") String q, @Param("tipo") String tipo, Pageable pageable);
}