package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import java.util.*;

public class ReparacionVoraz implements OperadorReparacion {

    @Override
    public String getNombre() {
        return "Inserción Voraz (Greedy)";
    }

    @Override
    public void reparar(Solucion solucion, List<Pedido> pedidosNoAsignados, ContextoProblema contexto) {
        Iterator<Pedido> iterador = pedidosNoAsignados.iterator();

        while (iterador.hasNext()) {
            Pedido pedido = iterador.next();
            Ruta mejorRuta = null;
            int mejorIndiceInsercion = -1;
            double menorIncrementoCosto = Double.MAX_VALUE;

            for (Ruta ruta : solucion.getRutas()) {
                double costoActual = ruta.getCostoTotal();

                for (int pos = 0; pos <= ruta.getPedidosAsignados().size(); pos++) {
                    Ruta rutaPrueba = ruta.copiar();
                    rutaPrueba.getPedidosAsignados().add(pos, pedido);
                    EvaluadorCostos.recalculareRuta(rutaPrueba, contexto.getMapa());

                    if (VerificadorRestricciones.esRutaFactible(rutaPrueba, contexto.getMapa())) {
                        double delta = rutaPrueba.getCostoTotal() - costoActual;
                        if (delta < menorIncrementoCosto) {
                            menorIncrementoCosto = delta;
                            mejorRuta = ruta;
                            mejorIndiceInsercion = pos;
                        }
                    }
                }
            }

            if (mejorRuta != null) {
                mejorRuta.getPedidosAsignados().add(mejorIndiceInsercion, pedido);
                EvaluadorCostos.recalculareRuta(mejorRuta, contexto.getMapa());
                iterador.remove();
            }
        }
    }
}
