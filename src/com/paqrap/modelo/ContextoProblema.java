package com.paqrap.modelo;

import java.util.*;

public class ContextoProblema {
    private final double marcaTiempoActual;
    private final MapaCuadricula mapa;
    private final List<NodoCuadricula> almacenes;
    private final List<Pedido> pedidosPendientes;
    private final List<EstadoVehiculo> vehiculosActivos;

    public ContextoProblema(double marcaTiempoActual, MapaCuadricula mapa) {
        this.marcaTiempoActual = marcaTiempoActual;
        this.mapa = mapa;
        this.almacenes = new ArrayList<>();
        this.pedidosPendientes = new ArrayList<>();
        this.vehiculosActivos = new ArrayList<>();
    }

    public double getMarcaTiempoActual() {
        return marcaTiempoActual;
    }

    public MapaCuadricula getMapa() {
        return mapa;
    }

    public List<NodoCuadricula> getAlmacenes() {
        return almacenes;
    }

    public List<Pedido> getPedidosPendientes() {
        return pedidosPendientes;
    }

    public List<EstadoVehiculo> getVehiculosActivos() {
        return vehiculosActivos;
    }

    public void agregarAlmacen(NodoCuadricula nodo) {
        almacenes.add(nodo);
    }

    public void agregarPedido(Pedido pedido) {
        pedidosPendientes.add(pedido);
    }

    public void agregarVehiculo(EstadoVehiculo vehiculo) {
        vehiculosActivos.add(vehiculo);
    }
}
