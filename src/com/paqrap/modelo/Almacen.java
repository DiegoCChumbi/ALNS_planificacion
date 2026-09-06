package com.paqrap.modelo;

/**
 * Representa un almacén (Centro de Distribución Central o Hub Intermedio).
 * Cumple con la regla de negocio:
 * - Almacén Central: inventario permanente/infinito.
 * - Almacenes Intermedios: capacidad máxima de 1,000 unidades y recarga cada 24 horas (23:59:59).
 */
public class Almacen extends NodoCuadricula {

    private final String nombre;
    private final boolean esCentral;
    private final int capacidadMaxima;
    private int stockDisponible;

    public Almacen(String id, String nombre, int x, int y, boolean esCentral, int capacidadMaxima, int stockDisponible) {
        super(id, x, y, esCentral ? TipoNodo.ALMACEN_CENTRAL : TipoNodo.ALMACEN_INTERMEDIO);
        this.nombre = nombre;
        this.esCentral = esCentral;
        this.capacidadMaxima = esCentral ? Integer.MAX_VALUE : capacidadMaxima;
        this.stockDisponible = esCentral ? Integer.MAX_VALUE : stockDisponible;
    }

    public Almacen(String id, String nombre, int x, int y, boolean esCentral) {
        this(id, nombre, x, y, esCentral, esCentral ? Integer.MAX_VALUE : 1000, esCentral ? Integer.MAX_VALUE : 1000);
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esCentral() {
        return esCentral;
    }

    public int getCapacidadMaxima() {
        return capacidadMaxima;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }

    public void setStockDisponible(int stockDisponible) {
        if (!esCentral) {
            this.stockDisponible = Math.min(stockDisponible, capacidadMaxima);
        }
    }

    /**
     * Despacha unidades del almacén si hay stock suficiente.
     * @param cantidad Cantidad de unidades de producto P a despachar
     * @return true si se pudo despachar, false si no hay stock suficiente
     */
    public boolean despachar(int cantidad) {
        if (esCentral) return true;
        if (stockDisponible >= cantidad) {
            stockDisponible -= cantidad;
            return true;
        }
        return false;
    }

    /**
     * Recarga instantánea del almacén intermedio a su capacidad máxima (23:59:59).
     */
    public void recargar() {
        if (!esCentral) {
            this.stockDisponible = this.capacidadMaxima;
        }
    }

    @Override
    public String toString() {
        return String.format("%s[%s (%d,%d), Stock: %s/%s]",
                getId(), nombre, getX(), getY(),
                esCentral ? "∞" : String.valueOf(stockDisponible),
                esCentral ? "∞" : String.valueOf(capacidadMaxima));
    }
}
