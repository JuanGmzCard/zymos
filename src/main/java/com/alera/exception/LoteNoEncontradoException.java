package com.alera.exception;

public class LoteNoEncontradoException extends RuntimeException {
    public LoteNoEncontradoException(Long id) {
        super("Lote no encontrado: " + id);
    }
}