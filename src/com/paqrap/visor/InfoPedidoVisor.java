package com.paqrap.visor;

/**
 * Representa el estado de un pedido para su visualización.
 */
public class InfoPedidoVisor {
    private final String id;
    private final String destinoId;
    private final int x;
    private final int y;
    private final int cantidad;
    private final double tiempoLiberacion;
    private final double plazoHoras;
    private final double tiempoMaximoEntrega;
    private String estado; // PENDIENTE, EN_TRANSITO, ENTREGADO, TARDE, NO_ASIGNADO
    private Double tiempoEntrega;
    private String relojEntrega;
    private String vehiculoAsignado;

    public InfoPedidoVisor(String id, String destinoId, int x, int y, int cantidad, double tiempoLiberacion, double plazoHoras) {
        this.id = id;
        this.destinoId = destinoId;
        this.x = x;
        this.y = y;
        this.cantidad = cantidad;
        this.tiempoLiberacion = tiempoLiberacion;
        this.plazoHoras = plazoHoras;
        this.tiempoMaximoEntrega = tiempoLiberacion + plazoHoras;
        this.estado = "PENDIENTE";
        this.tiempoEntrega = null;
        this.relojEntrega = null;
        this.vehiculoAsignado = null;
    }

    public String getId() { return id; }
    public String getDestinoId() { return destinoId; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getCantidad() { return cantidad; }
    public double getTiempoLiberacion() { return tiempoLiberacion; }
    public double getPlazoHoras() { return plazoHoras; }
    public double getTiempoMaximoEntrega() { return tiempoMaximoEntrega; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Double getTiempoEntrega() { return tiempoEntrega; }
    public void setTiempoEntrega(Double tiempoEntrega) { this.tiempoEntrega = tiempoEntrega; }
    public String getRelojEntrega() { return relojEntrega; }
    public void setRelojEntrega(String relojEntrega) { this.relojEntrega = relojEntrega; }
    public String getVehiculoAsignado() { return vehiculoAsignado; }
    public void setVehiculoAsignado(String vehiculoAsignado) { this.vehiculoAsignado = vehiculoAsignado; }
}
