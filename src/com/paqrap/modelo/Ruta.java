package com.paqrap.modelo;

import java.util.*;

public class Ruta {
    private final EstadoVehiculo vehiculo;
    private final List<Pedido> pedidosAsignados;
    private final List<NodoCuadricula> nodosCamino;
    private final List<Double> tiemposLlegada;
    private double distanciaTotal;
    private double costoTotal;

    public Ruta(EstadoVehiculo vehiculo) {
        this.vehiculo = vehiculo.copiar();
        this.pedidosAsignados = new ArrayList<>();
        this.nodosCamino = new ArrayList<>();
        this.tiemposLlegada = new ArrayList<>();
        this.nodosCamino.add(vehiculo.getUbicacionActual());
        this.tiemposLlegada.add(vehiculo.getMarcaTiempoActual());
        this.distanciaTotal = 0.0;
        this.costoTotal = 0.0;
    }

    public Ruta copiar() {
        Ruta r = new Ruta(this.vehiculo);
        r.pedidosAsignados.addAll(this.pedidosAsignados);
        r.nodosCamino.clear();
        r.nodosCamino.addAll(this.nodosCamino);
        r.tiemposLlegada.clear();
        r.tiemposLlegada.addAll(this.tiemposLlegada);
        r.distanciaTotal = this.distanciaTotal;
        r.costoTotal = this.costoTotal;
        return r;
    }

    public EstadoVehiculo getVehiculo() {
        return vehiculo;
    }

    public List<Pedido> getPedidosAsignados() {
        return pedidosAsignados;
    }

    public List<NodoCuadricula> getNodosCamino() {
        return nodosCamino;
    }

    public List<Double> getTiemposLlegada() {
        return tiemposLlegada;
    }

    public double getDistanciaTotal() {
        return distanciaTotal;
    }

    public void setDistanciaTotal(double distanciaTotal) {
        this.distanciaTotal = distanciaTotal;
        this.costoTotal = distanciaTotal * vehiculo.getTipo().getCostoPorKm();
    }

    public double getCostoTotal() {
        return costoTotal;
    }

    @Override
    public String toString() {
        return String.format("Ruta[%s, Pedidos:%d, Dist:%.1fkm, Costo:S/%.2f]",
                vehiculo.getId(), pedidosAsignados.size(), distanciaTotal, costoTotal);
    }
}
