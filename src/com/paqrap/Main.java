package com.paqrap;

import com.paqrap.configuracion.*;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.*;
import com.paqrap.simulador.*;
import java.io.File;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("===============================================================================");
        System.out.println("      PaqRap - Sistema ALNS Parametrizado mediante Archivo JSON                ");
        System.out.println("===============================================================================\n");

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
}
