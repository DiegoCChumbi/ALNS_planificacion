package com.paqrap.simulador;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.evaluador.CalculadorDistancia;
import com.paqrap.modelo.*;
import java.util.*;

public class MotorSimulacion {
    private final MapaCuadricula mapa;
    private final GestorLogSimulacion gestorLog;
    private final ConfiguracionSistema configuracion;
    private final Map<String, EstadoVehiculo> estadosVehiculos;
    private final Set<String> pedidosCompletados;

    public MotorSimulacion(MapaCuadricula mapa, GestorLogSimulacion gestorLog, ConfiguracionSistema configuracion) {
        this.mapa = mapa;
        this.gestorLog = gestorLog;
        this.configuracion = (configuracion != null) ? configuracion : new ConfiguracionSistema();
        this.estadosVehiculos = new HashMap<>();
        this.pedidosCompletados = new HashSet<>();
    }

    public MotorSimulacion(MapaCuadricula mapa, GestorLogSimulacion gestorLog) {
        this(mapa, gestorLog, new ConfiguracionSistema());
    }

    public List<EventoSimulacion> simularRutas(Solucion solucion, double tiempoInicio, double tiempoFinMax) {
        List<EventoSimulacion> eventosFase = new ArrayList<>();
        double horaBase = configuracion.getOperacion().getHoraInicioTurno();
        double kmPorCuadra = configuracion.getEntorno().getKmPorCuadra();
        double tiempoServicio = configuracion.getOperacion().getTiempoServicioClienteHoras();

        int totalPedidos = 0;
        for (Ruta r : solucion.getRutas()) {
            totalPedidos += r.getPedidosAsignados().size();
        }

        eventosFase.add(new EventoSimulacion(
                tiempoInicio,
                horaBase,
                TipoEvento.PLANIFICACION_RUTAS,
                null,
                null,
                -1,
                -1,
                "Rutas planificadas activadas (" + solucion.getRutas().size() + " vehículos, " + totalPedidos + " pedidos asignados)",
                String.format(Locale.US, "Costo estimado: S/ %.2f | No asignados: %d",
                        solucion.calcularCostoTotal(configuracion.getOperacion().getPenalizacionPedidoNoAsignado()),
                        solucion.getPedidosNoAsignados().size())
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
                    horaBase,
                    TipoEvento.DESPACHO_VEHICULO,
                    vehiculo.getId(),
                    null,
                    nodoActual.getX(),
                    nodoActual.getY(),
                    String.format("Vehículo despachado desde %s con %d pedidos", nodoActual.getId(), ruta.getPedidosAsignados().size()),
                    String.format("Carga: %d/%d paq. | Vel: %.0f km/h", cargaActual, tipoVehiculo.getCapacidad(), velocidad)
            ));

            double duracionRefrigerio = configuracion.getOperacion().getDuracionRefrigerioHoras();
            double tiempoInicioRefrigerio = configuracion.getOperacion().getDuracionTurnoHoras() / 2.0;
            boolean refrigerioTomado = vehiculo.isRefrigerioTomado();

            for (Pedido pedido : ruta.getPedidosAsignados()) {
                if (tiempoActual >= tiempoFinMax) break;

                // Pausa obligatoria de refrigerio si llega la hora del almuerzo antes del viaje
                if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerio && tiempoActual < tiempoFinMax) {
                    double tInicioRef = tiempoActual;
                    double tFinRef = Math.min(tInicioRef + duracionRefrigerio, tiempoFinMax);
                    eventosFase.add(new EventoSimulacion(
                            tInicioRef,
                            horaBase,
                            TipoEvento.INICIO_REFRIGERIO,
                            vehiculo.getId(),
                            null,
                            nodoActual.getX(),
                            nodoActual.getY(),
                            String.format("Conductor de %s inicia pausa obligatoria de refrigerio (%.1fh)", vehiculo.getId(), duracionRefrigerio),
                            String.format("Pausa en (%d,%d) | Turno: %02d:00", nodoActual.getX(), nodoActual.getY(), (int) horaBase)
                    ));
                    tiempoActual = tFinRef;
                    if (tInicioRef + duracionRefrigerio <= tiempoFinMax) {
                        refrigerioTomado = true;
                        eventosFase.add(new EventoSimulacion(
                                tiempoActual,
                                horaBase,
                                TipoEvento.FIN_REFRIGERIO,
                                vehiculo.getId(),
                                null,
                                nodoActual.getX(),
                                nodoActual.getY(),
                                String.format("Conductor de %s finaliza refrigerio y reanuda operaciones", vehiculo.getId()),
                                String.format("Tiempo reanudación: t=%.2fh", tiempoActual)
                        ));
                    }
                }

                if (tiempoActual >= tiempoFinMax) break;

                NodoCuadricula destino = pedido.getDestino();
                List<NodoCuadricula> camino = mapa.getCaminoMasCorto(nodoActual, destino);

                // Movimiento esquina por esquina
                for (int i = 0; i < camino.size() - 1; i++) {
                    NodoCuadricula origenPaso = camino.get(i);
                    NodoCuadricula destinoPaso = camino.get(i + 1);

                    double distPaso = kmPorCuadra;
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
                            horaBase,
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
                        horaBase,
                        TipoEvento.LLEGADA_A_DESTINO,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Arribo a destino de cliente %s", destino.getId()),
                        String.format(Locale.US, "Pedido: %s | Demanda: %d", pedido.getId(), pedido.getCantidad())
                ));

                // Inicio servicio
                eventosFase.add(new EventoSimulacion(
                        tiempoActual,
                        horaBase,
                        TipoEvento.INICIO_SERVICIO,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Inicia entrega y descarga de pedido %s", pedido.getId()),
                        String.format(Locale.US, "Duración de servicio: %.2fh", tiempoServicio)
                ));

                double tiempoFinServicio = tiempoActual + tiempoServicio;
                boolean aTiempo = (tiempoActual <= pedido.getTiempoMaximoEntrega());
                double holgura = pedido.getTiempoMaximoEntrega() - tiempoActual;
                String colorSemaforo = configuracion.getSemaforos().determinarColor(holgura);

                tiempoActual = Math.min(tiempoFinServicio, tiempoFinMax);
                cargaActual -= pedido.getCantidad();

                if (tiempoFinServicio <= tiempoFinMax) {
                    pedidosCompletados.add(pedido.getId());
                }

                eventosFase.add(new EventoSimulacion(
                        tiempoActual,
                        horaBase,
                        TipoEvento.FIN_SERVICIO_ENTREGA,
                        vehiculo.getId(),
                        pedido.getId(),
                        destino.getX(),
                        destino.getY(),
                        String.format("Finalizó entrega de %s | Estado: %s [Semáforo: %s] (Plazo límite: %.1fh, Margen: %+.2fh)",
                                pedido.getId(), aTiempo ? "A TIEMPO (CUMPLE)" : "TARDÍO (DEMORA)", colorSemaforo, pedido.getTiempoMaximoEntrega(), holgura),
                        String.format("Carga remanente en vehículo: %d/%d paq.", cargaActual, tipoVehiculo.getCapacidad())
                ));
            }

            // Retorno al almacén base si ya entregó todos sus pedidos
            NodoCuadricula base = vehiculo.getAlmacenBase();
            if (base != null && !nodoActual.equals(base) && cargaActual == 0 && tiempoActual < tiempoFinMax) {
                // Pausa de refrigerio en camino si no se tomó aún
                if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerio && tiempoActual < tiempoFinMax) {
                    double tInicioRef = tiempoActual;
                    double tFinRef = Math.min(tInicioRef + duracionRefrigerio, tiempoFinMax);
                    eventosFase.add(new EventoSimulacion(
                            tInicioRef,
                            horaBase,
                            TipoEvento.INICIO_REFRIGERIO,
                            vehiculo.getId(),
                            null,
                            nodoActual.getX(),
                            nodoActual.getY(),
                            String.format("Conductor de %s inicia pausa obligatoria de refrigerio (%.1fh)", vehiculo.getId(), duracionRefrigerio),
                            String.format("Pausa en (%d,%d) previa al retorno a base", nodoActual.getX(), nodoActual.getY())
                    ));
                    tiempoActual = tFinRef;
                    if (tInicioRef + duracionRefrigerio <= tiempoFinMax) {
                        refrigerioTomado = true;
                        eventosFase.add(new EventoSimulacion(
                                tiempoActual,
                                horaBase,
                                TipoEvento.FIN_REFRIGERIO,
                                vehiculo.getId(),
                                null,
                                nodoActual.getX(),
                                nodoActual.getY(),
                                String.format("Conductor de %s finaliza refrigerio y reanuda retorno a base", vehiculo.getId()),
                                String.format("Tiempo reanudación: t=%.2fh", tiempoActual)
                        ));
                    }
                }

                if (tiempoActual < tiempoFinMax) {
                    eventosFase.add(new EventoSimulacion(
                            tiempoActual,
                            horaBase,
                            TipoEvento.RETORNO_ALMACEN,
                            vehiculo.getId(),
                            null,
                            nodoActual.getX(),
                            nodoActual.getY(),
                            String.format("Iniciando retorno hacia almacén base %s (%d,%d)", base.getId(), base.getX(), base.getY()),
                            String.format(Locale.US, "Distancia previa: %.1f km | Descarga completa", distanciaRecorrida)
                    ));

                    List<NodoCuadricula> caminoRetorno = mapa.getCaminoMasCorto(nodoActual, base);
                    for (int i = 0; i < caminoRetorno.size() - 1; i++) {
                        NodoCuadricula origenPaso = caminoRetorno.get(i);
                        NodoCuadricula destinoPaso = caminoRetorno.get(i + 1);

                        double distPaso = kmPorCuadra;
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
                                horaBase,
                                TipoEvento.MOVIMIENTO_TRAMO,
                                vehiculo.getId(),
                                null,
                                destinoPaso.getX(),
                                destinoPaso.getY(),
                                String.format("Tránsito retorno (%d,%d) -> (%d,%d) hacia base %s",
                                        origenPaso.getX(), origenPaso.getY(), destinoPaso.getX(), destinoPaso.getY(), base.getId()),
                                String.format(Locale.US, "Dist: %.1f km | Costo: S/ %.2f | Retorno a Base", distanciaRecorrida, costoAcum)
                        ));
                    }

                    if (tiempoActual <= tiempoFinMax && nodoActual.equals(base)) {
                        eventosFase.add(new EventoSimulacion(
                                tiempoActual,
                                horaBase,
                                TipoEvento.LLEGADA_ALMACEN,
                                vehiculo.getId(),
                                null,
                                base.getX(),
                                base.getY(),
                                String.format("Vehículo %s arribó exitosamente a su almacén base %s", vehiculo.getId(), base.getId()),
                                String.format(Locale.US, "Jornada completada | Dist total: %.1f km | Costo total: S/ %.2f", distanciaRecorrida, distanciaRecorrida * costoKm)
                        ));
                    }
                }
            }

            EstadoVehiculo evEstado = new EstadoVehiculo(vehiculo.getId(), tipoVehiculo, nodoActual, vehiculo.getAlmacenBase(), tiempoActual, vehiculo.getTiempoInicioTurno());
            evEstado.setCapacidadDisponible(tipoVehiculo.getCapacidad() - cargaActual);
            evEstado.setRefrigerioTomado(refrigerioTomado);
            estadosVehiculos.put(vehiculo.getId(), evEstado);
        }

        eventosFase.sort(Comparator.comparingDouble(EventoSimulacion::getTiempoHoras));

        for (EventoSimulacion ev : eventosFase) {
            gestorLog.registrarEvento(ev);
        }

        return eventosFase;
    }

    public Map<String, EstadoVehiculo> getEstadosVehiculos() {
        return estadosVehiculos;
    }

    public Set<String> getPedidosCompletados() {
        return pedidosCompletados;
    }

    public void registrarBloqueoCalle(double tiempo, int x1, int y1, int x2, int y2) {
        mapa.bloquearArista(x1, y1, x2, y2);
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                configuracion.getOperacion().getHoraInicioTurno(),
                TipoEvento.INCIDENCIA_BLOQUEO,
                null,
                null,
                x1,
                y1,
                String.format("ALERTA VIAL: Calle bloqueada entre esquinas (%d,%d) y (%d,%d). Requiere desvío.", x1, y1, x2, y2),
                "Estado: VÍA CLAUSURADA"
        ));
    }

    public void registrarAveriaVehiculo(double tiempo, String vehiculoId, int x, int y, String motivo) {
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                configuracion.getOperacion().getHoraInicioTurno(),
                TipoEvento.INCIDENCIA_AVERIA_VEHICULO,
                vehiculoId,
                null,
                x,
                y,
                String.format("EMERGENCIA DE FLOTA: Vehículo %s sufrió avería mecánica en (%d,%d). Fuera de servicio.", vehiculoId, x, y),
                "Motivo: " + motivo + " | Acción: Reasignación forzosa de pedidos"
        ));
    }

    public void registrarNuevoPedido(double tiempo, Pedido pedido) {
        gestorLog.registrarEvento(new EventoSimulacion(
                tiempo,
                configuracion.getOperacion().getHoraInicioTurno(),
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
                configuracion.getOperacion().getHoraInicioTurno(),
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
