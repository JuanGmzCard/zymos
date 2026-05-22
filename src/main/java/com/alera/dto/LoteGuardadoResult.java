package com.alera.dto;

import com.alera.model.LoteCerveza;
import java.util.List;

public class LoteGuardadoResult {

    private final LoteCerveza lote;
    private final List<String> advertencias;

    public LoteGuardadoResult(LoteCerveza lote, List<String> advertencias) {
        this.lote = lote;
        this.advertencias = advertencias;
    }

    public LoteCerveza getLote() { return lote; }
    public List<String> getAdvertencias() { return advertencias; }
    public boolean tieneAdvertencias() { return !advertencias.isEmpty(); }

    public String getMensajeAdvertencias() {
        if (advertencias.isEmpty()) return null;
        return "Stock insuficiente para: " + String.join(", ", advertencias)
                + ". El lote se guardó pero verifica el inventario.";
    }
}