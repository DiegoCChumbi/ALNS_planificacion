package com.paqrap;

import com.paqrap.configuracion.*;
import com.paqrap.entrada.*;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.*;
import com.paqrap.simulador.*;
import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("      PaqRap - Sistema ALNS Parametrizado mediante Archivo JSON                ");
        System.out.println("===============================================================================\n");

        EntradaCli entradaCli = EntradaCli.parsear(args);
        if (entradaCli != null) {
            ejecutarConEntradas(entradaCli);
            return;
        }

        if (args.length > 0 && ("--escenarios".equalsIgnoreCase(args[0]) || "-e".equalsIgnoreCase(args[0])
                || "--comparativa".equalsIgnoreCase(args[0]) || "-c".equalsIgnoreCase(args[0]))) {
            try {
                ExperimentoComparativo.main(args.length > 1 ? new String[]{args[1]} : new String[]{"config/configuracion.json"});
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // 1. Cargar archivo de configuración JSON (por argumento CLI o por defecto)
        String rutaConfig = (args.length > 0) ? args[0] : "config/configuracion.json";
        File archivoConfig = new File(rutaConfig);
        ConfiguracionSistema config;

        try {
            if (archivoConfig.exists()) {
                System.out.println(">> Cargando configuración desde archivo JSON: " + archivoConfig.getPath());
                config = ConfiguracionSistema.cargarDesdeArchivo(archivoConfig);
            } else {
                System.out.println(">> Archivo " + rutaConfig + " no encontrado. Inicializando configuración por defecto.");
                config = new ConfiguracionSistema();
            }
        } catch (Exception e) {
            System.err.println("Error al parsear el JSON de configuración: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // 2. Construir Entorno, Mapa y Contexto inicial desde los parámetros JSON
        MapaCuadricula mapa = config.construirMapa();
        ConfiguracionEntorno ent = config.getEntorno();
        ConfiguracionOperacion op = config.getOperacion();

        System.out.println("\nParámetros Cargados del Sistema:");
        System.out.printf(" • Red Cuadrícula   : %dx%d km (%.1f km por cuadra, doble sentido: %b)\n",
                ent.getAnchoMapa(), ent.getAltoMapa(), ent.getKmPorCuadra(), ent.isCallesDobleSentido());
        System.out.printf(" • Almacenes        : 1 Central en (%d,%d) + %d Intermedios\n",
                config.getAlmacenCentral().getX(), config.getAlmacenCentral().getY(), config.getAlmacenesIntermedios().size());
        System.out.printf(" • Tipos de Vehículo: %d categorías registradas (", config.getTiposVehiculo().size());
        for (int i = 0; i < config.getTiposVehiculo().size(); i++) {
            TipoVehiculo tv = config.getTiposVehiculo().get(i);
            System.out.printf("%s: cap=%d, vel=%.0fkm/h, cost=S/%.2f/km%s",
                    tv.getNombre(), tv.getCapacidad(), tv.getVelocidadKmH(), tv.getCostoPorKm(),
                    (i < config.getTiposVehiculo().size() - 1) ? " | " : "");
        }
        System.out.println(")");
        System.out.printf(" • Flota Activa     : %d unidades asignadas a almacenes\n", config.getFlota().size());
        System.out.printf(" • Turno Laboral    : %.1fh (inicio: %02d:00, atención cliente: %.1fh, refrigerio: %.1fh)\n",
                op.getDuracionTurnoHoras(), (int) op.getHoraInicioTurno(), op.getTiempoServicioClienteHoras(), op.getDuracionRefrigerioHoras());
        System.out.printf(" • ALNS Parámetros  : maxIter=%d, maxSinMejora=%d, reac=%.2f, SPP=%d, SA_Temp=%.1f\n",
                config.getAlns().getMaxIteraciones(), config.getAlns().getMaxSinMejora(), config.getAlns().getFactorReaccion(),
                config.getAlns().getIntervaloSPP(), config.getAlns().getTemperaturaAceptacionSA());
        System.out.printf(" • Cartera Pedidos  : %d pedidos iniciales cargados\n", config.getPedidosIniciales().size());
        System.out.println("-------------------------------------------------------------------------------\n");

        // 3. Inicializar Gestor de Log y Motor de Simulación
        GestorLogSimulacion gestorLog = new GestorLogSimulacion(
                config.getSimulacion().getDirectorioLogs(),
                config.getSimulacion().isImprimirEnConsola()
        );
        MotorSimulacion motorSimulacion = new MotorSimulacion(mapa, gestorLog, config);

        gestorLog.registrarEvento(new EventoSimulacion(
                0.0,
                op.getHoraInicioTurno(),
                TipoEvento.INICIO_SIMULACION,
                null,
                null,
                config.getAlmacenCentral().getX(),
                config.getAlmacenCentral().getY(),
                String.format("Inicio de Operaciones PaqRap (Turno %02d:00:00)", (int) op.getHoraInicioTurno()),
                String.format("%d Almacenes | %d Vehículos | %d Pedidos",
                        1 + config.getAlmacenesIntermedios().size(), config.getFlota().size(), config.getPedidosIniciales().size())
        ));

        // 4. Planificación Inicial ALNS
        System.out.println("-------------------------------------------------------------------------------");
        System.out.printf("FASE 1: PLANIFICACIÓN INICIAL ALNS (t = 0.0h / %02d:00:00)\n", (int) op.getHoraInicioTurno());
        System.out.println("-------------------------------------------------------------------------------");

        ContextoProblema contextoInicial = config.construirContextoInicial(mapa);
        SolucionadorALNS solucionador = new SolucionadorALNS(config.getAlns());

        long tInicio = System.currentTimeMillis();
        Solucion solucionInicial = solucionador.resolver(contextoInicial);
        long tFin = System.currentTimeMillis();

        imprimirResumenSolucion(solucionInicial, (tFin - tInicio), op.getPenalizacionPedidoNoAsignado());

        // Encontrar tiempo de primera disrupción
        double tiempoDisrupcion = 2.5;
        List<Map<String, Object>> disrupciones = config.getDisrupciones();
        if (!disrupciones.isEmpty()) {
            tiempoDisrupcion = LectorJson.getDouble(disrupciones.get(0), "tiempoHoras", 2.5);
        }

        System.out.printf("\n>> [Simulador] Ejecutando simulación de movimientos (0.0h a %.1fh)...\n", tiempoDisrupcion);
        motorSimulacion.simularRutas(solucionInicial, 0.0, tiempoDisrupcion);

        // 5. Procesamiento de Disrupciones Parametrizadas desde el JSON
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.printf("FASE 2: PROCESANDO DISRUPCIONES EN TIEMPO REAL (t = %.1fh)\n", tiempoDisrupcion);
        System.out.println("-------------------------------------------------------------------------------");

        Set<String> vehiculosAveriados = new HashSet<>();
        List<Pedido> nuevosPedidosExpres = new ArrayList<>();

        for (Map<String, Object> dis : disrupciones) {
            double tDis = LectorJson.getDouble(dis, "tiempoHoras", tiempoDisrupcion);
            String tipoDis = LectorJson.getString(dis, "tipo", "");

            if ("BLOQUEO_CALLE".equalsIgnoreCase(tipoDis)) {
                int x1 = LectorJson.getInt(dis, "x1", 0);
                int y1 = LectorJson.getInt(dis, "y1", 0);
                int x2 = LectorJson.getInt(dis, "x2", 0);
                int y2 = LectorJson.getInt(dis, "y2", 0);
                motorSimulacion.registrarBloqueoCalle(tDis, x1, y1, x2, y2);
                System.out.printf(">> ALERTA VIAL: Calle (%d,%d) <-> (%d,%d) BLOQUEADA.\n", x1, y1, x2, y2);
            } else if ("AVERIA_VEHICULO".equalsIgnoreCase(tipoDis)) {
                String vid = LectorJson.getString(dis, "vehiculoId", "");
                int x = LectorJson.getInt(dis, "x", 0);
                int y = LectorJson.getInt(dis, "y", 0);
                String motivo = LectorJson.getString(dis, "motivo", "Falla mecánica");
                vehiculosAveriados.add(vid);
                motorSimulacion.registrarAveriaVehiculo(tDis, vid, x, y, motivo);
                System.out.printf(">> EMERGENCIA FLOTA: Vehículo %s AVERIADO en (%d,%d). Motivo: %s\n", vid, x, y, motivo);
            } else if ("NUEVO_PEDIDO".equalsIgnoreCase(tipoDis)) {
                String id = LectorJson.getString(dis, "id", "Ped-Expres");
                String destId = LectorJson.getString(dis, "destinoId", "C-Expres");
                int x = LectorJson.getInt(dis, "x", 0);
                int y = LectorJson.getInt(dis, "y", 0);
                int cant = LectorJson.getInt(dis, "cantidad", 1);
                double plazo = LectorJson.getDouble(dis, "plazoHoras", 4.0);
                Pedido pExp = new Pedido(id, new NodoCuadricula(destId, x, y, TipoNodo.CLIENTE), cant, tDis, plazo);
                nuevosPedidosExpres.add(pExp);
                motorSimulacion.registrarNuevoPedido(tDis, pExp);
                System.out.printf(">> NUEVO PEDIDO: %s en (%d,%d) | Demanda: %d | Plazo: %.1fh (Entrega máx: %.1fh)\n",
                        id, x, y, cant, plazo, pExp.getTiempoMaximoEntrega());
            }
        }

        // 6. Replanificación Dinámica ALNS
        System.out.println("\n-------------------------------------------------------------------------------");
        System.out.printf("FASE 3: REPLANIFICACIÓN ALNS DINÁMICA ANTE CONTINGENCIAS (t = %.1fh)\n", tiempoDisrupcion);
        System.out.println("-------------------------------------------------------------------------------");

        ContextoProblema contextoReplan = new ContextoProblema(tiempoDisrupcion, mapa, op);
        contextoReplan.agregarAlmacen(config.getAlmacenCentral());
        for (NodoCuadricula ai : config.getAlmacenesIntermedios()) {
            contextoReplan.agregarAlmacen(ai);
        }

        // Flota activa actualizada (excluyendo vehículos averiados)
        Map<String, EstadoVehiculo> estados = motorSimulacion.getEstadosVehiculos();
        for (EstadoVehiculo evOriginal : config.getFlota()) {
            String vid = evOriginal.getId();
            if (vehiculosAveriados.contains(vid)) {
                continue;
            }
            EstadoVehiculo evActual = estados.get(vid);
            if (evActual != null) {
                contextoReplan.agregarVehiculo(evActual.copiar());
            } else {
                contextoReplan.agregarVehiculo(evOriginal.copiar());
            }
        }

        // Pedidos pendientes no completados hasta tiempoDisrupcion + nuevos exprés
        Set<String> completados = motorSimulacion.getPedidosCompletados();
        int pendientesContador = 0;
        for (Pedido p : config.getPedidosIniciales()) {
            if (!completados.contains(p.getId())) {
                contextoReplan.agregarPedido(p);
                pendientesContador++;
            }
        }
        for (Pedido pExp : nuevosPedidosExpres) {
            contextoReplan.agregarPedido(pExp);
            pendientesContador++;
        }

        System.out.printf(">> Estado del Sistema a las %s: %d entregas completadas, %d pedidos por atender.\n",
                EventoSimulacion.formatearReloj(tiempoDisrupcion, op.getHoraInicioTurno()), completados.size(), pendientesContador);
        System.out.printf(">> Flota disponible: %d vehículos operativos (excluidos: %s).\n",
                contextoReplan.getVehiculosActivos().size(), vehiculosAveriados);

        tInicio = System.currentTimeMillis();
        Solucion solucionReplan = solucionador.resolver(contextoReplan);
        tFin = System.currentTimeMillis();

        imprimirResumenSolucion(solucionReplan, (tFin - tInicio), op.getPenalizacionPedidoNoAsignado());

        // 7. Simular ejecución post-replanificación hasta completar pedidos
        System.out.println("\n>> [Simulador] Ejecutando simulación tras replanificación...");
        motorSimulacion.simularRutas(solucionReplan, tiempoDisrupcion, 18.0);

        motorSimulacion.registrarFinSimulacion(18.0, String.format(Locale.US,
                "Costo Final Replanificado: S/ %.2f | Pedidos No Asignados: %d",
                solucionReplan.calcularCostoTotal(op.getPenalizacionPedidoNoAsignado()),
                solucionReplan.getPedidosNoAsignados().size()));

        System.out.println("\n===============================================================================");
        System.out.println("   ¡Simulación y Replanificación Parametrizada Concluida con Éxito!           ");
        System.out.println("===============================================================================");
        System.out.println(">> Estadísticas y Registro de Auditoría:");
        System.out.println("   • Archivo de Configuración JSON: " + archivoConfig.getPath());
        System.out.println("   • Archivo Log de Texto Plano   : " + gestorLog.getArchivoLogTexto().getPath());
        System.out.println("   • Archivo Log en JSON          : " + gestorLog.getArchivoLogJson().getPath());
        System.out.println("   • Total de Eventos Registrados : " + gestorLog.getEventos().size());
        System.out.println("   • Evaluación de Escenarios ALNS (5 Días y Colapso Logístico):");
        System.out.println("     java -cp bin com.paqrap.Main --escenarios");
        System.out.println("===============================================================================");
    }

    private static void imprimirResumenSolucion(Solucion solucion, long tiempoMs, double penalizacionNoAsignado) {
        System.out.printf("Tiempo de Cómputo ALNS : %d ms\n", tiempoMs);
        System.out.printf("Costo Total de Rutas   : S/ %.2f\n", solucion.calcularCostoTotal(penalizacionNoAsignado));
        System.out.printf("Pedidos No Asignados   : %d\n", solucion.getPedidosNoAsignados().size());
        if (!solucion.getPedidosNoAsignados().isEmpty()) {
            System.out.println("ALERTA: Pedidos no asignados: " + solucion.getPedidosNoAsignados());
        }

        int vehiculosConRuta = 0;
        int totalAsignados = 0;
        double distanciaTotal = 0.0;

        for (Ruta r : solucion.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) {
                vehiculosConRuta++;
                totalAsignados += r.getPedidosAsignados().size();
                distanciaTotal += r.getDistanciaTotal();
            }

        }
        System.out.printf("Vehículos con Rutas    : %d de %d\n", vehiculosConRuta, solucion.getRutas().size());
        System.out.printf("Total Pedidos Atendidos: %d\n", totalAsignados);
        System.out.printf("Distancia Total de Red : %.1f km\n", distanciaTotal);

        System.out.println("\nDetalle de Rutas Asignadas:");
        for (Ruta r : solucion.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) {
                System.out.printf("  • [%s (%s)] %d pedidos | Dist: %5.1f km | Costo: S/ %6.2f\n",
                        r.getVehiculo().getId(), r.getVehiculo().getTipo().getNombre(),
                        r.getPedidosAsignados().size(), r.getDistanciaTotal(), r.getCostoTotal());
                System.out.print("    Paradas: ");
                for (int i = 0; i < r.getNodosCamino().size(); i++) {
                    System.out.printf("%s(t=%.2fh) -> ", r.getNodosCamino().get(i).getId(), r.getTiemposLlegada().get(i));
                }
                System.out.println("FIN");
            }
        }
    }

    private static void ejecutarConEntradas(EntradaCli cli) {
        try {
            File archivoConfig = new File(cli.configuracion);
            ConfiguracionSistema config = archivoConfig.isFile()
                    ? ConfiguracionSistema.cargarDesdeArchivo(archivoConfig)
                    : new ConfiguracionSistema();
            RegistroEntradas entradas = LectorEntradas.cargar(cli.ventas, cli.bloqueos);
            List<Pedido> pedidos = new ArrayList<>(entradas.getPedidos());
            if (cli.maxPedidos > 0 && pedidos.size() > cli.maxPedidos) {
                pedidos = new ArrayList<>(pedidos.subList(0, cli.maxPedidos));
            }
            config.getPedidosIniciales().clear();
            config.getPedidosIniciales().addAll(pedidos);

            MapaCuadricula mapa = config.construirMapa(entradas);
            for (BloqueoTemporal bloqueo : entradas.getBloqueos()) mapa.agregarBloqueoTemporal(bloqueo);

            System.out.printf("Entradas externas: %d archivo(s) de ventas, %d de bloqueos | "
                            + "%d pedidos, %d bloqueos temporales%n",
                    entradas.getArchivosVentasLeidos(), entradas.getArchivosBloqueosLeidos(),
                    pedidos.size(), entradas.getCantidadBloqueos());
            System.out.printf("Mapa dimensionado automáticamente: %dx%d (máximos de entradas: %d,%d)%n",
                    mapa.getAncho(), mapa.getAlto(), entradas.getMaximaX(), entradas.getMaximaY());

            if (cli.maxIteraciones > 0) {
                config.getAlns().setMaxIteraciones(cli.maxIteraciones);
                config.getAlns().setMaxSinMejora(Math.min(config.getAlns().getMaxSinMejora(), cli.maxIteraciones));
            }
            SolucionadorALNS solucionador = new SolucionadorALNS(config.getAlns());
            GestorLogSimulacion gestorLog = new GestorLogSimulacion(
                    config.getSimulacion().getDirectorioLogs(),
                    config.getSimulacion().isImprimirEnConsola());
            MotorSimulacion motor = new MotorSimulacion(mapa, gestorLog, config);
            double hasta = cli.hastaHoras;
            if (hasta <= 0) {
                hasta = pedidos.stream().mapToDouble(Pedido::getTiempoLiberacion)
                        .max().orElse(0.0) + config.getOperacion().getDuracionTurnoHoras();
            }
            double duracionTurno = config.getOperacion().getDuracionTurnoHoras();
            Set<String> completados = motor.getPedidosCompletados();
            long inicioTotal = System.currentTimeMillis();
            int turnosProcesados = 0;
            int totalPedidosCandidatos = 0;
            int totalPlanificados = 0;

            for (double inicioTurno = 0.0; inicioTurno < hasta; inicioTurno += duracionTurno) {
                double finTurno = Math.min(inicioTurno + duracionTurno, hasta);
                ContextoProblema contexto = new ContextoProblema(inicioTurno, mapa, config.getOperacion());
                contexto.agregarAlmacen(config.getAlmacenCentral());
                for (NodoCuadricula almacen : config.getAlmacenesIntermedios()) {
                    contexto.agregarAlmacen(almacen);
                }
                for (EstadoVehiculo original : config.getFlota()) {
                    EstadoVehiculo vehiculo = new EstadoVehiculo(
                            original.getId(), original.getTipo(), original.getAlmacenBase(),
                            original.getAlmacenBase(), inicioTurno, inicioTurno);
                    contexto.agregarVehiculo(vehiculo);
                }
                for (Pedido pedido : pedidos) {
                    if (!completados.contains(pedido.getId())
                            && pedido.getTiempoLiberacion() < finTurno
                            && pedido.getTiempoMaximoEntrega() >= inicioTurno) {
                        contexto.agregarPedido(pedido);
                    }
                }
                if (contexto.getPedidosPendientes().isEmpty()) {
                    continue;
                }

                turnosProcesados++;
                totalPedidosCandidatos += contexto.getPedidosPendientes().size();
                System.out.printf(">> Iniciando turno t=%.1f-%.1fh con %d pedidos candidatos%n",
                        inicioTurno, finTurno, contexto.getPedidosPendientes().size());
                long inicioTurnoMs = System.currentTimeMillis();
                Solucion solucion = solucionador.resolver(contexto);
                totalPlanificados += solucion.getRutas().stream()
                        .mapToInt(ruta -> ruta.getPedidosAsignados().size()).sum();
                System.out.printf("Turno t=%.1f-%.1fh: %d pedidos candidatos, %d asignados, %d no asignados%n",
                        inicioTurno, finTurno, contexto.getPedidosPendientes().size(),
                        totalPedidosEnRutas(solucion), solucion.getPedidosNoAsignados().size());
                motor.simularRutas(solucion, inicioTurno, finTurno);
                System.out.printf("  ALNS: %d ms | completados acumulados: %d%n",
                        System.currentTimeMillis() - inicioTurnoMs, completados.size());
            }
            long finTotal = System.currentTimeMillis();
            motor.registrarFinSimulacion(hasta, String.format(Locale.US,
                    "Simulación con entradas externas | Pedidos: %d | Completados: %d | Bloqueos: %d",
                    pedidos.size(), completados.size(), entradas.getCantidadBloqueos()));
            System.out.printf(Locale.US,
                    "Simulación ejecutada hasta t=%.2fh; eventos: %d; completados: %d/%d; "
                            + "tiempo total ALNS: %d ms%n",
                    hasta, gestorLog.getEventos().size(), completados.size(), pedidos.size(),
                    finTotal - inicioTotal);
            System.out.println("\n==================== RESUMEN DE EJECUCIÓN ====================");
            System.out.printf(Locale.US, "Horizonte simulado       : %.2f h%n", hasta);
            System.out.printf("Turnos procesados         : %d%n", turnosProcesados);
            System.out.printf("Pedidos cargados          : %d%n", pedidos.size());
            System.out.printf("Pedidos candidatos        : %d%n", totalPedidosCandidatos);
            System.out.printf("Asignaciones generadas   : %d%n", totalPlanificados);
            System.out.printf("Pedidos completados       : %d%n", completados.size());
            System.out.printf("Pedidos pendientes        : %d%n", Math.max(0, pedidos.size() - completados.size()));
            System.out.printf("Bloqueos temporales       : %d%n", entradas.getCantidadBloqueos());
            System.out.printf(Locale.US, "Cumplimiento completado  : %.1f%%%n",
                    pedidos.isEmpty() ? 100.0 : completados.size() * 100.0 / pedidos.size());
            System.out.printf("Pedidos no asignados únicos: %d%n",
                    Math.max(0, pedidos.size() - completados.size()));
            System.out.printf("Tiempo total de ejecución : %d ms%n", finTotal - inicioTotal);
            System.out.println("Estado                    : EJECUCIÓN FINALIZADA");
            System.out.println("===============================================================");
        } catch (Exception e) {
            System.err.println("Error cargando entradas externas: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static int totalPedidosEnRutas(Solucion solucion) {
        return solucion.getRutas().stream()
                .mapToInt(ruta -> ruta.getPedidosAsignados().size())
                .sum();
    }

    private static final class EntradaCli {
        private Path ventas;
        private Path bloqueos;
        private String configuracion = "config/configuracion.json";
        private int maxPedidos;
        private int maxIteraciones;
        private double hastaHoras;

        private static EntradaCli parsear(String[] args) {
            boolean encontrado = false;
            EntradaCli cli = new EntradaCli();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ("--inputs".equals(arg) || "--ventas".equals(arg)) {
                    cli.ventas = Path.of(valor(args, ++i, arg)); encontrado = true;
                } else if ("--bloqueos".equals(arg)) {
                    cli.bloqueos = Path.of(valor(args, ++i, arg)); encontrado = true;
                } else if ("--config".equals(arg)) {
                    cli.configuracion = valor(args, ++i, arg);
                } else if ("--max-pedidos".equals(arg)) {
                    cli.maxPedidos = Integer.parseInt(valor(args, ++i, arg));
                    if (cli.maxPedidos < 1) throw new IllegalArgumentException("--max-pedidos debe ser positivo");
                } else if ("--max-iteraciones".equals(arg)) {
                    cli.maxIteraciones = Integer.parseInt(valor(args, ++i, arg));
                    if (cli.maxIteraciones < 1) throw new IllegalArgumentException("--max-iteraciones debe ser positivo");
                } else if ("--hasta-horas".equals(arg)) {
                    cli.hastaHoras = Double.parseDouble(valor(args, ++i, arg));
                    if (cli.hastaHoras <= 0) throw new IllegalArgumentException("--hasta-horas debe ser positivo");
                } else if (arg.startsWith("--")) {
                    throw new IllegalArgumentException("Opción desconocida: " + arg);
                } else if (i == 0) {
                    cli.configuracion = arg;
                }
            }
            return encontrado ? cli : null;
        }

        private static String valor(String[] args, int indice, String opcion) {
            if (indice >= args.length || args[indice].startsWith("--")) {
                throw new IllegalArgumentException("Falta valor para " + opcion);
            }
            return args[indice];
        }
    }
}
