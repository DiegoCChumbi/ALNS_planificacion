package com.paqrap.simulador;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.PlanificadorRutas;
import com.paqrap.solucionador.SolucionadorALNS;
import java.io.File;
import java.util.Locale;

/**
 * Módulo de Evaluación y Experimentación de los Escenarios requeridos por la situación auténtica:
 * Escenario 1: Operaciones del caso maestro día a día (tiempo real).
 * Escenario 2: Simulación del periodo de 5 días continuos (15 turnos de 8 horas).
 * Escenario 3: Simulación hasta el colapso logístico (curvas de saturación y punto de quiebre).
 */
public class ExperimentoComparativo {

    public static void main(String[] args) throws Exception {
        String rutaConfig = (args.length > 0) ? args[0] : "config/configuracion.json";
        ConfiguracionSistema config = ConfiguracionSistema.cargarDesdeArchivo(new File(rutaConfig));
        MapaCuadricula mapa = config.construirMapa();

        System.out.println("===============================================================================");
        System.out.println("   PAQRAP - BANCO DE EXPERIMENTACIÓN NUMÉRICA Y EVALUACIÓN DE ESCENARIOS       ");
        System.out.println("           Algoritmo ALNS (Adaptive Large Neighborhood Search)                 ");
        System.out.println("===============================================================================\n");

        PlanificadorRutas alns = new SolucionadorALNS(config.getAlns());

        // ---------------------------------------------------------------------
        // ESCENARIO 1: PLANIFICACIÓN Y DESEMPEÑO EN EL CASO MAESTRO
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("ESCENARIO 1: Evaluación sobre Caso Maestro (35 Pedidos / 16 Vehículos)");
        System.out.println("-------------------------------------------------------------------------------");

        ContextoProblema ctx1 = config.construirContextoInicial(mapa);
        long t1 = System.currentTimeMillis();
        Solucion solALNS = alns.resolver(ctx1);
        long tALNS = System.currentTimeMillis() - t1;

        int vehiculosALNS = contarVehiculosConCarga(solALNS);

        System.out.printf("%-22s | %-12s | %-14s | %-14s | %-12s | %-12s\n",
                "Algoritmo", "Tiempo (ms)", "Costo Total", "Distancia (km)", "Vehículos", "No Asignados");
        System.out.println("-----------------------+--------------+----------------+----------------+--------------+-------------");
        System.out.printf(Locale.US, "%-22s | %10d ms | S/ %11.2f | %11.1f km | %10d de 16 | %10d\n",
                alns.getNombre(), tALNS, solALNS.calcularCostoTotal(), solALNS.calcularDistanciaTotal(), vehiculosALNS, solALNS.getPedidosNoAsignados().size());
        System.out.println("-------------------------------------------------------------------------------\n");

        // ---------------------------------------------------------------------
        // ESCENARIO 2: SIMULACIÓN CONTINUA DE 5 DÍAS (15 TURNOS)
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("ESCENARIO 2: Simulación de Operaciones Continuas de 5 Días (15 Turnos de 8h)");
        System.out.println("-------------------------------------------------------------------------------");

        SimuladorCincoDias sim5D = new SimuladorCincoDias();
        System.out.println(">> Ejecutando Simulación Multidiaria 5D con ALNS...");
        SimuladorCincoDias.Resultado5Dias res5D_ALNS = sim5D.ejecutar(config, alns, 18);

        System.out.printf("\n%-22s | %-12s | %-12s | %-14s | %-14s | %-12s\n",
                "Métrica 5D (ALNS)", "Tiempo (ms)", "Atendidos", "Costo 5 Días", "Distancia 5D", "% A Tiempo");
        System.out.println("-----------------------+--------------+--------------+----------------+----------------+-------------");
        System.out.printf(Locale.US, "%-22s | %10d ms | %10d paq | S/ %11.2f | %11.1f km | %10.1f%%\n",
                res5D_ALNS.nombreAlgoritmo, res5D_ALNS.tiempoComputoMs, res5D_ALNS.totalPedidosAtendidos,
                res5D_ALNS.costoTotalSoles, res5D_ALNS.distanciaTotalKm, res5D_ALNS.porcentajeCumplimiento);
        System.out.println("-------------------------------------------------------------------------------\n");

        // ---------------------------------------------------------------------
        // ESCENARIO 3: ESTRÉS PROGRESIVO HASTA EL COLAPSO LOGÍSTICO
        // ---------------------------------------------------------------------
        System.out.println("-------------------------------------------------------------------------------");
        System.out.println("ESCENARIO 3: Prueba de Estrés Progresivo hasta el Colapso Logístico");
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
