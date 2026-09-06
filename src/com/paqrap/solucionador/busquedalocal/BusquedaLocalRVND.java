package com.paqrap.solucionador.busquedalocal;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import java.util.*;

public class BusquedaLocalRVND {
    private final double epsilonMejora;

    public BusquedaLocalRVND(double epsilonMejora) {
        this.epsilonMejora = epsilonMejora;
    }

    public BusquedaLocalRVND() {
        this(0.01);
    }

    public Solucion aplicar(Solucion solucionActual, ContextoProblema contexto) {
        Solucion solucion = solucionActual.copiar();
        // Vecindarios: 1: Reubicar (Shift 1,0), 2: Intercambiar (Swap 1,1), 3: 2-Opt Intra, 4: Swap (2,1), 5: 2-Opt* Inter-ruta
        List<Integer> vecindarios = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collections.shuffle(vecindarios);

        while (!vecindarios.isEmpty()) {
            int vecindario = vecindarios.remove(0);
            boolean mejoRando = false;

            switch (vecindario) {
                case 1:
                    mejoRando = aplicarReubicacion(solucion, contexto);
                    break;
                case 2:
                    mejoRando = aplicarIntercambio(solucion, contexto);
                    break;
                case 3:
                    mejoRando = aplicarDosOpt(solucion, contexto);
                    break;
                case 4:
                    mejoRando = aplicarIntercambioDosUno(solucion, contexto);
                    break;
                case 5:
                    mejoRando = aplicarDosOptInterRuta(solucion, contexto);
                    break;
            }

            if (mejoRando) {
                vecindarios = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
                Collections.shuffle(vecindarios);
            }
        }

        return solucion;
    }

    private boolean aplicarReubicacion(Solucion solucion, ContextoProblema contexto) {
        for (Ruta r1 : solucion.getRutas()) {
            for (int i = 0; i < r1.getPedidosAsignados().size(); i++) {
                Pedido pedido = r1.getPedidosAsignados().get(i);

                for (Ruta r2 : solucion.getRutas()) {
                    int maxPos = (r1 == r2) ? r2.getPedidosAsignados().size() - 1 : r2.getPedidosAsignados().size();

                    for (int j = 0; j <= maxPos; j++) {
                        if (r1 == r2 && (i == j || i == j - 1)) continue;

                        Solucion solPrueba = solucion.copiar();
                        Ruta testR1 = obtenerRutaPorIdVehiculo(solPrueba, r1.getVehiculo().getId());
                        Ruta testR2 = obtenerRutaPorIdVehiculo(solPrueba, r2.getVehiculo().getId());

                        testR1.getPedidosAsignados().remove(i);
                        if (r1 == r2 && j > i) {
                            testR2.getPedidosAsignados().add(j - 1, pedido);
                        } else {
                            testR2.getPedidosAsignados().add(j, pedido);
                        }

                        EvaluadorCostos.recalculareRuta(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion());
                        if (r1 != r2) EvaluadorCostos.recalculareRuta(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion());

                        if (VerificadorRestricciones.esRutaFactible(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion()) &&
                            VerificadorRestricciones.esRutaFactible(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                            if (solPrueba.calcularCostoTotal() < solucion.calcularCostoTotal() - epsilonMejora) {
                                solucion.getRutas().clear();
                                solucion.getRutas().addAll(solPrueba.getRutas());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean aplicarIntercambio(Solucion solucion, ContextoProblema contexto) {
        for (int r1Idx = 0; r1Idx < solucion.getRutas().size(); r1Idx++) {
            Ruta r1 = solucion.getRutas().get(r1Idx);

            for (int r2Idx = r1Idx; r2Idx < solucion.getRutas().size(); r2Idx++) {
                Ruta r2 = solucion.getRutas().get(r2Idx);

                for (int i = 0; i < r1.getPedidosAsignados().size(); i++) {
                    int startJ = (r1 == r2) ? i + 1 : 0;

                    for (int j = startJ; j < r2.getPedidosAsignados().size(); j++) {
                        Solucion solPrueba = solucion.copiar();
                        Ruta testR1 = solPrueba.getRutas().get(r1Idx);
                        Ruta testR2 = solPrueba.getRutas().get(r2Idx);

                        Pedido p1 = testR1.getPedidosAsignados().get(i);
                        Pedido p2 = testR2.getPedidosAsignados().get(j);

                        testR1.getPedidosAsignados().set(i, p2);
                        testR2.getPedidosAsignados().set(j, p1);

                        EvaluadorCostos.recalculareRuta(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion());
                        if (r1 != r2) EvaluadorCostos.recalculareRuta(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion());

                        if (VerificadorRestricciones.esRutaFactible(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion()) &&
                            VerificadorRestricciones.esRutaFactible(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                            if (solPrueba.calcularCostoTotal() < solucion.calcularCostoTotal() - epsilonMejora) {
                                solucion.getRutas().clear();
                                solucion.getRutas().addAll(solPrueba.getRutas());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean aplicarDosOpt(Solucion solucion, ContextoProblema contexto) {
        for (Ruta r : solucion.getRutas()) {
            List<Pedido> pedidos = r.getPedidosAsignados();
            if (pedidos.size() < 3) continue;

            for (int i = 0; i < pedidos.size() - 1; i++) {
                for (int j = i + 1; j < pedidos.size(); j++) {
                    Solucion solPrueba = solucion.copiar();
                    Ruta testR = obtenerRutaPorIdVehiculo(solPrueba, r.getVehiculo().getId());

                    Collections.reverse(testR.getPedidosAsignados().subList(i, j + 1));
                    EvaluadorCostos.recalculareRuta(testR, contexto.getMapa(), contexto.getConfiguracionOperacion());

                    if (VerificadorRestricciones.esRutaFactible(testR, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                        if (solPrueba.calcularCostoTotal() < solucion.calcularCostoTotal() - epsilonMejora) {
                            solucion.getRutas().clear();
                            solucion.getRutas().addAll(solPrueba.getRutas());
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean aplicarIntercambioDosUno(Solucion solucion, ContextoProblema contexto) {
        for (int r1Idx = 0; r1Idx < solucion.getRutas().size(); r1Idx++) {
            Ruta r1 = solucion.getRutas().get(r1Idx);
            if (r1.getPedidosAsignados().size() < 2) continue;

            for (int r2Idx = 0; r2Idx < solucion.getRutas().size(); r2Idx++) {
                if (r1Idx == r2Idx) continue;
                Ruta r2 = solucion.getRutas().get(r2Idx);
                if (r2.getPedidosAsignados().isEmpty()) continue;

                for (int i = 0; i < r1.getPedidosAsignados().size() - 1; i++) {
                    for (int j = 0; j < r2.getPedidosAsignados().size(); j++) {
                        Solucion solPrueba = solucion.copiar();
                        Ruta testR1 = solPrueba.getRutas().get(r1Idx);
                        Ruta testR2 = solPrueba.getRutas().get(r2Idx);

                        Pedido p1a = testR1.getPedidosAsignados().get(i);
                        Pedido p1b = testR1.getPedidosAsignados().get(i + 1);
                        Pedido p2 = testR2.getPedidosAsignados().get(j);

                        // Remover de r1 e insertar p2
                        testR1.getPedidosAsignados().remove(i + 1);
                        testR1.getPedidosAsignados().set(i, p2);

                        // Remover p2 de r2 e insertar p1a, p1b
                        testR2.getPedidosAsignados().remove(j);
                        testR2.getPedidosAsignados().add(j, p1b);
                        testR2.getPedidosAsignados().add(j, p1a);

                        EvaluadorCostos.recalculareRuta(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion());
                        EvaluadorCostos.recalculareRuta(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion());

                        if (VerificadorRestricciones.esRutaFactible(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion()) &&
                            VerificadorRestricciones.esRutaFactible(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                            if (solPrueba.calcularCostoTotal() < solucion.calcularCostoTotal() - epsilonMejora) {
                                solucion.getRutas().clear();
                                solucion.getRutas().addAll(solPrueba.getRutas());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean aplicarDosOptInterRuta(Solucion solucion, ContextoProblema contexto) {
        for (int r1Idx = 0; r1Idx < solucion.getRutas().size(); r1Idx++) {
            Ruta r1 = solucion.getRutas().get(r1Idx);
            if (r1.getPedidosAsignados().isEmpty()) continue;

            for (int r2Idx = r1Idx + 1; r2Idx < solucion.getRutas().size(); r2Idx++) {
                Ruta r2 = solucion.getRutas().get(r2Idx);
                if (r2.getPedidosAsignados().isEmpty()) continue;

                int size1 = r1.getPedidosAsignados().size();
                int size2 = r2.getPedidosAsignados().size();

                for (int i = 0; i < size1; i++) {
                    for (int j = 0; j < size2; j++) {
                        Solucion solPrueba = solucion.copiar();
                        Ruta testR1 = solPrueba.getRutas().get(r1Idx);
                        Ruta testR2 = solPrueba.getRutas().get(r2Idx);

                        List<Pedido> cola1 = new ArrayList<>(testR1.getPedidosAsignados().subList(i + 1, size1));
                        List<Pedido> cola2 = new ArrayList<>(testR2.getPedidosAsignados().subList(j + 1, size2));

                        testR1.getPedidosAsignados().subList(i + 1, size1).clear();
                        testR2.getPedidosAsignados().subList(j + 1, size2).clear();

                        testR1.getPedidosAsignados().addAll(cola2);
                        testR2.getPedidosAsignados().addAll(cola1);

                        EvaluadorCostos.recalculareRuta(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion());
                        EvaluadorCostos.recalculareRuta(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion());

                        if (VerificadorRestricciones.esRutaFactible(testR1, contexto.getMapa(), contexto.getConfiguracionOperacion()) &&
                            VerificadorRestricciones.esRutaFactible(testR2, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                            if (solPrueba.calcularCostoTotal() < solucion.calcularCostoTotal() - epsilonMejora) {
                                solucion.getRutas().clear();
                                solucion.getRutas().addAll(solPrueba.getRutas());
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private Ruta obtenerRutaPorIdVehiculo(Solucion sol, String idVehiculo) {
        for (Ruta r : sol.getRutas()) {
            if (r.getVehiculo().getId().equals(idVehiculo)) return r;
        }
        return null;
    }
}
