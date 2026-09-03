package com.paqrap.modelo;

import java.util.*;

public class Solucion {
    private final List<Ruta> rutas;
    private final List<Pedido> pedidosNoAsignados;

    public Solucion() {
        this.rutas = new ArrayList<>();
        this.pedidosNoAsignados = new ArrayList<>();
    }

    public Solucion copiar() {
        Solucion sol = new Solucion();
        for (Ruta r : this.rutas) {
            sol.rutas.add(r.copiar());
        }
        sol.pedidosNoAsignados.addAll(this.pedidosNoAsignados);
        return sol;
    }

    public List<Ruta> getRutas() {
        return rutas;
    }

    public List<Pedido> getPedidosNoAsignados() {
        return pedidosNoAsignados;
    }

    public double calcularCostoTotal() {
        double costo = 0.0;
        for (Ruta r : rutas) {
            costo += r.getCostoTotal();
        }
        // Penalización por pedidos no asignados
        costo += pedidosNoAsignados.size() * 10000.0;
        return costo;
    }

    @Override
    public String toString() {
        return String.format("Solucion[Rutas:%d, NoAsignados:%d, CostoTotal:S/%.2f]",
                rutas.size(), pedidosNoAsignados.size(), calcularCostoTotal());
    }
}
