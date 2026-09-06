package com.paqrap.visor;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa el estado interpolado y dinámico de un vehículo para su visualización.
 */
public class InfoVehiculoVisor {
    private final String id;
    private final String tipo;
    private double x;
    private double y;
    private final int capacidadTotal;
    private int cargaActual;
    private String estado; // EN_ESPERA, EN_RUTA, ENTREGANDO, REFRIGERIO, RETORNANDO, EN_ALMACEN, AVERIADO
    private String almacenBase;
    private String pedidoActual;
    private String detalleEstado;
    private final List<double[]> trayectoriaRecorrida;

    public InfoVehiculoVisor(String id, String tipo, double x, double y, int capacidadTotal, String almacenBase) {
        this.id = id;
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.capacidadTotal = capacidadTotal;
        this.cargaActual = 0;
        this.estado = "EN_ESPERA";
        this.almacenBase = almacenBase;
        this.pedidoActual = null;
        this.detalleEstado = "En base " + almacenBase;
        this.trayectoriaRecorrida = new ArrayList<>();
        this.trayectoriaRecorrida.add(new double[]{x, y});
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
    public int getCapacidadTotal() { return capacidadTotal; }
    public int getCargaActual() { return cargaActual; }
    public void setCargaActual(int cargaActual) { this.cargaActual = cargaActual; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getAlmacenBase() { return almacenBase; }
    public void setAlmacenBase(String almacenBase) { this.almacenBase = almacenBase; }
    public String getPedidoActual() { return pedidoActual; }
    public void setPedidoActual(String pedidoActual) { this.pedidoActual = pedidoActual; }
    public String getDetalleEstado() { return detalleEstado; }
    public void setDetalleEstado(String detalleEstado) { this.detalleEstado = detalleEstado; }
    public List<double[]> getTrayectoriaRecorrida() { return trayectoriaRecorrida; }

    public void agregarPuntoTrayectoria(double px, double py) {
        if (trayectoriaRecorrida.isEmpty()) {
            trayectoriaRecorrida.add(new double[]{px, py});
            return;
        }
        double[] ult = trayectoriaRecorrida.get(trayectoriaRecorrida.size() - 1);
        if (Math.abs(ult[0] - px) > 0.01 || Math.abs(ult[1] - py) > 0.01) {
            trayectoriaRecorrida.add(new double[]{px, py});
        }
    }
}
