package com.paqrap.entrada;

/**
 * Indica que una línea de entrada no respeta el formato esperado.
 */
public class EntradaInvalidaException extends IllegalArgumentException {
    public EntradaInvalidaException(String mensaje) {
        super(mensaje);
    }

    public EntradaInvalidaException(String fuente, int linea, String contenido, String detalle) {
        super(String.format("%s:%d: %s (línea: %s)", fuente, linea, detalle, contenido));
    }
}
