package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.CalculadorDistancia;
import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

public class DestruccionShaw implements OperadorDestruccion {
    private final double pesoDistancia;
    private final double pesoTiempo;

    public DestruccionShaw(double pesoDistancia, double pesoTiempo) {
        this.pesoDistancia = pesoDistancia;
        this.pesoTiempo = pesoTiempo;
    }

    public DestruccionShaw() {
        this(1.0, 2.0);
    }

    @Override
    public String getNombre() {
        return "Destrucción Relacionada (Shaw)";
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
        Pedido semilla = todosAsignados.get(random.nextInt(todosAsignados.size()));
        removidos.add(semilla);
        todosAsignados.remove(semilla);

        while (removidos.size() < q && !todosAsignados.isEmpty()) {
            Pedido referencia = removidos.get(random.nextInt(removidos.size()));

            Pedido masRelacionado = null;
            double menorRelacion = Double.MAX_VALUE;

            for (Pedido p : todosAsignados) {
                double dist = CalculadorDistancia.getDistancia(contexto.getMapa(), referencia.getDestino(), p.getDestino());
                double difPlazo = Math.abs(referencia.getPlazoHoras() - p.getPlazoHoras());
                double relacion = dist * pesoDistancia + difPlazo * pesoTiempo;

                if (relacion < menorRelacion) {
                    menorRelacion = relacion;
                    masRelacionado = p;
                }
            }

            if (masRelacionado != null) {
                removidos.add(masRelacionado);
                todosAsignados.remove(masRelacionado);
            } else {
                break;
            }
        }

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
