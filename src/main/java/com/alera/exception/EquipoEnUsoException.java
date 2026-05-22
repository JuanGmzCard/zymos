package com.alera.exception;

public class EquipoEnUsoException extends RuntimeException {
    public EquipoEnUsoException(String nombreEquipo, long totalLotes) {
        super("El equipo \"" + nombreEquipo + "\" tiene " + totalLotes
                + " lote(s) activo(s) asignado(s). Finaliza o reasigna los lotes antes de eliminarlo.");
    }
}