package com.paqrap.simulador;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.PlanificadorRutas;
import java.util.*;

/**
 * Motor de Simulación para el Escenario 3: Estrés hasta el Colapso Logístico.
 * Evalúa la resiliencia operativa de PaqRap aumentando progresivamente la tasa de demanda
 * hasta saturar la capacidad de transporte y el tiempo disponible por turno (8h).
 * Determina con precisión matemática el punto de quiebre logístico de la empresa.
 */
public class SimuladorColapsoLogistico {

    public static class MetricaEstrés {
        public int nivel;
        public int pedidosEmitidos;
        public int paquetesDemandados;
        public int paquetesCapacidadFlota;
        public int pedidosAsignados;
        public int pedidosNoAsignados;
        public double costoTotal;
        public double porcentajeAtencion;
        public String estadoOperativo; // "ESTABLE", "ALERTA", "COLAPSO_LOGISTICO"
        public String causaCuelloBotella;
    }

    public static class ReporteColapso {
        public String nombreAlgoritmo;
        public int puntoQuiebrePedidos;
        public int capacidadMaximaFlotaPaquetes;
        public List<MetricaEstrés> curvasEstres = new ArrayList<>();
    }

    public ReporteColapso ejecutarPruebaEstres(ConfiguracionSistema config, PlanificadorRutas planificador) {
        ReporteColapso reporte = new ReporteColapso();
        reporte.nombreAlgoritmo = planificador.getNombre();

        MapaCuadricula mapa = config.construirMapa();
        Random rand = new Random(100);

        // Capacidad física total de la flota
        int capTotalFlota = 0;
        for (EstadoVehiculo ev : config.getFlota()) {
            capTotalFlota += ev.getTipo().getCapacidad();
        }
        reporte.capacidadMaximaFlotaPaquetes = capTotalFlota;

        // Niveles de estrés progresivo
        int[] nivelesPedidos = { 15, 25, 35, 45, 60, 80, 110, 150 };

        for (int i = 0; i < nivelesPedidos.length; i++) {
            int numPedidos = nivelesPedidos[i];
            MetricaEstrés m = new MetricaEstrés();
            m.nivel = i + 1;
            m.pedidosEmitidos = numPedidos;
            m.paquetesCapacidadFlota = capTotalFlota;

            ContextoProblema ctx = new ContextoProblema(0.0, mapa, config.getOperacion());
            ctx.agregarAlmacen(config.getAlmacenCentral());
            for (NodoCuadricula ai : config.getAlmacenesIntermedios()) {
                ctx.agregarAlmacen(ai);
            }
            for (EstadoVehiculo ev : config.getFlota()) {
                ctx.agregarVehiculo(ev.copiar());
            }

            int paquetesDemandados = 0;
            double[] opcionesPlazo = { 4.0, 8.0, 12.0, 18.0, 36.0 };
            for (int p = 1; p <= numPedidos; p++) {
                int x = rand.nextInt(config.getEntorno().getAnchoMapa() + 1);
                int y = rand.nextInt(config.getEntorno().getAltoMapa() + 1);
                int cant = 1 + rand.nextInt(4); // 1 a 4 paquetes
                double plazo = opcionesPlazo[rand.nextInt(opcionesPlazo.length)];
                Pedido ped = new Pedido("STRESS-" + p, new NodoCuadricula("C-" + p, x, y, TipoNodo.CLIENTE), cant, 0.0, plazo);
                ctx.agregarPedido(ped);
                paquetesDemandados += cant;
            }
            m.paquetesDemandados = paquetesDemandados;

            // Planificar
            Solucion sol = planificador.resolver(ctx);
            m.pedidosNoAsignados = sol.getPedidosNoAsignados().size();
            m.pedidosAsignados = numPedidos - m.pedidosNoAsignados;
            m.costoTotal = sol.calcularCostoTotal();
            m.porcentajeAtencion = (numPedidos > 0) ? (m.pedidosAsignados * 100.0 / numPedidos) : 100.0;

            if (m.porcentajeAtencion >= 98.0) {
                m.estadoOperativo = "OPERACIÓN FLUIDA";
                m.causaCuelloBotella = "Ninguno. Flota con holgura suficiente.";
            } else if (m.porcentajeAtencion >= 80.0) {
                m.estadoOperativo = "CONGESTIÓN / ALERTA";
                m.causaCuelloBotella = "Tiempo de atención al cliente (1h) satura jornadas de 8h.";
            } else {
                m.estadoOperativo = "COLAPSO LOGÍSTICO";
                if (paquetesDemandados > capTotalFlota) {
                    m.causaCuelloBotella = String.format("Demanda (%d paq) sobrepasó la capacidad física máxima de la flota (%d paq).",
                            paquetesDemandados, capTotalFlota);
                } else {
                    m.causaCuelloBotella = "Límite temporal: No hay suficientes horas-vehículo para atender tantas paradas de 1h antes del fin de turno.";
                }

                if (reporte.puntoQuiebrePedidos == 0) {
                    reporte.puntoQuiebrePedidos = numPedidos;
                }
            }

            reporte.curvasEstres.add(m);
        }

        return reporte;
    }
}
