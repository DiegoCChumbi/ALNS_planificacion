package com.paqrap.visor;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.configuracion.LectorJson;
import com.paqrap.modelo.NodoCuadricula;
import com.paqrap.simulador.EventoSimulacion;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embebido en Java estándar (com.sun.net.httpserver)
 * que provee una interfaz web HTML5/Canvas moderna y ligera para monitorear
 * la cuadrícula y flota desde cualquier navegador en tiempo real.
 */
public class ServidorVisorWeb {

    private static HttpServer servidorActivo = null;
    private static int puertoActivo = 8080;
    private final MotorEstadoVisor motor;
    private final int puerto;

    public ServidorVisorWeb(MotorEstadoVisor motor, int puerto) {
        this.motor = motor;
        this.puerto = puerto;
    }

    public static synchronized void iniciarSiNoActivo(MotorEstadoVisor motor) {
        if (servidorActivo != null) return;
        int p = 8080;
        while (p < 8090) {
            try {
                ServidorVisorWeb s = new ServidorVisorWeb(motor, p);
                s.iniciar();
                servidorActivo = s.getHttpServer();
                puertoActivo = p;
                System.out.println(">> [Visor Web] Servidor activo en http://localhost:" + p);
                break;
            } catch (IOException e) {
                p++;
            }
        }
    }

    public static int getPuertoActivo() {
        return puertoActivo;
    }

    private HttpServer server;

    public HttpServer getHttpServer() {
        return server;
    }

    public void iniciar() throws IOException {
        server = HttpServer.create(new InetSocketAddress(puerto), 0);
        server.createContext("/", new HandlerPaginaPrincipal());
        server.createContext("/api/todo", new HandlerTodoJson());
        server.createContext("/api/estado", new HandlerEstadoJson());
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
    }

    private class HandlerPaginaPrincipal implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            byte[] contenido = generarHtmlVisor().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, contenido.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(contenido);
            }
        }
    }

    private class HandlerTodoJson implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            ConfiguracionSistema cfg = motor.getConfig();

            // Mapa
            sb.append("\"mapa\":{");
            sb.append("\"ancho\":").append(cfg.getEntorno().getAnchoMapa()).append(",");
            sb.append("\"alto\":").append(cfg.getEntorno().getAltoMapa()).append(",");
            sb.append("\"kmPorCuadra\":").append(cfg.getEntorno().getKmPorCuadra()).append(",");
            sb.append("\"horaInicio\":").append(cfg.getOperacion().getHoraInicioTurno()).append(",");
            sb.append("\"tiempoMaximo\":").append(motor.getTiempoMaximo());
            sb.append("},");

            // Almacenes
            sb.append("\"almacenes\":{");
            NodoCuadricula ac = cfg.getAlmacenCentral();
            sb.append("\"central\":{\"id\":\"").append(ac.getId()).append("\",\"x\":").append(ac.getX()).append(",\"y\":").append(ac.getY()).append("},");
            sb.append("\"intermedios\":[");
            for (int i = 0; i < cfg.getAlmacenesIntermedios().size(); i++) {
                NodoCuadricula ai = cfg.getAlmacenesIntermedios().get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"id\":\"").append(ai.getId()).append("\",\"x\":").append(ai.getX()).append(",\"y\":").append(ai.getY()).append("}");
            }
            sb.append("]},");

            // Disrupciones
            sb.append("\"disrupciones\":[");
            List<Map<String, Object>> dis = cfg.getDisrupciones();
            for (int i = 0; i < dis.size(); i++) {
                Map<String, Object> d = dis.get(i);
                if (i > 0) sb.append(",");
                sb.append("{");
                sb.append("\"tiempoHoras\":").append(LectorJson.getDouble(d, "tiempoHoras", 0.0)).append(",");
                sb.append("\"tipo\":\"").append(LectorJson.getString(d, "tipo", "")).append("\",");
                sb.append("\"x1\":").append(LectorJson.getInt(d, "x1", -1)).append(",");
                sb.append("\"y1\":").append(LectorJson.getInt(d, "y1", -1)).append(",");
                sb.append("\"x2\":").append(LectorJson.getInt(d, "x2", -1)).append(",");
                sb.append("\"y2\":").append(LectorJson.getInt(d, "y2", -1)).append(",");
                sb.append("\"vehiculoId\":\"").append(LectorJson.getString(d, "vehiculoId", "")).append("\",");
                sb.append("\"motivo\":\"").append(LectorJson.getString(d, "motivo", "")).append("\"");
                sb.append("}");
            }
            sb.append("],");

            // Eventos
            sb.append("\"eventos\":[");
            List<EventoSimulacion> evs = motor.getEventos();
            for (int i = 0; i < evs.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(evs.get(i).toJson());
            }
            sb.append("]");

            sb.append("}");

            byte[] jsonBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, jsonBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonBytes);
            }
        }
    }

    private class HandlerEstadoJson implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");

            String query = exchange.getRequestURI().getQuery();
            double t = 0.0;
            if (query != null && query.contains("t=")) {
                for (String param : query.split("&")) {
                    if (param.startsWith("t=")) {
                        try {
                            t = Double.parseDouble(param.substring(2));
                        } catch (Exception ignored) {}
                    }
                }
            }

            EstadoVisor estado = motor.calcularEstado(t);
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"tiempoHoras\":").append(String.format(Locale.US, "%.4f", estado.getTiempoHoras())).append(",");
            sb.append("\"reloj\":\"").append(estado.getReloj()).append("\",");
            sb.append("\"pedidosEntregados\":").append(estado.getPedidosEntregados()).append(",");
            sb.append("\"pedidosPendientes\":").append(estado.getPedidosPendientes()).append(",");
            sb.append("\"pedidosEnTransito\":").append(estado.getPedidosEnTransito()).append(",");
            sb.append("\"distanciaTotalKm\":").append(String.format(Locale.US, "%.2f", estado.getDistanciaTotalKm())).append(",");
            sb.append("\"costoTotalSoles\":").append(String.format(Locale.US, "%.2f", estado.getCostoTotalSoles())).append(",");

            // Vehículos
            sb.append("\"vehiculos\":[");
            int vi = 0;
            for (InfoVehiculoVisor v : estado.getVehiculos().values()) {
                if (vi++ > 0) sb.append(",");
                sb.append("{");
                sb.append("\"id\":\"").append(v.getId()).append("\",");
                sb.append("\"tipo\":\"").append(v.getTipo()).append("\",");
                sb.append("\"x\":").append(String.format(Locale.US, "%.2f", v.getX())).append(",");
                sb.append("\"y\":").append(String.format(Locale.US, "%.2f", v.getY())).append(",");
                sb.append("\"cargaActual\":").append(v.getCargaActual()).append(",");
                sb.append("\"capacidadTotal\":").append(v.getCapacidadTotal()).append(",");
                sb.append("\"estado\":\"").append(v.getEstado()).append("\",");
                sb.append("\"detalle\":\"").append(v.getDetalleEstado().replace("\"", "\\\"")).append("\"");
                sb.append("}");
            }
            sb.append("],");

            // Pedidos
            sb.append("\"pedidos\":[");
            int pi = 0;
            for (InfoPedidoVisor p : estado.getPedidos().values()) {
                if (pi++ > 0) sb.append(",");
                sb.append("{");
                sb.append("\"id\":\"").append(p.getId()).append("\",");
                sb.append("\"destinoId\":\"").append(p.getDestinoId()).append("\",");
                sb.append("\"x\":").append(p.getX()).append(",");
                sb.append("\"y\":").append(p.getY()).append(",");
                sb.append("\"cantidad\":").append(p.getCantidad()).append(",");
                sb.append("\"plazo\":").append(String.format(Locale.US, "%.1f", p.getPlazoHoras())).append(",");
                sb.append("\"estado\":\"").append(p.getEstado()).append("\",");
                sb.append("\"tiempoEntrega\":").append(p.getTiempoEntrega() == null ? "null" : String.format(Locale.US, "%.2f", p.getTiempoEntrega())).append(",");
                sb.append("\"relojEntrega\":").append(p.getRelojEntrega() == null ? "null" : "\"" + p.getRelojEntrega() + "\"");
                sb.append("}");
            }
            sb.append("],");

            // Eventos recientes
            sb.append("\"eventos\":[");
            int ei = 0;
            for (EventoSimulacion ev : estado.getEventosRecientes()) {
                if (ei++ > 0) sb.append(",");
                sb.append("{");
                sb.append("\"reloj\":\"").append(ev.getReloj()).append("\",");
                sb.append("\"tipo\":\"").append(ev.getTipo()).append("\",");
                sb.append("\"descripcion\":\"").append(ev.getDescripcion().replace("\"", "\\\"")).append("\"");
                sb.append("}");
            }
            sb.append("],");

            // Bloqueos
            sb.append("\"bloqueos\":[");
            int bi = 0;
            for (TramoBloqueado b : estado.getBloqueosActivos()) {
                if (bi++ > 0) sb.append(",");
                sb.append("{\"x1\":").append(b.getX1()).append(",\"y1\":").append(b.getY1())
                  .append(",\"x2\":").append(b.getX2()).append(",\"y2\":").append(b.getY2()).append("}");
            }
            sb.append("]");

            sb.append("}");

            byte[] jsonBytes = sb.toString().getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, jsonBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(jsonBytes);
            }
        }
    }

    private String generarHtmlVisor() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>PaqRap - Visor de Monitoreo de Cuadrilla ALNS</title>\n" +
                "  <style>\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }\n" +
                "    body { background-color: #0b0f19; color: #f1f5f9; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }\n" +
                "    header { background: #0f172a; padding: 12px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid #1e293b; }\n" +
                "    .logo { font-size: 1.25rem; font-weight: 700; color: #38bdf8; display: flex; align-items: center; gap: 8px; }\n" +
                "    .sub { font-size: 0.8rem; color: #94a3b8; font-weight: normal; margin-left: 10px; }\n" +
                "    .reloj { font-family: monospace; font-size: 1.1rem; color: #facc15; font-weight: bold; background: #1e293b; padding: 4px 12px; border-radius: 6px; }\n" +
                "    .main-container { display: flex; flex: 1; overflow: hidden; }\n" +
                "    .map-container { flex: 1; position: relative; display: flex; justify-content: center; align-items: center; background: #090d16; overflow: hidden; }\n" +
                "    canvas { background: #111827; cursor: grab; box-shadow: 0 4px 20px rgba(0,0,0,0.5); border-radius: 8px; }\n" +
                "    canvas:active { cursor: grabbing; }\n" +
                "    .sidebar { width: 400px; background: #0f172a; border-left: 1px solid #1e293b; display: flex; flex-direction: column; }\n" +
                "    .kpi-bar { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 12px; background: #1e293b; }\n" +
                "    .kpi-card { background: #0f172a; padding: 8px; border-radius: 6px; text-align: center; border: 1px solid #334155; }\n" +
                "    .kpi-val { font-size: 1.1rem; font-weight: bold; color: #38bdf8; }\n" +
                "    .kpi-lbl { font-size: 0.65rem; color: #94a3b8; text-transform: uppercase; margin-top: 2px; }\n" +
                "    .tabs { display: flex; border-bottom: 1px solid #334155; background: #0f172a; }\n" +
                "    .tab-btn { flex: 1; padding: 10px; background: none; border: none; color: #94a3b8; cursor: pointer; font-size: 0.8rem; font-weight: 600; }\n" +
                "    .tab-btn.active { color: #38bdf8; border-bottom: 2px solid #38bdf8; background: #1e293b; }\n" +
                "    .tab-content { flex: 1; overflow-y: auto; padding: 10px; }\n" +
                "    .fleet-item { background: #1e293b; border: 1px solid #334155; border-radius: 6px; padding: 8px 12px; margin-bottom: 8px; display: flex; justify-content: space-between; align-items: center; font-size: 0.8rem; cursor: pointer; transition: 0.2s; }\n" +
                "    .fleet-item:hover { border-color: #38bdf8; }\n" +
                "    .badge { font-size: 0.65rem; padding: 2px 6px; border-radius: 4px; font-weight: 600; }\n" +
                "    .badge-auto { background: #1e3a8a; color: #93c5fd; }\n" +
                "    .badge-moto { background: #7c2d12; color: #fdba74; }\n" +
                "    .badge-bici { background: #064e3b; color: #6ee7b7; }\n" +
                "    .badge-averia { background: #7f1d1d; color: #fca5a5; }\n" +
                "    .log-item { font-family: monospace; font-size: 0.72rem; padding: 4px 6px; border-bottom: 1px solid #1e293b; color: #cbd5e1; }\n" +
                "    .footer-controls { background: #0f172a; padding: 12px 20px; border-top: 1px solid #1e293b; display: flex; flex-direction: column; gap: 10px; }\n" +
                "    .slider-row { display: flex; align-items: center; gap: 12px; }\n" +
                "    input[type=range] { flex: 1; accent-color: #38bdf8; cursor: pointer; }\n" +
                "    .btn-row { display: flex; justify-content: space-between; align-items: center; }\n" +
                "    .btn-group { display: flex; gap: 8px; align-items: center; }\n" +
                "    .btn { background: #334155; color: white; border: none; padding: 6px 14px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; cursor: pointer; transition: 0.2s; }\n" +
                "    .btn:hover { background: #475569; }\n" +
                "    .btn-play { background: #2563eb; }\n" +
                "    .btn-play:hover { background: #1d4ed8; }\n" +
                "    .btn-alert { background: #dc2626; }\n" +
                "    .btn-alert:hover { background: #b91c1c; }\n" +
                "    .btn-gold { background: #d97706; }\n" +
                "    .btn-teal { background: #0d9488; }\n" +
                "    .legend-overlay { position: absolute; top: 12px; right: 12px; background: rgba(15, 23, 42, 0.9); border: 1px solid #334155; padding: 10px; border-radius: 6px; font-size: 0.75rem; pointer-events: none; }\n" +
                "    .legend-row { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }\n" +
                "    .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <header>\n" +
                "    <div class=\"logo\">🚚 PAQRAP <span class=\"sub\">Visor de Monitoreo Dinámico de Cuadrilla (Grid 30x30 km)</span></div>\n" +
                "    <div style=\"display: flex; gap: 12px; align-items: center;\">\n" +
                "      <div id=\"lblReloj\" class=\"reloj\">🕒 07:00:00 (t = 0.00h)</div>\n" +
                "      <button class=\"btn\" onclick=\"resetVista()\">🔍 Reset Vista</button>\n" +
                "    </div>\n" +
                "  </header>\n" +
                "  <div class=\"main-container\">\n" +
                "    <div class=\"map-container\">\n" +
                "      <canvas id=\"gridCanvas\" width=\"750\" height=\"750\"></canvas>\n" +
                "      <div class=\"legend-overlay\">\n" +
                "        <div style=\"font-weight: bold; margin-bottom: 6px; color: #f8fafc;\">Leyenda de Simulación</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #eab308;\"></span> Almacén Central (AC)</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #a855f7;\"></span> Almacenes Intermedios (AI)</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #ef4444;\"></span> Calle Bloqueada / Avería</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #10b981;\"></span> Pedido Entregado</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #38bdf8;\"></span> En Tránsito / Entrega</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #f59e0b;\"></span> Pedido Pendiente</div>\n" +
                "        <div class=\"legend-row\"><span class=\"dot\" style=\"background: #3b82f6;\"></span> Auto | Moto | Bici</div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    <div class=\"sidebar\">\n" +
                "      <div class=\"kpi-bar\">\n" +
                "        <div class=\"kpi-card\"><div id=\"kpiEntregas\" class=\"kpi-val\">0 / 35</div><div class=\"kpi-lbl\">Entregas</div></div>\n" +
                "        <div class=\"kpi-card\"><div id=\"kpiFlota\" class=\"kpi-val\">16 / 16</div><div class=\"kpi-lbl\">Flota Activa</div></div>\n" +
                "        <div class=\"kpi-card\"><div id=\"kpiBloqueos\" class=\"kpi-val\">0</div><div class=\"kpi-lbl\">Bloqueos Viales</div></div>\n" +
                "      </div>\n" +
                "      <div class=\"tabs\">\n" +
                "        <button class=\"tab-btn active\" onclick=\"cambiarTab('flota')\">Flota</button>\n" +
                "        <button class=\"tab-btn\" onclick=\"cambiarTab('pedidos')\">Pedidos</button>\n" +
                "        <button class=\"tab-btn\" onclick=\"cambiarTab('eventos')\">Log en Vivo</button>\n" +
                "      </div>\n" +
                "      <div id=\"tabFlota\" class=\"tab-content\"></div>\n" +
                "      <div id=\"tabPedidos\" class=\"tab-content\" style=\"display:none;\"></div>\n" +
                "      <div id=\"tabEventos\" class=\"tab-content\" style=\"display:none;\"></div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <div class=\"footer-controls\">\n" +
                "    <div class=\"slider-row\">\n" +
                "      <span style=\"font-size:0.8rem; color:#94a3b8;\">0.0h</span>\n" +
                "      <input type=\"range\" id=\"timeSlider\" min=\"0\" max=\"800\" value=\"0\" oninput=\"onSliderChange(this.value)\">\n" +
                "      <span id=\"maxTimeLabel\" style=\"font-size:0.8rem; color:#94a3b8;\">8.0h</span>\n" +
                "    </div>\n" +
                "    <div class=\"btn-row\">\n" +
                "      <div class=\"btn-group\">\n" +
                "        <button class=\"btn\" onclick=\"irA(0.0)\">⏮ 0.0h</button>\n" +
                "        <button class=\"btn\" onclick=\"paso(-0.05)\">⏪ -3m</button>\n" +
                "        <button id=\"btnPlay\" class=\"btn btn-play\" onclick=\"togglePlay()\">▶ Reproducir</button>\n" +
                "        <button class=\"btn\" onclick=\"paso(0.05)\">⏩ +3m</button>\n" +
                "        <button class=\"btn btn-alert\" onclick=\"irA(2.5)\">⚡ Bloqueos (2.5h)</button>\n" +
                "        <button class=\"btn btn-gold\" onclick=\"irA(4.0)\">☕ Refrigerio (4.0h)</button>\n" +
                "        <button class=\"btn btn-teal\" onclick=\"irA(8.0)\">🏁 Fin Turno (8.0h)</button>\n" +
                "      </div>\n" +
                "      <div class=\"btn-group\">\n" +
                "        <span style=\"font-size:0.8rem; color:#94a3b8;\">Velocidad:</span>\n" +
                "        <button class=\"btn\" onclick=\"setVelocidad(1)\">1x</button>\n" +
                "        <button class=\"btn\" style=\"background:#2563eb;\" onclick=\"setVelocidad(5)\">5x</button>\n" +
                "        <button class=\"btn\" onclick=\"setVelocidad(10)\">10x</button>\n" +
                "        <button class=\"btn\" onclick=\"setVelocidad(25)\">25x</button>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    let datosGlobales = null;\n" +
                "    let tiempoActual = 0.0;\n" +
                "    let maxTiempo = 8.0;\n" +
                "    let reproduciendo = false;\n" +
                "    let velocidad = 5.0;\n" +
                "    let animReq = null;\n" +
                "    let ultimoTimestamp = null;\n" +
                "    let vehiculoSeleccionado = null;\n" +
                "    let tabActiva = 'flota';\n" +
                "\n" +
                "    // Pan & Zoom\n" +
                "    let zoom = 1.0, panX = 0, panY = 0;\n" +
                "    let isDragging = false, startX = 0, startY = 0;\n" +
                "    const canvas = document.getElementById('gridCanvas');\n" +
                "    const ctx = canvas.getContext('2d');\n" +
                "\n" +
                "    canvas.addEventListener('mousedown', e => { isDragging = true; startX = e.clientX - panX; startY = e.clientY - panY; });\n" +
                "    window.addEventListener('mouseup', () => isDragging = false);\n" +
                "    window.addEventListener('mousemove', e => { if (isDragging) { panX = e.clientX - startX; panY = e.clientY - startY; render(); } });\n" +
                "    canvas.addEventListener('wheel', e => { e.preventDefault(); const factor = e.deltaY < 0 ? 1.15 : 0.85; zoom = Math.max(0.5, Math.min(4.0, zoom * factor)); render(); });\n" +
                "\n" +
                "    function resetVista() { zoom = 1.0; panX = 0; panY = 0; render(); }\n" +
                "\n" +
                "    async function inicializar() {\n" +
                "      try {\n" +
                "        const res = await fetch('/api/todo');\n" +
                "        datosGlobales = await res.json();\n" +
                "        maxTiempo = datosGlobales.mapa.tiempoMaximo || 8.0;\n" +
                "        document.getElementById('timeSlider').max = Math.round(maxTiempo * 100);\n" +
                "        document.getElementById('maxTimeLabel').innerText = maxTiempo.toFixed(1) + 'h';\n" +
                "        actualizar(0.0);\n" +
                "      } catch (err) {\n" +
                "        console.error('Error cargando datos:', err);\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function togglePlay() {\n" +
                "      reproduciendo = !reproduciendo;\n" +
                "      const btn = document.getElementById('btnPlay');\n" +
                "      if (reproduciendo) {\n" +
                "        btn.innerText = '⏸ Pausar';\n" +
                "        btn.style.background = '#dc2626';\n" +
                "        ultimoTimestamp = performance.now();\n" +
                "        animLoop(ultimoTimestamp);\n" +
                "      } else {\n" +
                "        btn.innerText = '▶ Reproducir';\n" +
                "        btn.style.background = '#2563eb';\n" +
                "        if (animReq) cancelAnimationFrame(animReq);\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function animLoop(ts) {\n" +
                "      if (!reproduciendo) return;\n" +
                "      const dtMs = ts - ultimoTimestamp;\n" +
                "      ultimoTimestamp = ts;\n" +
                "      const dtHoras = (dtMs / 1000.0) * velocidad / 60.0;\n" +
                "      let nuevoT = tiempoActual + dtHoras;\n" +
                "      if (nuevoT >= maxTiempo) {\n" +
                "        nuevoT = maxTiempo;\n" +
                "        actualizar(nuevoT);\n" +
                "        togglePlay();\n" +
                "        return;\n" +
                "      }\n" +
                "      actualizar(nuevoT);\n" +
                "      animReq = requestAnimationFrame(animLoop);\n" +
                "    }\n" +
                "\n" +
                "    function setVelocidad(v) { velocidad = v; }\n" +
                "    function irA(t) { actualizar(t); }\n" +
                "    function paso(delta) { actualizar(Math.max(0, Math.min(maxTiempo, tiempoActual + delta))); }\n" +
                "    function onSliderChange(val) { actualizar(val / 100.0); }\n" +
                "\n" +
                "    async function actualizar(t) {\n" +
                "      tiempoActual = t;\n" +
                "      document.getElementById('timeSlider').value = Math.round(t * 100);\n" +
                "      // Pedir snapshot al endpoint ligero\n" +
                "      try {\n" +
                "        const res = await fetch('/api/estado?t=' + t.toFixed(4));\n" +
                "        const est = await res.json();\n" +
                "        document.getElementById('lblReloj').innerText = `🕒 ${est.reloj} (t = ${t.toFixed(2)}h)`;\n" +
                "        document.getElementById('kpiEntregas').innerText = `${est.pedidosEntregados} / ${est.pedidosEntregados + est.pedidosPendientes + est.pedidosEnTransito}`;\n" +
                "        document.getElementById('kpiFlota').innerText = `${est.vehiculos.filter(v => v.estado !== 'AVERIADO').length} / ${est.vehiculos.length}`;\n" +
                "        document.getElementById('kpiBloqueos').innerText = `${est.bloqueos.length}`;\n" +
                "        renderEstado(est);\n" +
                "        actualizarSidebar(est);\n" +
                "      } catch (e) { console.error(e); }\n" +
                "    }\n" +
                "\n" +
                "    let estadoGuardado = null;\n" +
                "    function render() { if (estadoGuardado) renderEstado(estadoGuardado); }\n" +
                "\n" +
                "    function renderEstado(est) {\n" +
                "      estadoGuardado = est;\n" +
                "      const w = canvas.width, h = canvas.height;\n" +
                "      ctx.clearRect(0, 0, w, h);\n" +
                "\n" +
                "      ctx.save();\n" +
                "      ctx.translate(panX, panY);\n" +
                "\n" +
                "      const margin = 45;\n" +
                "      const mapW = w - 2 * margin;\n" +
                "      const mapH = h - 2 * margin;\n" +
                "      const scale = (mapW / 30.0) * zoom;\n" +
                "      const ox = margin, oy = h - margin;\n" +
                "\n" +
                "      function toScreen(x, y) { return { sx: ox + x * scale, sy: oy - y * scale }; }\n" +
                "\n" +
                "      // 1. Rejilla\n" +
                "      ctx.strokeStyle = '#1e293b'; ctx.lineWidth = 1;\n" +
                "      for (let i = 0; i <= 30; i++) {\n" +
                "        const p1 = toScreen(i, 0), p2 = toScreen(i, 30);\n" +
                "        ctx.beginPath(); ctx.moveTo(p1.sx, p1.sy); ctx.lineTo(p2.sx, p2.sy); ctx.stroke();\n" +
                "        if (i % 5 === 0) {\n" +
                "          ctx.fillStyle = '#64748b'; ctx.font = '10px monospace';\n" +
                "          ctx.fillText(i, p1.sx - 4, p1.sy + 14);\n" +
                "        }\n" +
                "      }\n" +
                "      for (let j = 0; j <= 30; j++) {\n" +
                "        const p1 = toScreen(0, j), p2 = toScreen(30, j);\n" +
                "        ctx.beginPath(); ctx.moveTo(p1.sx, p1.sy); ctx.lineTo(p2.sx, p2.sy); ctx.stroke();\n" +
                "        if (j % 5 === 0) {\n" +
                "          ctx.fillStyle = '#64748b'; ctx.font = '10px monospace';\n" +
                "          ctx.fillText(j, p1.sx - 18, p1.sy + 3);\n" +
                "        }\n" +
                "      }\n" +
                "\n" +
                "      // 2. Calles bloqueadas\n" +
                "      if (est.bloqueos) {\n" +
                "        est.bloqueos.forEach(b => {\n" +
                "          const p1 = toScreen(b.x1, b.y1), p2 = toScreen(b.x2, b.y2);\n" +
                "          ctx.strokeStyle = '#ef4444'; ctx.lineWidth = 4; ctx.setLineDash([4, 4]);\n" +
                "          ctx.beginPath(); ctx.moveTo(p1.sx, p1.sy); ctx.lineTo(p2.sx, p2.sy); ctx.stroke();\n" +
                "          ctx.setLineDash([]);\n" +
                "        });\n" +
                "      }\n" +
                "\n" +
                "      // 3. Almacenes\n" +
                "      if (datosGlobales && datosGlobales.almacenes) {\n" +
                "        const ac = datosGlobales.almacenes.central;\n" +
                "        const pAc = toScreen(ac.x, ac.y);\n" +
                "        ctx.fillStyle = '#eab308'; ctx.fillRect(pAc.sx - 8, pAc.sy - 8, 16, 16);\n" +
                "        ctx.strokeStyle = '#ffffff'; ctx.strokeRect(pAc.sx - 8, pAc.sy - 8, 16, 16);\n" +
                "        ctx.fillStyle = '#f8fafc'; ctx.font = 'bold 9px sans-serif'; ctx.fillText('AC (15,15)', pAc.sx - 20, pAc.sy - 12);\n" +
                "\n" +
                "        datosGlobales.almacenes.intermedios.forEach(ai => {\n" +
                "          const pAi = toScreen(ai.x, ai.y);\n" +
                "          ctx.fillStyle = '#a855f7'; ctx.fillRect(pAi.sx - 6, pAi.sy - 6, 12, 12);\n" +
                "          ctx.strokeStyle = '#ffffff'; ctx.strokeRect(pAi.sx - 6, pAi.sy - 6, 12, 12);\n" +
                "          ctx.fillStyle = '#cbd5e1'; ctx.font = '9px sans-serif'; ctx.fillText(ai.id, pAi.sx - 15, pAi.sy - 10);\n" +
                "        });\n" +
                "      }\n" +
                "      if (est.pedidos) {\n" +
                "        est.pedidos.forEach(p => {\n" +
                "          const pt = toScreen(p.x, p.y);\n" +
                "          let col = '#f59e0b';\n" +
                "          if (p.estado === 'ENTREGADO') col = '#10b981';\n" +
                "          else if (p.estado === 'EN_TRANSITO' || p.estado === 'EN_ENTREGA') col = '#38bdf8';\n" +
                "          else if (p.estado === 'TARDE') col = '#ef4444';\n" +
                "          ctx.fillStyle = col;\n" +
                "          ctx.beginPath();\n" +
                "          ctx.arc(pt.sx, pt.sy, 5, 0, Math.PI * 2);\n" +
                "          ctx.fill();\n" +
                "          ctx.strokeStyle = '#ffffff';\n" +
                "          ctx.lineWidth = 1;\n" +
                "          ctx.stroke();\n" +
                "          ctx.fillStyle = '#cbd5e1';\n" +
                "          ctx.font = '8px monospace';\n" +
                "          ctx.fillText(p.destinoId, pt.sx + 7, pt.sy + 3);\n" +
                "        });\n" +
                "      }\n" +
                "\n" +
                "      est.vehiculos.forEach(v => {\n" +
                "        const pv = toScreen(v.x, v.y);\n" +
                "        let col = '#3b82f6';\n" +
                "        if (v.tipo === 'Moto') col = '#f97316';\n" +
                "        else if (v.tipo === 'Bicicleta' || v.tipo === 'Bici') col = '#10b981';\n" +
                "        if (v.estado === 'AVERIADO') col = '#ef4444';\n" +
                "\n" +
                "        ctx.fillStyle = col; ctx.beginPath(); ctx.arc(pv.sx, pv.sy, 6.5, 0, Math.PI * 2); ctx.fill();\n" +
                "        ctx.strokeStyle = '#ffffff'; ctx.lineWidth = 1.5; ctx.stroke();\n" +
                "\n" +
                "        ctx.fillStyle = '#ffffff'; ctx.font = 'bold 8px sans-serif';\n" +
                "        ctx.fillText(v.id, pv.sx - 10, pv.sy + 15);\n" +
                "      });\n" +
                "\n" +
                "      ctx.restore();\n" +
                "    }\n" +
                "\n" +
                "    function cambiarTab(tab) {\n" +
                "      tabActiva = tab;\n" +
                "      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));\n" +
                "      document.querySelectorAll('.tab-content').forEach(c => c.style.display = 'none');\n" +
                "      if (event && event.target) event.target.classList.add('active');\n" +
                "      document.getElementById('tab' + tab.charAt(0).toUpperCase() + tab.slice(1)).style.display = 'block';\n" +
                "    }\n" +
                "\n" +
                "    function actualizarSidebar(est) {\n" +
                "      const cFlota = document.getElementById('tabFlota');\n" +
                "      cFlota.innerHTML = est.vehiculos.map(v => `\n" +
                "        <div class=\"fleet-item\">\n" +
                "          <div>\n" +
                "            <strong style=\"color:#f8fafc;\">${v.id}</strong> <span class=\"badge badge-${v.tipo.toLowerCase()}\">${v.tipo}</span>\n" +
                "            <div style=\"font-size:0.7rem; color:#94a3b8; margin-top:3px;\">${v.detalle}</div>\n" +
                "          </div>\n" +
                "          <div style=\"text-align:right;\">\n" +
                "            <span style=\"color:#38bdf8;\">(${v.x.toFixed(1)}, ${v.y.toFixed(1)})</span>\n" +
                "            <div style=\"font-size:0.7rem; color:#facc15;\">Carga: ${v.cargaActual}/${v.capacidadTotal}</div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      `).join('');\n" +
                "\n" +
                "      const cPed = document.getElementById('tabPedidos');\n" +
                "      if (est.pedidos) {\n" +
                "        cPed.innerHTML = est.pedidos.map(p => `\n" +
                "          <div class=\"fleet-item\">\n" +
                "            <div>\n" +
                "              <strong style=\"color:#f8fafc;\">${p.id}</strong> <span style=\"font-size:0.7rem; color:#94a3b8;\">(${p.destinoId} en ${p.x},${p.y})</span>\n" +
                "              <div style=\"font-size:0.7rem; color:#facc15;\">Demanda: ${p.cantidad} paq. | Plazo: ${p.plazo}h</div>\n" +
                "            </div>\n" +
                "            <div style=\"text-align:right;\">\n" +
                "              <span class=\"badge badge-${p.estado === 'ENTREGADO' ? 'bici' : (p.estado === 'TARDE' ? 'averia' : 'auto')}\">${p.estado}</span>\n" +
                "              ${p.relojEntrega ? `<div style=\"font-size:0.65rem; color:#10b981;\">Entregado: ${p.relojEntrega}</div>` : ''}\n" +
                "            </div>\n" +
                "          </div>\n" +
                "        `).join('');\n" +
                "      }\n" +
                "\n" +
                "      const cEv = document.getElementById('tabEventos');\n" +
                "      if (est.eventos) {\n" +
                "        cEv.innerHTML = est.eventos.map(e => `\n" +
                "          <div class=\"log-item\">\n" +
                "            <span style=\"color:#facc15;\">[${e.reloj}]</span> <span style=\"color:#38bdf8;\">[${e.tipo}]</span> ${e.descripcion}\n" +
                "          </div>\n" +
                "        `).join('');\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    window.onload = inicializar;\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>\n";
    }
}
