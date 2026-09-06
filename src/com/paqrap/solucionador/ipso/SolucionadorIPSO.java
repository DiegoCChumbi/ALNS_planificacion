package com.paqrap.solucionador.ipso;

import com.paqrap.evaluador.CalculadorDistancia;
import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.PlanificadorRutas;
import java.util.*;

/**
 * Implementación del algoritmo IPSO (Improved Particle Swarm Optimization) para VRP.
 * Especificado en el informe académico de selección de algoritmos (Equipo 6F, 2026).
 * Modela el enjambre con permutaciones discretas, operadores de intercambio (Swap Sequences),
 * Cruce de Orden (Order Crossover - OX) y mutación heurística guiada por distancia.
 */
public class SolucionadorIPSO implements PlanificadorRutas {

    private final ConfiguracionIPSO configuracion;
    private final Random random;

    public SolucionadorIPSO(ConfiguracionIPSO configuracion) {
        this.configuracion = configuracion;
        this.random = new Random();
    }

    public SolucionadorIPSO() {
        this(new ConfiguracionIPSO());
    }

    @Override
    public String getNombre() {
        return "IPSO (Improved Particle Swarm Optimization)";
    }

    @Override
    public Solucion resolver(ContextoProblema contexto) {
        List<Pedido> pedidos = new ArrayList<>(contexto.getPedidosPendientes());
        if (pedidos.isEmpty()) {
            Solucion solVacia = new Solucion();
            for (EstadoVehiculo ev : contexto.getVehiculosActivos()) {
                solVacia.getRutas().add(new Ruta(ev));
            }
            return solVacia;
        }

        int tamanoEnjambre = configuracion.getTamanoEnjambre();
        int maxIter = configuracion.getMaxIteraciones();

        // 1. Inicializar enjambre con permutaciones
        List<ParticulaIPSO> enjambre = new ArrayList<>(tamanoEnjambre);
        ParticulaIPSO globalBest = null;
        double globalBestFitness = -Double.MAX_VALUE;
        Solucion globalBestSolucion = null;

        for (int i = 0; i < tamanoEnjambre; i++) {
            List<Pedido> perm = new ArrayList<>(pedidos);
            Collections.shuffle(perm, random);

            ParticulaIPSO p = new ParticulaIPSO(perm);
            Solucion sol = decodificarEnSolucion(perm, contexto);
            double fit = calcularFitness(sol, contexto);

            p.setMejorFitness(fit);
            p.setMejorSolucion(sol);

            if (fit > globalBestFitness) {
                globalBestFitness = fit;
                globalBest = p;
                globalBestSolucion = sol.copiar();
            }

            enjambre.add(p);
        }

        // 2. Bucle iterativo del enjambre
        for (int iter = 1; iter <= maxIter; iter++) {
            double sumaFitness = 0.0;

            for (ParticulaIPSO p : enjambre) {
                List<Pedido> x = new ArrayList<>(p.getPosicion());

                // Operadores de diferencia (P_best - X) y (G_best - X)
                List<OperadorSwap> diffPBest = calcularDiferenciaSwap(p.getMejorPosicion(), x);
                List<OperadorSwap> diffGBest = calcularDiferenciaSwap(globalBest.getMejorPosicion(), x);

                // Actualizar velocidad con componentes cognitiva y social
                List<OperadorSwap> nuevaVel = new ArrayList<>();
                for (OperadorSwap s : diffPBest) {
                    if (random.nextDouble() < (configuracion.getC1Cognitivo() / 2.0)) {
                        nuevaVel.add(s);
                    }
                }
                for (OperadorSwap s : diffGBest) {
                    if (random.nextDouble() < (configuracion.getC2Social() / 2.0)) {
                        nuevaVel.add(s);
                    }
                }
                p.setVelocidad(nuevaVel);

                // Actualizar posición aplicando swaps
                for (OperadorSwap s : nuevaVel) {
                    s.aplicar(x);
                }

                // Cruce de Orden (OX) con el mejor global G_best
                if (random.nextDouble() < configuracion.getProbabilidadCruceOX() && x.size() > 2) {
                    x = aplicarOrderCrossover(x, globalBest.getMejorPosicion());
                }

                // Mutación heurística guiada por proximidad geográfica
                if (random.nextDouble() < configuracion.getProbabilidadMutacionHeuristica() && x.size() > 1) {
                    aplicarMutacionHeuristicaDistancia(x, contexto);
                }

                p.setPosicion(x);

                // Evaluar aptitud (Fitness)
                Solucion solActual = decodificarEnSolucion(x, contexto);
                double fitActual = calcularFitness(solActual, contexto);
                sumaFitness += fitActual;

                if (fitActual > p.getMejorFitness()) {
                    p.setMejorFitness(fitActual);
                    p.setMejorPosicion(x);
                    p.setMejorSolucion(solActual);

                    if (fitActual > globalBestFitness) {
                        globalBestFitness = fitActual;
                        globalBest = p;
                        globalBestSolucion = solActual.copiar();
                    }
                }
            }

            // Detección de estancamiento de enjambre (diversificación de rescate)
            double mediaFitness = sumaFitness / tamanoEnjambre;
            double varianza = 0.0;
            for (ParticulaIPSO p : enjambre) {
                varianza += Math.pow(p.getMejorFitness() - mediaFitness, 2);
            }
            varianza /= tamanoEnjambre;

            if (varianza < configuracion.getUmbralEstancamiento() && iter < maxIter - 10) {
                // Mutación forzada a la mitad del enjambre para escapar del óptimo local
                for (int k = 1; k < tamanoEnjambre / 2; k++) {
                    Collections.shuffle(enjambre.get(k).getPosicion(), random);
                }
            }
        }

        return globalBestSolucion != null ? globalBestSolucion : decodificarEnSolucion(pedidos, contexto);
    }

    /**
     * Decodifica una permutación de pedidos en rutas factibles multi-vehículo.
     * Respeta capacidades, refrigerios obligatorios y retorno a base.
     */
    public Solucion decodificarEnSolucion(List<Pedido> permutacion, ContextoProblema contexto) {
        Solucion sol = new Solucion();
        for (EstadoVehiculo ev : contexto.getVehiculosActivos()) {
            Ruta r = new Ruta(ev);
            EvaluadorCostos.recalculareRuta(r, contexto.getMapa(), contexto.getConfiguracionOperacion());
            sol.getRutas().add(r);
        }

        for (Pedido pedido : permutacion) {
            Ruta mejorRuta = null;
            int mejorPos = -1;
            double menorDelta = Double.MAX_VALUE;

            for (Ruta ruta : sol.getRutas()) {
                double costoBase = ruta.getCostoTotal();
                for (int pos = 0; pos <= ruta.getPedidosAsignados().size(); pos++) {
                    Ruta prueba = ruta.copiar();
                    prueba.getPedidosAsignados().add(pos, pedido);
                    EvaluadorCostos.recalculareRuta(prueba, contexto.getMapa(), contexto.getConfiguracionOperacion());

                    if (VerificadorRestricciones.esRutaFactible(prueba, contexto.getMapa(), contexto.getConfiguracionOperacion())) {
                        double delta = prueba.getCostoTotal() - costoBase;
                        if (delta < menorDelta) {
                            menorDelta = delta;
                            mejorRuta = ruta;
                            mejorPos = pos;
                        }
                    }
                }
            }

            if (mejorRuta != null) {
                mejorRuta.getPedidosAsignados().add(mejorPos, pedido);
                EvaluadorCostos.recalculareRuta(mejorRuta, contexto.getMapa(), contexto.getConfiguracionOperacion());
            } else {
                sol.getPedidosNoAsignados().add(pedido);
            }
        }

        return sol;
    }

    private double calcularFitness(Solucion sol, ContextoProblema contexto) {
        double penalizacion = contexto.getConfiguracionOperacion().getPenalizacionPedidoNoAsignado();
        double costo = sol.calcularCostoTotal(penalizacion);
        return 1.0 / (1.0 + costo);
    }

    /**
     * Calcula la secuencia mínima de Swaps que transforma la permutación origen en destino.
     */
    private List<OperadorSwap> calcularDiferenciaSwap(List<Pedido> destino, List<Pedido> origen) {
        List<OperadorSwap> swaps = new ArrayList<>();
        List<Pedido> temp = new ArrayList<>(origen);

        for (int i = 0; i < destino.size(); i++) {
            Pedido deseado = destino.get(i);
            if (!temp.get(i).equals(deseado)) {
                int targetIdx = temp.indexOf(deseado);
                if (targetIdx != -1) {
                    swaps.add(new OperadorSwap(i, targetIdx));
                    Collections.swap(temp, i, targetIdx);
                }
            }
        }
        return swaps;
    }

    /**
     * Cruce de Orden (Order Crossover - OX) para preservar subsecuencias de clientes.
     */
    private List<Pedido> aplicarOrderCrossover(List<Pedido> padre1, List<Pedido> padre2) {
        int n = padre1.size();
        int cut1 = random.nextInt(n);
        int cut2 = random.nextInt(n);
        if (cut1 > cut2) {
            int tmp = cut1;
            cut1 = cut2;
            cut2 = tmp;
        }

        Pedido[] hijo = new Pedido[n];
        Set<Pedido> incluidos = new HashSet<>();

        // Copiar segmento de corte de padre1
        for (int i = cut1; i <= cut2; i++) {
            hijo[i] = padre1.get(i);
            incluidos.add(padre1.get(i));
        }

        // Rellenar con orden de padre2
        int idxHijo = (cut2 + 1) % n;
        for (int i = 0; i < n; i++) {
            int idxPadre2 = (cut2 + 1 + i) % n;
            Pedido candidato = padre2.get(idxPadre2);
            if (!incluidos.contains(candidato)) {
                hijo[idxHijo] = candidato;
                idxHijo = (idxHijo + 1) % n;
            }
        }

        return new ArrayList<>(Arrays.asList(hijo));
    }

    /**
     * Mutación heurística guiada por distancia: detecta dos clientes próximos y los agrupa contiguos.
     */
    private void aplicarMutacionHeuristicaDistancia(List<Pedido> perm, ContextoProblema contexto) {
        int i = random.nextInt(perm.size());
        Pedido ref = perm.get(i);
        int mejorJ = -1;
        double menorDist = Double.MAX_VALUE;

        for (int j = 0; j < perm.size(); j++) {
            if (i == j) continue;
            double d = CalculadorDistancia.getDistancia(contexto.getMapa(), ref.getDestino(), perm.get(j).getDestino());
            if (d < menorDist) {
                menorDist = d;
                mejorJ = j;
            }
        }

        if (mejorJ != -1) {
            // Mover el vecino más cercano contiguo a la posición i
            Pedido vecino = perm.remove(mejorJ);
            int nuevaPos = (i < perm.size()) ? i : perm.size();
            perm.add(nuevaPos, vecino);
        }
    }
}
