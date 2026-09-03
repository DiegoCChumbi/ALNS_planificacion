package com.paqrap.modelo;

import java.util.Objects;

public class Pedido {
    private final String id;
    private final NodoCuadricula destino;
    private final int cantidad;
    private final double tiempoLiberacion; // Horas desde inicio de simulación
    private final double plazoHoras; // 4, 8, 12, 18 o 36
    private final double tiempoMaximoEntrega;

    public Pedido(String id, NodoCuadricula destino, int cantidad, double tiempoLiberacion, double plazoHoras) {
        this.id = id;
        this.destino = destino;
        this.cantidad = cantidad;
        this.tiempoLiberacion = tiempoLiberacion;
        this.plazoHoras = plazoHoras;
        this.tiempoMaximoEntrega = tiempoLiberacion + plazoHoras;
    }

    public String getId() {
        return id;
    }

    public NodoCuadricula getDestino() {
        return destino;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getTiempoLiberacion() {
        return tiempoLiberacion;
    }

    public double getPlazoHoras() {
        return plazoHoras;
    }

    public double getTiempoMaximoEntrega() {
        return tiempoMaximoEntrega;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pedido pedido = (Pedido) o;
        return Objects.equals(id, pedido.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Pedido[%s, Cant:%d, Dest:%s, Plazo:%.1fh]", id, cantidad, destino.getId(), plazoHoras);
    }
}
