package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.CalculadorDistancia;
import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

/**
 * Operador de Ruina: Eliminación por Conglomerados / Zonas Geográficas (Cluster / Radial Removal).
 * Referencia: Ropke & Pisinger (2006b), Friedrich & Elbert (2022).
 * Selecciona un cliente semilla y elimina sus vecinos más próximos en la cuadrícula,
 * permitiendo rediseñar y consolidar zonas geográficas completas.
 */
public class DestruccionCluster implements OperadorDestruccion {

    @Override
    public String getNombre() {
        return "Destrucción por Conglomerado (Cluster Removal)";
    }

    @Override
    public List<Pedido> destruir(Solucion solucion, int q, ContextoProblema contexto) {
        List<Pedido> removidos = new ArrayList<>();
        List<Pedido> todosAsignados = new ArrayList<>();

        for (Ruta r : solucion.getRutas()) {
            todosAsignados.addAll(r.getPedidosAsignados());
        }

        if (todosAsignados.isEmpty()) return removidos;

        Random random = new Random();
        Pedido semilla = todosAsignados.remove(random.nextInt(todosAsignados.size()));
        removidos.add(semilla);

        // Ordenar los demás pedidos por cercanía geográfica al nodo semilla
        todosAsignados.sort(Comparator.comparingDouble(p ->
                CalculadorDistancia.getDistancia(contexto.getMapa(), semilla.getDestino(), p.getDestino())));

        int aRemover = Math.min(q - 1, todosAsignados.size());
        for (int i = 0; i < aRemover; i++) {
            removidos.add(todosAsignados.get(i));
        }

        // Remover de las rutas y recalcular
        for (Pedido p : removidos) {
            for (Ruta r : solucion.getRutas()) {
                if (r.getPedidosAsignados().remove(p)) {
                    EvaluadorCostos.recalculareRuta(r, contexto.getMapa(), contexto.getConfiguracionOperacion());
                    break;
                }
            }
        }

        return removidos;
    }
}
