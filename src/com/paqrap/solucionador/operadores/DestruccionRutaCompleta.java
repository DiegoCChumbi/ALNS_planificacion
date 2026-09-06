package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

/**
 * Operador de Ruina: Eliminación de Ruta Completa (Route Removal).
 * Referencia: Nagata & Bräysy (2009), Friedrich & Elbert (2022).
 * Selecciona una ruta completa activa y extrae sus clientes para forzar
 * la reasignación hacia otros vehículos, incentivando la reducción de flota y consolidación de carga.
 */
public class DestruccionRutaCompleta implements OperadorDestruccion {

    @Override
    public String getNombre() {
        return "Destrucción de Ruta Completa (Route Removal)";
    }

    @Override
    public List<Pedido> destruir(Solucion solucion, int q, ContextoProblema contexto) {
        List<Pedido> removidos = new ArrayList<>();

        List<Ruta> rutasActivas = new ArrayList<>();
        for (Ruta r : solucion.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) {
                rutasActivas.add(r);
            }
        }

        if (rutasActivas.isEmpty()) return removidos;

        Random random = new Random();
        Collections.shuffle(rutasActivas, random);

        for (Ruta ruta : rutasActivas) {
            if (removidos.size() >= q) break;

            List<Pedido> pedidosRuta = new ArrayList<>(ruta.getPedidosAsignados());
            for (Pedido p : pedidosRuta) {
                ruta.getPedidosAsignados().remove(p);
                removidos.add(p);
                if (removidos.size() >= q) break;
            }
            EvaluadorCostos.recalculareRuta(ruta, contexto.getMapa(), contexto.getConfiguracionOperacion());
        }

        return removidos;
    }
}
