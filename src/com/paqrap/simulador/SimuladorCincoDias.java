package com.paqrap.simulador;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.evaluador.VerificadorRestricciones;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.PlanificadorRutas;
import java.util.*;

/**
 * Motor de Simulación para el Escenario 2: Operaciones continuas de 5 días (5d).
 * Simula 15 turnos de trabajo consecutivos (8h por turno, 3 turnos diarios) con:
 * - Demanda distribuida en 5 categorías de SLA (4h, 8h, 12h, 18h y 36h).
 * - Recarga instantánea diaria de almacenes intermedios (cada 24h a las 23:59:59).
 * - Seguimiento de rotación de flota, descanso de conductores y auditoría de plazos.
 */
public class SimuladorCincoDias {

    public static class MetricasDia {
        public int dia;
        public int pedidosRecibidos = 0;
        public int pedidosEntregados = 0;
        public int pedidosATiempo = 0;
        public int pedidosTardios = 0;
        public double distanciaTotalKm = 0.0;
        public double costoTotalSoles = 0.0;
        public int stockConsumidoAlm1 = 0;
        public int stockConsumidoAlm2 = 0;
    }

    public static class Resultado5Dias {
        public String nombreAlgoritmo;
        public long tiempoComputoMs;
        public int totalPedidosAtendidos = 0;
        public int totalPedidosATiempo = 0;
        public int totalPedidosTardios = 0;
        public double distanciaTotalKm = 0.0;
        public double costoTotalSoles = 0.0;
        public double porcentajeCumplimiento = 0.0;
        public List<MetricasDia> detallePorDia = new ArrayList<>();
    }

    public Resultado5Dias ejecutar(ConfiguracionSistema config, PlanificadorRutas planificador, int pedidosPorTurnoPromedio) {
        long tInicio = System.currentTimeMillis();
        Resultado5Dias resultado = new Resultado5Dias();
        resultado.nombreAlgoritmo = planificador.getNombre();

        MapaCuadricula mapa = config.construirMapa();
        Random rand = new Random(42); // Semilla reproducible

        // Almacenes con inventario
        Almacen almCentral = (Almacen) config.getAlmacenCentral();
        Almacen alm1 = (Almacen) config.getAlmacenesIntermedios().get(0);
        Almacen alm2 = (Almacen) config.getAlmacenesIntermedios().get(1);

        double[] opcionesSLA = { 4.0, 8.0, 12.0, 18.0, 36.0 };
        double horasTotales = 120.0; // 5 días x 24 horas
        int totalTurnos = 15;
        double duracionTurno = 8.0;

        List<Pedido> colaGlobalPedidos = new ArrayList<>();
        int contadorPedidos = 1;

        for (int d = 1; d <= 5; d++) {
            MetricasDia metDia = new MetricasDia();
            metDia.dia = d;

            for (int t = 1; t <= 3; t++) {
                int turnoGlobal = (d - 1) * 3 + t;
                double tiempoInicioTurnoRelativo = (turnoGlobal - 1) * duracionTurno;
                double horaReloj = (7.0 + (t - 1) * 8.0) % 24.0;

                // Generar nuevos pedidos de este turno
                int pedidosEsteTurno = pedidosPorTurnoPromedio - 3 + rand.nextInt(7);
                for (int p = 0; p < pedidosEsteTurno; p++) {
                    int x = rand.nextInt(config.getEntorno().getAnchoMapa() + 1);
                    int y = rand.nextInt(config.getEntorno().getAltoMapa() + 1);
                    int cant = 1 + rand.nextInt(3);
                    double sla = opcionesSLA[rand.nextInt(opcionesSLA.length)];
                    String idPed = String.format("P5D-D%d-T%d-%02d", d, t, p + 1);
                    Pedido ped = new Pedido(idPed, new NodoCuadricula("Dest-" + idPed, x, y, TipoNodo.CLIENTE), cant, tiempoInicioTurnoRelativo, sla);
                    colaGlobalPedidos.add(ped);
                    metDia.pedidosRecibidos++;
                }

                // Configurar contexto del turno
                ContextoProblema ctxTurno = new ContextoProblema(tiempoInicioTurnoRelativo, mapa, config.getOperacion());
                ctxTurno.agregarAlmacen(almCentral);
                ctxTurno.agregarAlmacen(alm1);
                ctxTurno.agregarAlmacen(alm2);

                for (EstadoVehiculo ev : config.getFlota()) {
                    EstadoVehiculo copiaEv = new EstadoVehiculo(
                            ev.getId(),
                            ev.getTipo(),
                            ev.getAlmacenBase(),
                            ev.getAlmacenBase(),
                            0.0,
                            horaReloj
                    );
                    ctxTurno.agregarVehiculo(copiaEv);
                }

                for (Pedido ped : colaGlobalPedidos) {
                    ctxTurno.agregarPedido(ped);
                }

                // Resolver rutas con el planificador ALNS
                Solucion solucionTurno = planificador.resolver(ctxTurno);

                // Procesar entregas de este turno
                Set<Pedido> pedidosAtendidosTurno = new HashSet<>();
                for (Ruta ruta : solucionTurno.getRutas()) {
                    if (ruta.getPedidosAsignados().isEmpty()) continue;

                    metDia.distanciaTotalKm += ruta.getDistanciaTotal();
                    metDia.costoTotalSoles += ruta.getCostoTotal();

                    // Descontar inventario de almacén base
                    EstadoVehiculo ev = ruta.getVehiculo();
                    int demandaRuta = 0;
                    for (Pedido ped : ruta.getPedidosAsignados()) {
                        demandaRuta += ped.getCantidad();
                        pedidosAtendidosTurno.add(ped);

                        // Comprobar si se entregó dentro del SLA
                        double tiempoLlegadaGlobal = tiempoInicioTurnoRelativo + (ruta.getTiemposLlegada().get(ruta.getPedidosAsignados().indexOf(ped) + 1));
                        if (tiempoLlegadaGlobal <= ped.getTiempoMaximoEntrega()) {
                            metDia.pedidosATiempo++;
                        } else {
                            metDia.pedidosTardios++;
                        }
                    }

                    if (ev.getAlmacenBase().getId().equals(alm1.getId())) {
                        alm1.despachar(demandaRuta);
                        metDia.stockConsumidoAlm1 += demandaRuta;
                    } else if (ev.getAlmacenBase().getId().equals(alm2.getId())) {
                        alm2.despachar(demandaRuta);
                        metDia.stockConsumidoAlm2 += demandaRuta;
                    }
                }

                metDia.pedidosEntregados += pedidosAtendidosTurno.size();
                colaGlobalPedidos.removeAll(pedidosAtendidosTurno);
            }

            // Recarga instantánea a las 23:59:59 de cada día (almacenes intermedios recargan a 1,000)
            alm1.recargar();
            alm2.recargar();

            resultado.detallePorDia.add(metDia);
            resultado.totalPedidosAtendidos += metDia.pedidosEntregados;
            resultado.totalPedidosATiempo += metDia.pedidosATiempo;
            resultado.totalPedidosTardios += metDia.pedidosTardios;
            resultado.distanciaTotalKm += metDia.distanciaTotalKm;
            resultado.costoTotalSoles += metDia.costoTotalSoles;
        }

        resultado.tiempoComputoMs = System.currentTimeMillis() - tInicio;
        int totalAtendidos = resultado.totalPedidosAtendidos;
        resultado.porcentajeCumplimiento = (totalAtendidos > 0) ? (resultado.totalPedidosATiempo * 100.0 / totalAtendidos) : 100.0;

        return resultado;
    }
}
