package com.paqrap.visor;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.configuracion.LectorJson;
import com.paqrap.modelo.*;
import com.paqrap.simulador.EventoSimulacion;
import com.paqrap.simulador.TipoEvento;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor encargado de reconstruir el estado completo del sistema (posiciones,
 * estados de pedidos, calles bloqueadas y KPIs) para cualquier instante de tiempo t.
 */
public class MotorEstadoVisor {
    private final ConfiguracionSistema config;
    private final List<EventoSimulacion> eventos;
    private final Map<String, List<EventoSimulacion>> eventosPorVehiculo;
    private final double tiempoMaximo;
    private final Pattern patronCarga = Pattern.compile("Carga(?: remanente en vehículo)?: (\\d+)");
    private final Pattern patronDist = Pattern.compile("Dist: ([0-9.]+) km");
    private final Pattern patronCosto = Pattern.compile("Costo: S/ ([0-9.]+)");

    public MotorEstadoVisor(ConfiguracionSistema config, List<EventoSimulacion> eventos) {
        this.config = config;
        this.eventos = new ArrayList<>(eventos);
        this.eventos.sort(Comparator.comparingDouble(EventoSimulacion::getTiempoHoras));
        this.eventosPorVehiculo = new HashMap<>();

        double maxT = 8.0;
        for (EventoSimulacion e : this.eventos) {
            if (e.getTiempoHoras() > maxT) {
                maxT = e.getTiempoHoras();
            }
            if (e.getVehiculoId() != null) {
                eventosPorVehiculo.computeIfAbsent(e.getVehiculoId(), k -> new ArrayList<>()).add(e);
            }
        }
        this.tiempoMaximo = maxT;
    }

    public double getTiempoMaximo() {
        return tiempoMaximo;
    }

    public List<EventoSimulacion> getEventos() {
        return eventos;
    }

    public ConfiguracionSistema getConfig() {
        return config;
    }

    public EstadoVisor calcularEstado(double t) {
        double horaBase = config.getOperacion().getHoraInicioTurno();
        String reloj = EventoSimulacion.formatearReloj(t, horaBase);
        EstadoVisor estado = new EstadoVisor(t, reloj);

        // 1. Inicializar Pedidos (base inicial)
        for (Pedido p : config.getPedidosIniciales()) {
            InfoPedidoVisor ip = new InfoPedidoVisor(
                    p.getId(),
                    p.getDestino().getId(),
                    p.getDestino().getX(),
                    p.getDestino().getY(),
                    p.getCantidad(),
                    p.getTiempoLiberacion(),
                    p.getPlazoHoras()
            );
            estado.getPedidos().put(p.getId(), ip);
        }

        // 2. Inicializar Vehículos en sus almacenes base
        Map<String, TipoVehiculo> tiposMap = new HashMap<>();
        for (TipoVehiculo tv : config.getTiposVehiculo()) {
            tiposMap.put(tv.getId().toUpperCase(), tv);
        }

        for (EstadoVehiculo ev : config.getFlota()) {
            String vid = ev.getId();
            TipoVehiculo tv = ev.getTipo();
            int cap = tv != null ? tv.getCapacidad() : 24;
            String base = ev.getAlmacenBase() != null ? ev.getAlmacenBase().getId() : "AlmacenCentral";
            int bx = ev.getAlmacenBase() != null ? ev.getAlmacenBase().getX() : config.getAlmacenCentral().getX();
            int by = ev.getAlmacenBase() != null ? ev.getAlmacenBase().getY() : config.getAlmacenCentral().getY();

            InfoVehiculoVisor iv = new InfoVehiculoVisor(vid, tv != null ? tv.getNombre() : "Auto", bx, by, cap, base);
            estado.getVehiculos().put(vid, iv);
        }

        // 3. Procesar eventos hasta t para disrupciones, pedidos expres y estado general
        Set<String> bloqueosIds = new HashSet<>();
        List<EventoSimulacion> eventosHastaT = new ArrayList<>();

        for (EventoSimulacion e : eventos) {
            if (e.getTiempoHoras() > t) break;
            eventosHastaT.add(e);

            if (e.getTipo() == TipoEvento.INCIDENCIA_BLOQUEO) {
                // Parsear esquinas bloqueadas
                int x1 = e.getX();
                int y1 = e.getY();
                // Si el evento tiene coordenadas válidas
                if (x1 >= 0 && y1 >= 0) {
                    // Buscar coincidencia en disrupciones de config
                    for (Map<String, Object> dis : config.getDisrupciones()) {
                        if ("BLOQUEO_CALLE".equalsIgnoreCase(LectorJson.getString(dis, "tipo", ""))) {
                            int dx1 = LectorJson.getInt(dis, "x1", 0);
                            int dy1 = LectorJson.getInt(dis, "y1", 0);
                            int dx2 = LectorJson.getInt(dis, "x2", 0);
                            int dy2 = LectorJson.getInt(dis, "y2", 0);
                            String bid = dx1 + "," + dy1 + "-" + dx2 + "," + dy2;
                            if ((dx1 == x1 && dy1 == y1) && !bloqueosIds.contains(bid)) {
                                bloqueosIds.add(bid);
                                estado.getBloqueosActivos().add(new TramoBloqueado(dx1, dy1, dx2, dy2, e.getTiempoHoras(), e.getDescripcion()));
                            }
                        }
                    }
                }
            } else if (e.getTipo() == TipoEvento.INCIDENCIA_AVERIA_VEHICULO) {
                if (e.getVehiculoId() != null) {
                    estado.getVehiculosAveriados().add(e.getVehiculoId());
                }
            } else if (e.getTipo() == TipoEvento.INCIDENCIA_NUEVO_PEDIDO) {
                if (e.getPedidoId() != null && !estado.getPedidos().containsKey(e.getPedidoId())) {
                    for (Map<String, Object> dis : config.getDisrupciones()) {
                        if (e.getPedidoId().equals(LectorJson.getString(dis, "id", ""))) {
                            InfoPedidoVisor pExp = new InfoPedidoVisor(
                                    e.getPedidoId(),
                                    LectorJson.getString(dis, "destinoId", "C-Exp"),
                                    LectorJson.getInt(dis, "x", e.getX()),
                                    LectorJson.getInt(dis, "y", e.getY()),
                                    LectorJson.getInt(dis, "cantidad", 1),
                                    e.getTiempoHoras(),
                                    LectorJson.getDouble(dis, "plazoHoras", 4.0)
                            );
                            estado.getPedidos().put(e.getPedidoId(), pExp);
                            break;
                        }
                    }
                }
            } else if (e.getTipo() == TipoEvento.FIN_SERVICIO_ENTREGA) {
                if (e.getPedidoId() != null) {
                    InfoPedidoVisor ip = estado.getPedidos().get(e.getPedidoId());
                    if (ip != null) {
                        ip.setEstado("ENTREGADO");
                        ip.setTiempoEntrega(e.getTiempoHoras());
                        ip.setRelojEntrega(e.getReloj());
                    }
                }
            }
        }

        // 4. Calcular estado, posición e historial de cada vehículo
        double distGlobal = 0.0;
        double costoGlobal = 0.0;

        for (Map.Entry<String, InfoVehiculoVisor> entry : estado.getVehiculos().entrySet()) {
            String vid = entry.getKey();
            InfoVehiculoVisor iv = entry.getValue();

            List<EventoSimulacion> evs = eventosPorVehiculo.get(vid);
            if (evs == null || evs.isEmpty()) {
                continue;
            }

            // Buscar último evento antes o en t, y primer evento después de t
            EventoSimulacion evPrevio = null;
            EventoSimulacion evSiguiente = null;

            for (EventoSimulacion e : evs) {
                if (e.getTiempoHoras() <= t) {
                    evPrevio = e;
                    // Agregar a trayectoria
                    if (e.getX() >= 0 && e.getY() >= 0) {
                        iv.agregarPuntoTrayectoria(e.getX(), e.getY());
                    }
                    // Extracción de dist y costo
                    Matcher mDist = patronDist.matcher(e.getDatosAdicionales());
                    if (mDist.find()) {
                        try { distGlobal = Math.max(distGlobal, Double.parseDouble(mDist.group(1))); } catch (Exception ignored) {}
                    }
                    Matcher mCost = patronCosto.matcher(e.getDatosAdicionales());
                    if (mCost.find()) {
                        try { costoGlobal = Math.max(costoGlobal, Double.parseDouble(mCost.group(1))); } catch (Exception ignored) {}
                    }
                } else {
                    evSiguiente = e;
                    break;
                }
            }

            if (estado.getVehiculosAveriados().contains(vid)) {
                iv.setEstado("AVERIADO");
                iv.setDetalleEstado("⚠️ Avería mecánica permanente. Fuera de servicio.");
                // Ubicar en coordenadas de la avería
                for (EventoSimulacion e : evs) {
                    if (e.getTipo() == TipoEvento.INCIDENCIA_AVERIA_VEHICULO) {
                        iv.setX(e.getX());
                        iv.setY(e.getY());
                        break;
                    }
                }
                continue;
            }

            if (evPrevio == null) {
                // Todavía no inicia operaciones
                iv.setEstado("EN_ESPERA");
                iv.setDetalleEstado("En espera de despacho en almacén base");
                continue;
            }

            // Parsear carga actual del último evento
            Matcher mCarga = patronCarga.matcher(evPrevio.getDatosAdicionales());
            if (mCarga.find()) {
                try {
                    iv.setCargaActual(Integer.parseInt(mCarga.group(1)));
                } catch (Exception ignored) {}
            }

            // Interpolación de posición si está en tránsito
            if (evSiguiente != null && evSiguiente.getTipo() == TipoEvento.MOVIMIENTO_TRAMO &&
                evPrevio.getX() >= 0 && evPrevio.getY() >= 0 && evSiguiente.getX() >= 0 && evSiguiente.getY() >= 0) {
                double dt = evSiguiente.getTiempoHoras() - evPrevio.getTiempoHoras();
                if (dt > 0.0001) {
                    double fraccion = Math.max(0.0, Math.min(1.0, (t - evPrevio.getTiempoHoras()) / dt));
                    double interX = evPrevio.getX() + fraccion * (evSiguiente.getX() - evPrevio.getX());
                    double interY = evPrevio.getY() + fraccion * (evSiguiente.getY() - evPrevio.getY());
                    iv.setX(interX);
                    iv.setY(interY);
                    iv.agregarPuntoTrayectoria(interX, interY);
                } else {
                    iv.setX(evPrevio.getX());
                    iv.setY(evPrevio.getY());
                }
            } else {
                if (evPrevio.getX() >= 0 && evPrevio.getY() >= 0) {
                    iv.setX(evPrevio.getX());
                    iv.setY(evPrevio.getY());
                }
            }

            // Determinar estado textual
            TipoEvento te = evPrevio.getTipo();
            if (te == TipoEvento.INICIO_REFRIGERIO) {
                double duracionRef = config.getOperacion().getDuracionRefrigerioHoras();
                if (t < evPrevio.getTiempoHoras() + duracionRef) {
                    iv.setEstado("REFRIGERIO");
                    iv.setDetalleEstado("☕ Pausa obligatoria de almuerzo / refrigerio");
                } else {
                    iv.setEstado("EN_RUTA");
                    iv.setDetalleEstado("Reanudando servicio post-refrigerio");
                }
            } else if (te == TipoEvento.INICIO_SERVICIO) {
                double tServ = config.getOperacion().getTiempoServicioClienteHoras();
                if (t < evPrevio.getTiempoHoras() + tServ) {
                    iv.setEstado("ENTREGANDO");
                    iv.setDetalleEstado("📦 Entregando pedido " + evPrevio.getPedidoId() + " en cliente");
                    if (evPrevio.getPedidoId() != null) {
                        InfoPedidoVisor ip = estado.getPedidos().get(evPrevio.getPedidoId());
                        if (ip != null && !"ENTREGADO".equals(ip.getEstado())) {
                            ip.setEstado("EN_ENTREGA");
                            ip.setVehiculoAsignado(vid);
                        }
                    }
                } else {
                    iv.setEstado("EN_RUTA");
                    iv.setDetalleEstado("Entrega completada. Próximo destino.");
                }
            } else if (te == TipoEvento.RETORNO_ALMACEN) {
                iv.setEstado("RETORNANDO");
                iv.setDetalleEstado("🔄 En ruta de retorno al almacén base");
            } else if (te == TipoEvento.LLEGADA_ALMACEN) {
                iv.setEstado("EN_ALMACEN");
                iv.setDetalleEstado("🏁 Jornada concluida. En almacén base");
            } else if (te == TipoEvento.MOVIMIENTO_TRAMO) {
                iv.setEstado("EN_RUTA");
                iv.setDetalleEstado(evPrevio.getDescripcion());
                if (evPrevio.getPedidoId() != null) {
                    iv.setPedidoActual(evPrevio.getPedidoId());
                    InfoPedidoVisor ip = estado.getPedidos().get(evPrevio.getPedidoId());
                    if (ip != null && "PENDIENTE".equals(ip.getEstado())) {
                        ip.setEstado("EN_TRANSITO");
                        ip.setVehiculoAsignado(vid);
                    }
                }
            } else if (te == TipoEvento.DESPACHO_VEHICULO) {
                iv.setEstado("DESPACHADO");
                iv.setDetalleEstado(evPrevio.getDescripcion());
            } else {
                iv.setEstado("OPERATIVO");
                iv.setDetalleEstado(evPrevio.getDescripcion());
            }
        }

        // 5. Contabilizar pedidos y KPIs
        int entregados = 0;
        int pendientes = 0;
        int enTransito = 0;

        for (InfoPedidoVisor ip : estado.getPedidos().values()) {
            if ("ENTREGADO".equals(ip.getEstado())) {
                entregados++;
            } else if ("EN_TRANSITO".equals(ip.getEstado()) || "EN_ENTREGA".equals(ip.getEstado())) {
                enTransito++;
            } else {
                pendientes++;
                if (t > ip.getTiempoMaximoEntrega()) {
                    ip.setEstado("TARDE");
                }
            }
        }

        estado.setPedidosEntregados(entregados);
        estado.setPedidosPendientes(pendientes);
        estado.setPedidosEnTransito(enTransito);
        estado.setDistanciaTotalKm(distGlobal);
        estado.setCostoTotalSoles(costoGlobal);

        // 6. Últimos eventos para la consola
        int nEventos = eventosHastaT.size();
        int desde = Math.max(0, nEventos - 25);
        for (int i = desde; i < nEventos; i++) {
            estado.getEventosRecientes().add(eventosHastaT.get(i));
        }

        return estado;
    }
}
