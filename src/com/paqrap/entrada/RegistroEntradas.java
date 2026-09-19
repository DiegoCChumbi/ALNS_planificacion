package com.paqrap.entrada;

import com.paqrap.modelo.Pedido;
import java.util.*;

/**
 * Registro incremental de pedidos y bloqueos, útil tanto para lectores como para APIs.
 */
public final class RegistroEntradas {
    private final List<Pedido> pedidos = new ArrayList<>();
    private final List<BloqueoTemporal> bloqueos = new ArrayList<>();
    private final Set<String> idsPedidos = new HashSet<>();
    private int archivosVentasLeidos;
    private int archivosBloqueosLeidos;

    public RegistroEntradas registrarPedido(Pedido pedido) {
        Objects.requireNonNull(pedido, "pedido");
        if (!idsPedidos.add(pedido.getId())) {
            throw new EntradaInvalidaException("Pedido duplicado: " + pedido.getId());
        }
        pedidos.add(pedido);
        return this;
    }

    public RegistroEntradas registrarBloqueo(BloqueoTemporal bloqueo) {
        bloqueos.add(Objects.requireNonNull(bloqueo, "bloqueo"));
        return this;
    }

    public List<Pedido> getPedidos() { return Collections.unmodifiableList(pedidos); }
    public List<BloqueoTemporal> getBloqueos() { return Collections.unmodifiableList(bloqueos); }
    public int getCantidadPedidos() { return pedidos.size(); }
    public int getCantidadBloqueos() { return bloqueos.size(); }
    public boolean tienePedidoId(String id) { return idsPedidos.contains(id); }
    public int getArchivosVentasLeidos() { return archivosVentasLeidos; }
    public int getArchivosBloqueosLeidos() { return archivosBloqueosLeidos; }

    void incrementarArchivosVentas() { archivosVentasLeidos++; }
    void incrementarArchivosBloqueos() { archivosBloqueosLeidos++; }

    public int getMaximaX() {
        int max = 0;
        for (Pedido p : pedidos) max = Math.max(max, p.getDestino().getX());
        for (BloqueoTemporal b : bloqueos) for (BloqueoTemporal.Arista a : b.getAristas()) {
            max = Math.max(max, Math.max(a.getX1(), a.getX2()));
        }
        return max;
    }

    public int getMaximaY() {
        int max = 0;
        for (Pedido p : pedidos) max = Math.max(max, p.getDestino().getY());
        for (BloqueoTemporal b : bloqueos) for (BloqueoTemporal.Arista a : b.getAristas()) {
            max = Math.max(max, Math.max(a.getY1(), a.getY2()));
        }
        return max;
    }
}
