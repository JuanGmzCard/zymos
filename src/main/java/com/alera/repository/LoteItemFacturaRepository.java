package com.alera.repository;

import com.alera.model.LoteItemFactura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LoteItemFacturaRepository extends JpaRepository<LoteItemFactura, Long> {

    // Costo total por lote: replica la lógica de LoteItemFactura.getValorAsignado()
    // cantidad = 0 → costo completo del ítem; sino → proporcional
    @Query("SELECT lif.lote.id, " +
           "COALESCE(SUM(CASE WHEN lif.cantidadAsignada = 0 THEN lif.item.valorLinea " +
           "               WHEN lif.item.cantidad IS NULL OR lif.item.cantidad = 0 THEN 0 " +
           "               ELSE lif.cantidadAsignada * lif.item.valorLinea / lif.item.cantidad " +
           "          END), 0) " +
           "FROM LoteItemFactura lif GROUP BY lif.lote.id")
    List<Object[]> sumCostosPorLote();
}
