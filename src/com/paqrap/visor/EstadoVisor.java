package com.paqrap.visor;

import com.paqrap.simulador.EventoSimulacion;
import java.util.*;

/**
 * Snapshot del estado de la simulación en un instante de tiempo t determinado.
 */
public class EstadoVisor {
    private final double tiempoHoras;
    private final String reloj;
    private final Map<String, InfoVehiculoVisor> vehiculos;
    private final Map<String, InfoPedidoVisor> pedidos;
    private final List<TramoBloqueado> bloqueosActivos;
    private final Set<String> vehiculosAveriados;
    private final List<EventoSimulacion> eventosRecientes;
    private int pedidosEntregados;
    private int pedidosPendientes;
    private int pedidosEnTransito;
    private double distanciaTotalKm;
    private double costoTotalSoles;

    public EstadoVisor(double tiempoHoras, String reloj) {
        this.tiempoHoras = tiempoHoras;
        this.reloj = reloj;
        this.vehiculos = new LinkedHashMap<>();
        this.pedidos = new LinkedHashMap<>();
        this.bloqueosActivos = new ArrayList<>();
        this.vehiculosAveriados = new HashSet<>();
        this.eventosRecientes = new ArrayList<>();
        this.pedidosEntregados = 0;
        this.pedidosPendientes = 0;
        this.pedidosEnTransito = 0;
        this.distanciaTotalKm = 0.0;
        this.costoTotalSoles = 0.0;
    }

    public double getTiempoHoras() { return tiempoHoras; }
    public String getReloj() { return reloj; }
    public Map<String, InfoVehiculoVisor> getVehiculos() { return vehiculos; }
    public Map<String, InfoPedidoVisor> getPedidos() { return pedidos; }
    public List<TramoBloqueado> getBloqueosActivos() { return bloqueosActivos; }
    public Set<String> getVehiculosAveriados() { return vehiculosAveriados; }
    public List<EventoSimulacion> getEventosRecientes() { return eventosRecientes; }

    public int getPedidosEntregados() { return pedidosEntregados; }
    public void setPedidosEntregados(int pedidosEntregados) { this.pedidosEntregados = pedidosEntregados; }
    public int getPedidosPendientes() { return pedidosPendientes; }
    public void setPedidosPendientes(int pedidosPendientes) { this.pedidosPendientes = pedidosPendientes; }
    public int getPedidosEnTransito() { return pedidosEnTransito; }
    public void setPedidosEnTransito(int pedidosEnTransito) { this.pedidosEnTransito = pedidosEnTransito; }
    public double getDistanciaTotalKm() { return distanciaTotalKm; }
    public void setDistanciaTotalKm(double distanciaTotalKm) { this.distanciaTotalKm = distanciaTotalKm; }
    public double getCostoTotalSoles() { return costoTotalSoles; }
    public void setCostoTotalSoles(double costoTotalSoles) { this.costoTotalSoles = costoTotalSoles; }
}
