package com.alera.repository;

import com.alera.model.LoteCerveza;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LoteCervezaRepository extends JpaRepository<LoteCerveza, Long> {

    @Query("SELECT l FROM LoteCerveza l ORDER BY l.createdAt DESC")
    List<LoteCerveza> findAllOrderByCreatedAtDesc();

    // Top 5 a nivel de BD — no trae todos los registros
    @Query("SELECT l FROM LoteCerveza l ORDER BY l.createdAt DESC")
    List<LoteCerveza> findTop5(Pageable pageable);

    // Filtros + paginación + rango de fechas
    @Query("SELECT l FROM LoteCerveza l WHERE " +
           "(:estilo = '' OR LOWER(l.estilo) LIKE LOWER(CONCAT('%', :estilo, '%'))) " +
           "AND (:fase = '' OR " +
           "  (:fase = 'COMPLETADO'        AND l.carbFechaFinal   IS NOT NULL) OR " +
           "  (:fase = 'CARBONATACION'     AND l.carbFechaInicial  IS NOT NULL AND l.carbFechaFinal   IS NULL) OR " +
           "  (:fase = 'MADURACION'        AND l.madurFechaInicial IS NOT NULL AND l.carbFechaInicial  IS NULL) OR " +
           "  (:fase = 'ACONDICIONAMIENTO' AND l.acondFechaInicial IS NOT NULL AND l.madurFechaInicial IS NULL) OR " +
           "  (:fase = 'FERMENTACION'      AND l.fermFechaInicial  IS NOT NULL AND l.acondFechaInicial IS NULL) OR " +
           "  (:fase = 'INICIO'            AND l.fermFechaInicial  IS NULL)" +
           ") " +
           "AND (:desde IS NULL OR l.fechaElaboracion >= :desde) " +
           "AND (:hasta IS NULL OR l.fechaElaboracion <= :hasta) " +
           "ORDER BY l.createdAt DESC")
    Page<LoteCerveza> findByFiltros(@Param("estilo") String estilo,
                                    @Param("fase")   String fase,
                                    @Param("desde")  LocalDate desde,
                                    @Param("hasta")  LocalDate hasta,
                                    Pageable pageable);

    // Búsqueda global por código o estilo
    @Query("SELECT l FROM LoteCerveza l WHERE " +
           "LOWER(l.codigoLote) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(l.estilo) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "ORDER BY l.createdAt DESC")
    List<LoteCerveza> search(@Param("q") String q, Pageable pageable);

    @Query("SELECT l FROM LoteCerveza l LEFT JOIN FETCH l.ingredientes WHERE l.id = :id")
    Optional<LoteCerveza> findByIdWithIngredientes(@Param("id") Long id);

    @Query("SELECT MAX(CAST(SUBSTRING(l.codigoLote, LENGTH(:prefix) + 2) AS int)) " +
           "FROM LoteCerveza l WHERE l.codigoLote LIKE CONCAT(:prefix, '-%')")
    Integer findMaxConsecutivoPorPrefix(@Param("prefix") String prefix);

    @Query("SELECT COUNT(l) FROM LoteCerveza l WHERE l.carbFechaFinal IS NULL")
    long countEnProceso();

    @Query("SELECT COUNT(l) FROM LoteCerveza l WHERE l.carbFechaFinal IS NOT NULL")
    long countCompletados();

    // COUNT(DISTINCT estilo) — estadística de estilos únicos
    @Query("SELECT COUNT(DISTINCT l.estilo) FROM LoteCerveza l")
    long countDistinctEstilos();

    @Query("SELECT COUNT(l) FROM LoteCerveza l WHERE l.equipoFermentador.id = :equipoId AND l.carbFechaFinal IS NULL")
    long countLotesActivosByEquipo(@Param("equipoId") Long equipoId);

    // Litros producidos por mes — últimos N meses
    @Query(value = "SELECT CAST(EXTRACT(YEAR  FROM fecha_elaboracion) AS integer), " +
                   "       CAST(EXTRACT(MONTH FROM fecha_elaboracion) AS integer), " +
                   "       COALESCE(SUM(litros_finales), 0) " +
                   "FROM lotes_cerveza " +
                   "WHERE fecha_elaboracion >= :desde AND fecha_elaboracion IS NOT NULL " +
                   "AND deleted_at IS NULL AND tenant_id = :tenantId " +
                   "GROUP BY 1, 2 ORDER BY 1, 2",
           nativeQuery = true)
    List<Object[]> findLitrosPorMes(@Param("desde") LocalDate desde, @Param("tenantId") String tenantId);

    // Top estilos por cantidad de lotes
    @Query(value = "SELECT estilo, COUNT(*) FROM lotes_cerveza " +
                   "WHERE tenant_id = :tenantId AND deleted_at IS NULL " +
                   "GROUP BY estilo ORDER BY COUNT(*) DESC LIMIT 6",
           nativeQuery = true)
    List<Object[]> findLotesPorEstilo(@Param("tenantId") String tenantId);

    // Lotes creados desde una receta
    @Query("SELECT l FROM LoteCerveza l WHERE l.receta.id = :recetaId ORDER BY l.fechaElaboracion DESC NULLS LAST")
    List<LoteCerveza> findByRecetaId(@Param("recetaId") Long recetaId);

    // Comparativa: carga ingredientes para calcular eficiencia sin N+1
    @Query("SELECT DISTINCT l FROM LoteCerveza l LEFT JOIN FETCH l.ingredientes WHERE l.id IN :ids")
    List<LoteCerveza> findByIds(@Param("ids") List<Long> ids);

    // Kanban: lotes activos + completados recientes
    @Query("SELECT l FROM LoteCerveza l WHERE l.carbFechaFinal IS NULL OR l.carbFechaFinal >= :limite ORDER BY l.createdAt ASC")
    List<LoteCerveza> findParaKanban(@Param("limite") LocalDate limite);

    // Reporte de producción por período
    @Query("SELECT l FROM LoteCerveza l WHERE l.fechaElaboracion BETWEEN :desde AND :hasta ORDER BY l.fechaElaboracion DESC")
    List<LoteCerveza> findByPeriodo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(value = "SELECT estilo, COUNT(*) as cantidad, COALESCE(SUM(litros_finales),0) as litros " +
                   "FROM lotes_cerveza WHERE fecha_elaboracion BETWEEN :desde AND :hasta " +
                   "AND tenant_id = :tenantId AND deleted_at IS NULL " +
                   "GROUP BY estilo ORDER BY litros DESC",
           nativeQuery = true)
    List<Object[]> findResumenPorEstilo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
                                        @Param("tenantId") String tenantId);
}
