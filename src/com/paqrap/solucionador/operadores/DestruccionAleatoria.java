package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

public class DestruccionAleatoria implements OperadorDestruccion {

    @Override
    public String getNombre() {
        return "Destrucción Aleatoria";
    }

    @Override
    public List<Pedido> destruir(Solucion solucion, int q, ContextoProblema contexto) {
        List<Pedido> removidos = new ArrayList<>();
        Random random = new Random();

        List<Pedido> todosAsignados = new ArrayList<>();
        for (Ruta r : solucion.getRutas()) {
            todosAsignados.addAll(r.getPedidosAsignados());
        }

        if (todosAsignados.isEmpty()) return removidos;

        int aRemover = Math.min(q, todosAsignados.size());
        for (int i = 0; i < aRemover; i++) {
            int indice = random.nextInt(todosAsignados.size());
            Pedido p = todosAsignados.remove(indice);
            removidos.add(p);

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
