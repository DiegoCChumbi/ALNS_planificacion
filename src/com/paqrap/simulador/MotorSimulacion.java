package com.paqrap.simulador;

import com.paqrap.evaluador.CalculadorDistancia;
import com.paqrap.modelo.*;
import java.util.*;

public class MotorSimulacion {
    private final MapaCuadricula mapa;
    private final GestorLogSimulacion gestorLog;

    public MotorSimulacion(MapaCuadricula mapa, GestorLogSimulacion gestorLog) {
        this.mapa = mapa;
        this.gestorLog = gestorLog;
    }

    /**
     * Simula la ejecución de un conjunto de rutas desde un tiempo inicial hasta un tiempo límite o fin de rutas.
     * Genera eventos cronológicos micro (calle por calle) y macro (paradas, entregas, estados).
     */
    public List<EventoSimulacion> simularRutas(Solucion solucion, double tiempoInicio, double tiempoFinMax) {
        List<EventoSimulacion> eventosFase = new ArrayList<>();

        int totalPedidos = 0;
        for (Ruta r : solucion.getRutas()) {
            totalPedidos += r.getPedidosAsignados().size();
        }

        eventosFase.add(new EventoSimulacion(
                tiempoInicio,
                TipoEvento.PLANIFICACION_RUTAS,
                null,
                null,
                -1,
                -1,
                "Rutas planificadas activadas (" + solucion.getRutas().size() + " vehículos, " + totalPedidos + " pedidos asignados)",
                String.format(Locale.US, "Costo estimado: S/ %.2f | No asignados: %d", solucion.calcularCostoTotal(), solucion.getPedidosNoAsignados().size())
        ));

        for (Ruta ruta : solucion.getRutas()) {
            if (ruta.getPedidosAsignados().isEmpty()) continue;

            EstadoVehiculo vehiculo = ruta.getVehiculo();
            TipoVehiculo tipoVehiculo = vehiculo.getTipo();
            double velocidad = tipoVehiculo.getVelocidadKmH();
            double costoKm = tipoVehiculo.getCostoPorKm();

            int cargaActual = 0;
            for (Pedido p : ruta.getPedidosAsignados()) {
                cargaActual += p.getCantidad();
            }

            NodoCuadricula nodoActual = vehiculo.getUbicacionActual();
            double tiempoActual = Math.max(tiempoInicio, vehiculo.getMarcaTiempoActual());
            double distanciaRecorrida = 0.0;

            eventosFase.add(new EventoSimulacion(
                    tiempoActual,
                    TipoEvento.DESPACHO_VEHICULO,
                    vehiculo.getId(),
                    null,
                    nodoActual.getX(),
                    nodoActual.getY(),
                    String.format("Vehículo despachado desde %s con %d pedidos", nodoActual.getId(), ruta.getPedidosAsignados().size()),
                    String.format("Carga: %d/%d paq. | Vel: %.0f km/h", cargaActual, tipoVehiculo.getCapacidad(), velocidad)
            ));

            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (tiempoActual >= tiempoFinMax) break;

                NodoCuadricula destino = pedido.getDestino();
                List<NodoCuadricula> camino = mapa.getCaminoMasCorto(nodoActual, destino);

                // Movimiento esquina por esquina
                for (int i = 0; i < camino.size() - 1; i++) {
                    NodoCuadricula origenPaso = camino.get(i);
                    NodoCuadricula destinoPaso = camino.get(i + 1);

                    double distPaso = 1.0; // En cuadrícula, cada salto entre esquinas contiguas es 1 km
                    double tiempoPaso = distPaso / velocidad;

                    tiempoActual += tiempoPaso;
                    distanciaRecorrida += distPaso;
                    double costoAcum = distanciaRecorrida * costoKm;

                    if (tiempoActual > tiempoFinMax) {
                        tiempoActual = tiempoFinMax;
                        nodoActual = destinoPaso;
                        break;
                    }

                    eventosFase.add(new EventoSimulacion(
                            tiempoActual,
                            TipoEvento.MOVIMIENTO_TRAMO,
                            vehiculo.getId(),
                            pedido.getId(),
                            destinoPaso.getX(),
                            destinoPaso.getY(),
                            String.format("Tránsito tramo (%d,%d) -> (%d,%d) hacia %s",
                                    origenPaso.getX(), origenPaso.getY(), destinoPaso.getX(), destinoPaso.getY(), destino.getId()),
                            String.format(Locale.US, "Dist: %.1f km | Costo: S/ %.2f | Carga: %d", distanciaRecorrida, costoAcum, cargaActual)
                    ));
                }

                if (tiempoActual >= tiempoFinMax) {
                    nodoActual = destino;
                    break;
                }

                nodoActual = destino;

                // Llegada a cliente
                eventosFase.add(new EventoSimulacion(
                        tiempoActual,
                        TipoEvento.LLEGADA_A_DESTINO,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Arribo a destino de cliente %s", destino.getId()),
                        String.format(Locale.US, "Pedido: %s | Demanda: %d", pedido.getId(), pedido.getCantidad())
                ));

                // Inicio servicio (1 hora de atención)
                eventosFase.add(new EventoSimulacion(
                        tiempoActual,
                        TipoEvento.INICIO_SERVICIO,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Inicia entrega y descarga de pedido %s", pedido.getId()),
                        "Duración de servicio estimada: 1.00h"
                ));

                double tiempoFinServicio = tiempoActual + 1.0;
                boolean aTiempo = (tiempoActual <= pedido.getTiempoMaximoEntrega());
                double holgura = pedido.getTiempoMaximoEntrega() - tiempoActual;

                tiempoActual = Math.min(tiempoFinServicio, tiempoFinMax);
                cargaActual -= pedido.getCantidad();

                eventosFase.add(new EventoSimulacion(
                        tiempoActual,
                        TipoEvento.FIN_SERVICIO_ENTREGA,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Finalizó entrega de %s | Estado: %s (Plazo límite: %.1fh, Margen: %+.2fh)",
                                pedido.getId(), aTiempo ? "A TIEMPO (CUMPLE)" : "TARDÍO (DEMORA)", pedido.getTiempoMaximoEntrega(), holgura),
                        String.format("Carga remanente en vehículo: %d/%d paq.", cargaActual, tipoVehiculo.getCapacidad())
                ));
            }
        }

        // Ordenar cronológicamente todos los eventos de la fase
        eventosFase.sort(Comparator.comparingDouble(EventoSimulacion::getTiempoHoras));

        for (EventoSimulacion ev : eventosFase) {
            gestorLog.registrarEvento(ev);
        }

        return eventosFase;
    }

    public void registrarBloqueoCalle(double tiempo, int x1, int y1, int x2, int y2) {
        mapa.bloquearArista(x1, y1, x2, y2);
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                TipoEvento.INCIDENCIA_BLOQUEO,
                null,
                null,
                x1,
                y1,
                String.format("ALERTA VIAL: Calle bloqueada entre esquinas (%d,%d) y (%d,%d). Requiere desvío.", x1, y1, x2, y2),
                "Estado: VÍA CLAUSURADA"
        ));
    }

    public void registrarNuevoPedido(double tiempo, Pedido pedido) {
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                TipoEvento.INCIDENCIA_NUEVO_PEDIDO,
                null,
                pedido.getId(),
                pedido.getDestino().getX(),
                pedido.getDestino().getY(),
                String.format("NUEVA SOLICITUD: Ingresó pedido exprés %s en destino %s (%d,%d)",
                        pedido.getId(), pedido.getDestino().getId(), pedido.getDestino().getX(), pedido.getDestino().getY()),
                String.format("Demanda: %d paq. | Plazo: %.1fh (Entrega máx: %.1fh)", pedido.getCantidad(), pedido.getPlazoHoras(), pedido.getTiempoMaximoEntrega())
        ));
    }

    public void registrarFinSimulacion(double tiempo, String resumenFinal) {
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                TipoEvento.FIN_SIMULACION,
                null,
                null,
                -1,
                -1,
                "Simulación finalizada exitosamente.",
                resumenFinal
        ));
        gestorLog.guardarLogs();
    }
}
