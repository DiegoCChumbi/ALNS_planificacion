package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import java.util.*;

/**
 * Operador de Reparación: Inserción Regret con Factor de Ruido (Noise Regret).
 * Referencia: Ropke & Pisinger (2006b), Friedrich & Elbert (2022, Sección 4.3.3).
 * Incorpora un término de perturbación estocástica aleatoria al evaluar los costos
 * de inserción: ΔC' = max(0, ΔC * (1 + ξ)), con ξ ∈ [-factorRuido, +factorRuido].
 * Crucial para romper empates y simetrías en la cuadrícula de Manhattan de PaqRap.
 */
public class ReparacionRegretRuido implements OperadorReparacion {

    private final int kRegret;
    private final double factorRuidoMax;
    private final Random random;

    public ReparacionRegretRuido(int kRegret, double factorRuidoMax) {
        this.kRegret = kRegret;
        this.factorRuidoMax = factorRuidoMax;
        this.random = new Random();
    }

    public ReparacionRegretRuido(int kRegret) {
        this(kRegret, 0.20);
    }

    @Override
    public String getNombre() {
        return String.format("Inserción Regret-%d con Ruido (%.0f%%)", kRegret, factorRuidoMax * 100);
    }

    private static class OpcionInsercion {
        Ruta ruta;
        int posicion;
        double deltaCosto;
        double deltaCostoConRuido;

        OpcionInsercion(Ruta ruta, int posicion, double deltaCosto, double deltaCostoConRuido) {
            this.ruta = ruta;
            this.posicion = posicion;
            this.deltaCosto = deltaCosto;
            this.deltaCostoConRuido = deltaCostoConRuido;
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
                        EvaluadorCostos.recalculareRuta(rutaPrueba, contexto.getMapa(), contexto.getConfiguracionOperacion());

                        if (VerificadorRestricciones.esRutaFactible(rutaPrueba, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                            double delta = rutaPrueba.getCostoTotal() - costoActual;
                            double xi = (random.nextDouble() * 2.0 - 1.0) * factorRuidoMax;
                            double deltaConRuido = Math.max(0.0, delta * (1.0 + xi));
                            opciones.add(new OpcionInsercion(ruta, pos, delta, deltaConRuido));
                        }
                    }
                }

                if (opciones.isEmpty()) continue;

                // Ordenar según el costo perturbado con ruido
                opciones.sort(Comparator.comparingDouble(o -> o.deltaCostoConRuido));

                double regret = 0.0;
                if (opciones.size() == 1) {
                    regret = opciones.get(0).deltaCostoConRuido;
                } else {
                    int k = Math.min(kRegret - 1, opciones.size() - 1);
                    regret = opciones.get(k).deltaCostoConRuido - opciones.get(0).deltaCostoConRuido;
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
                EvaluadorCostos.recalculareRuta(rutaMaxRegret, contexto.getMapa(), contexto.getConfiguracionOperacion());
                pedidosNoAsignados.remove(pedidoMaxRegret);
            } else {
                break;
            }
        }
    }
}
