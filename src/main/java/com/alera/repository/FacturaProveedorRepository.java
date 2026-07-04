package com.alera.repository;

import com.alera.model.FacturaProveedor;
import com.alera.model.enums.EstadoFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {

    @Query("SELECT DISTINCT f FROM FacturaProveedor f LEFT JOIN FETCH f.items WHERE " +
           "(:estado IS NULL OR f.estado = :estado) AND " +
           "(:desde  IS NULL OR f.fechaFactura >= :desde) AND " +
           "(:hasta  IS NULL OR f.fechaFactura <= :hasta) " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, f.id DESC")
    List<FacturaProveedor> findWithItemsFiltered(
            @Param("estado") EstadoFactura estado,
            @Param("desde")  LocalDate desde,
            @Param("hasta")  LocalDate hasta);

    @Query("SELECT f FROM FacturaProveedor f WHERE " +
           "(:estado IS NULL OR f.estado = :estado) AND " +
           "(:desde  IS NULL OR f.fechaFactura >= :desde) AND " +
           "(:hasta  IS NULL OR f.fechaFactura <= :hasta) " +
           "ORDER BY f.fechaFactura DESC NULLS LAST, f.id DESC")
    Page<FacturaProveedor> findAllFiltered(
            @Param("estado") EstadoFactura estado,
            @Param("desde")  LocalDate desde,
            @Param("hasta")  LocalDate hasta,
            Pageable pageable);

    @Query("SELECT f FROM FacturaProveedor f LEFT JOIN FETCH f.items WHERE f.id = :id")
    Optional<FacturaProveedor> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT f FROM FacturaProveedor f WHERE " +
           "LOWER(COALESCE(f.numeroFactura,'')) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(COALESCE(f.proveedor,''))    LIKE LOWER(CONCAT('%',:q,'%')) " +
           "ORDER BY f.fechaFactura DESC NULLS LAST")
    List<FacturaProveedor> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT SUM(f.valorTotal) FROM FacturaProveedor f")
    BigDecimal sumTotalFacturas();

    @Query("SELECT COUNT(f) FROM FacturaProveedor f")
    long countTotal();

    @Query("SELECT COALESCE(SUM(f.valorTotal), 0) FROM FacturaProveedor f WHERE " +
           "(:estado IS NULL OR f.estado = :estado) AND " +
           "(:desde  IS NULL OR f.fechaFactura >= :desde) AND " +
           "(:hasta  IS NULL OR f.fechaFactura <= :hasta)")
    BigDecimal sumTotalFiltered(@Param("estado") EstadoFactura estado,
                                @Param("desde")  LocalDate desde,
                                @Param("hasta")  LocalDate hasta);

    @Query("SELECT COALESCE(SUM(f.valorTotal), 0) FROM FacturaProveedor f WHERE " +
           "f.estado IN :estados AND " +
           "(:desde  IS NULL OR f.fechaFactura >= :desde) AND " +
           "(:hasta  IS NULL OR f.fechaFactura <= :hasta)")
    BigDecimal sumPorEstados(@Param("estados") Collection<EstadoFactura> estados,
                             @Param("desde")   LocalDate desde,
                             @Param("hasta")   LocalDate hasta);

    @Query("SELECT COUNT(f) FROM FacturaProveedor f WHERE " +
           "f.estado IN :estados AND " +
           "(:desde  IS NULL OR f.fechaFactura >= :desde) AND " +
           "(:hasta  IS NULL OR f.fechaFactura <= :hasta)")
    long countPorEstados(@Param("estados") Collection<EstadoFactura> estados,
                         @Param("desde")   LocalDate desde,
                         @Param("hasta")   LocalDate hasta);

    @Query("SELECT f FROM FacturaProveedor f WHERE " +
           "f.estado IN :estados AND " +
           "(f.fechaFactura IS NULL OR f.fechaFactura <= :umbral) " +
           "ORDER BY f.fechaFactura ASC NULLS LAST")
    List<FacturaProveedor> findSinProcesar(@Param("estados") Collection<EstadoFactura> estados,
                                           @Param("umbral")  LocalDate umbral);
}
