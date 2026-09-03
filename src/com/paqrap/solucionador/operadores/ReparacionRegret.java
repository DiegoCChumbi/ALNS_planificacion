package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import java.util.*;

public class ReparacionRegret implements OperadorReparacion {

    private final int kRegret;

    public ReparacionRegret(int kRegret) {
        this.kRegret = kRegret;
    }

    @Override
    public String getNombre() {
        return "Inserción Regret-" + kRegret;
    }

    private static class OpcionInsercion {
        Ruta ruta;
        int posicion;
        double deltaCosto;

        OpcionInsercion(Ruta ruta, int posicion, double deltaCosto) {
            this.ruta = ruta;
            this.posicion = posicion;
            this.deltaCosto = deltaCosto;
        }
    }

    @Override
    public void reparar(Solucion solucion, List<Pedido> pedidosNoAsignados, ContextoProblema contexto) {
        while (!pedidosNoAsignados.isEmpty()) {
            Pedido pedidoMaxRegret = null;
            Ruta rutaMaxRegret = null;
            int posMaxRegret = -1;
            double valorMaxRegret = -Double.MAX_VALUE;

            for (Pedido pedido : pedidosNoAsignados) {
                List<OpcionInsercion> opciones = new ArrayList<>();

                for (Ruta ruta : solucion.getRutas()) {
                    double costoActual = ruta.getCostoTotal();

                    for (int pos = 0; pos <= ruta.getPedidosAsignados().size(); pos++) {
                        Ruta rutaPrueba = ruta.copiar();
                        rutaPrueba.getPedidosAsignados().add(pos, pedido);
                        EvaluadorCostos.recalculareRuta(rutaPrueba, contexto.getMapa());

                        if (VerificadorRestricciones.esRutaFactible(rutaPrueba, contexto.getMapa())) {
                            double delta = rutaPrueba.getCostoTotal() - costoActual;
                            opciones.add(new OpcionInsercion(ruta, pos, delta));
                        }
                    }
                }

                if (opciones.isEmpty()) continue;

                opciones.sort(Comparator.comparingDouble(o -> o.deltaCosto));

                double regret = 0.0;
                if (opciones.size() == 1) {
                    regret = opciones.get(0).deltaCosto;
                } else {
                    int k = Math.min(kRegret - 1, opciones.size() - 1);
                    regret = opciones.get(k).deltaCosto - opciones.get(0).deltaCosto;
                }

                if (regret > valorMaxRegret) {
                    valorMaxRegret = regret;
                    pedidoMaxRegret = pedido;
                    rutaMaxRegret = opciones.get(0).ruta;
                    posMaxRegret = opciones.get(0).posicion;
                }
            }

            if (pedidoMaxRegret != null && rutaMaxRegret != null) {
                rutaMaxRegret.getPedidosAsignados().add(posMaxRegret, pedidoMaxRegret);
                EvaluadorCostos.recalculareRuta(rutaMaxRegret, contexto.getMapa());
                pedidosNoAsignados.remove(pedidoMaxRegret);
            } else {
                break;
            }
        }
    }
}
