package com.paqrap.simulador;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.PlanificadorRutas;
import com.paqrap.solucionador.SolucionadorALNS;
import com.paqrap.solucionador.ipso.SolucionadorIPSO;
import java.io.File;
import java.util.Locale;

/**
 * Módulo de Experimentación Numérica Comparativa exigido por los requisitos no funcionales (RNF-a y RNF-b):
 * "Presentar dos soluciones algorítmicas para el planificador de la solución en Lenguaje Java y evaluadas por experimentación numérica."
 * "Los dos algoritmos de la experimentación numérica deben ser del tipo metaheurísticos (ALNS vs IPSO)."
 */
public class ExperimentoComparativo {

    public static void main(String[] args) throws Exception {
        String rutaConfig = (args.length > 0) ? args[0] : "config/configuracion.json";
        ConfiguracionSistema config = ConfiguracionSistema.cargarDesdeArchivo(new File(rutaConfig));
        MapaCuadricula mapa = config.construirMapa();

        System.out.println("===============================================================================");
        System.out.println("   PAQRAP - BANCO DE EXPERIMENTACIÓN NUMÉRICA COMPARATIVA (RNF-a / RNF-b)      ");
        System.out.println("                   ALNS vs IPSO (Metaheurísticas en Java)                      ");
        System.out.println("===============================================================================\n");

        // 1. Instanciar los dos planificadores metaheurísticos
        PlanificadorRutas alns = new SolucionadorALNS(config.getAlns());
        PlanificadorRutas ipso = new SolucionadorIPSO();

        // ---------------------------------------------------------------------
        // EXPERIMENTO 1: COMPARATIVA DIRECTA EN PLANIFICACIÓN INICIAL
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("EXPERIMENTO 1: Comparativa Directa sobre el Caso Maestro (35 Pedidos / 16 Vehículos)");
        System.out.println("-------------------------------------------------------------------------------");

        ContextoProblema ctx1 = config.construirContextoInicial(mapa);
        long t1 = System.currentTimeMillis();
        Solucion solALNS = alns.resolver(ctx1);
        long tALNS = System.currentTimeMillis() - t1;

        ContextoProblema ctx2 = config.construirContextoInicial(mapa);
        long t2 = System.currentTimeMillis();
        Solucion solIPSO = ipso.resolver(ctx2);
        long tIPSO = System.currentTimeMillis() - t2;

        int vehiculosALNS = contarVehiculosConCarga(solALNS);
        int vehiculosIPSO = contarVehiculosConCarga(solIPSO);

        System.out.printf("%-20s | %-12s | %-14s | %-14s | %-12s | %-12s\n",
                "Algoritmo", "Tiempo (ms)", "Costo Total", "Distancia (km)", "Vehículos", "No Asignados");
        System.out.println("---------------------+--------------+----------------+----------------+--------------+-------------");
        System.out.printf(Locale.US, "%-20s | %10d ms | S/ %11.2f | %11.1f km | %10d de 16 | %10d\n",
                alns.getNombre(), tALNS, solALNS.calcularCostoTotal(), solALNS.calcularDistanciaTotal(), vehiculosALNS, solALNS.getPedidosNoAsignados().size());
        System.out.printf(Locale.US, "%-20s | %10d ms | S/ %11.2f | %11.1f km | %10d de 16 | %10d\n",
                ipso.getNombre(), tIPSO, solIPSO.calcularCostoTotal(), solIPSO.calcularDistanciaTotal(), vehiculosIPSO, solIPSO.getPedidosNoAsignados().size());
        System.out.println("-------------------------------------------------------------------------------\n");

        // ---------------------------------------------------------------------
        // EXPERIMENTO 2: SIMULACIÓN CONTINUA DE 5 DÍAS (15 TURNOS)
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("EXPERIMENTO 2: Simulación de Operaciones Continuas de 5 Días (15 Turnos de 8h)");
        System.out.println("-------------------------------------------------------------------------------");

        SimuladorCincoDias sim5D = new SimuladorCincoDias();
        System.out.println(">> Ejecutando Simulación 5D con ALNS...");
        SimuladorCincoDias.Resultado5Dias res5D_ALNS = sim5D.ejecutar(config, alns, 18);
        System.out.println(">> Ejecutando Simulación 5D con IPSO...");
        SimuladorCincoDias.Resultado5Dias res5D_IPSO = sim5D.ejecutar(config, ipso, 18);

        System.out.printf("\n%-20s | %-12s | %-12s | %-14s | %-14s | %-12s\n",
                "Algoritmo (5D)", "Tiempo (ms)", "Atendidos", "Costo 5 Días", "Distancia 5D", "% A Tiempo");
        System.out.println("---------------------+--------------+--------------+----------------+----------------+-------------");
        System.out.printf(Locale.US, "%-20s | %10d ms | %10d paq | S/ %11.2f | %11.1f km | %10.1f%%\n",
                res5D_ALNS.nombreAlgoritmo, res5D_ALNS.tiempoComputoMs, res5D_ALNS.totalPedidosAtendidos,
                res5D_ALNS.costoTotalSoles, res5D_ALNS.distanciaTotalKm, res5D_ALNS.porcentajeCumplimiento);
        System.out.printf(Locale.US, "%-20s | %10d ms | %10d paq | S/ %11.2f | %11.1f km | %10.1f%%\n",
                res5D_IPSO.nombreAlgoritmo, res5D_IPSO.tiempoComputoMs, res5D_IPSO.totalPedidosAtendidos,
                res5D_IPSO.costoTotalSoles, res5D_IPSO.distanciaTotalKm, res5D_IPSO.porcentajeCumplimiento);
        System.out.println("-------------------------------------------------------------------------------\n");

        // ---------------------------------------------------------------------
        // EXPERIMENTO 3: ESTRÉS PROGRESIVO HASTA EL COLAPSO LOGÍSTICO
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("EXPERIMENTO 3: Prueba de Estrés Progresivo hasta el Colapso Logístico");
        System.out.println("-------------------------------------------------------------------------------");

        SimuladorColapsoLogistico simColapso = new SimuladorColapsoLogistico();
        SimuladorColapsoLogistico.ReporteColapso repALNS = simColapso.ejecutarPruebaEstres(config, alns);

        System.out.printf("Capacidad Máxima Teórica de Flota: %d paquetes por turno\n", repALNS.capacidadMaximaFlotaPaquetes);
        System.out.printf("%-6s | %-10s | %-10s | %-10s | %-12s | %-10s | %-25s\n",
                "Nivel", "Pedidos", "Demanda", "Asignados", "No Asignados", "% Atención", "Estado Operativo");
        System.out.println("-------+------------+------------+------------+--------------+------------+--------------------------");
        for (SimuladorColapsoLogistico.MetricaEstrés m : repALNS.curvasEstres) {
            System.out.printf(Locale.US, "Niv %-2d | %10d | %7d paq | %10d | %10d   | %8.1f%%  | %s\n",
                    m.nivel, m.pedidosEmitidos, m.paquetesDemandados, m.pedidosAsignados, m.pedidosNoAsignados, m.porcentajeAtencion, m.estadoOperativo);
        }
        System.out.println("-------------------------------------------------------------------------------");
        if (repALNS.puntoQuiebrePedidos > 0) {
            System.out.printf(">> PUNTO DE QUIEBRE / COLAPSO LOGÍSTICO IDENTIFICADO: ~%d pedidos/turno\n", repALNS.puntoQuiebrePedidos);
            System.out.println("   (A partir de este volumen, la saturación del turno de 8h y el tiempo de atención de 1h/cliente desbordan la flota)\n");
        }
    }

    private static int contarVehiculosConCarga(Solucion sol) {
        int c = 0;
        for (Ruta r : sol.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) c++;
        }
        return c;
    }
}
