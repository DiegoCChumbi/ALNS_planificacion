package com.paqrap;

import com.paqrap.modelo.*;
import com.paqrap.solucionador.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        System.out.println("=========================================================");
        System.out.println("   PaqRap - Motor ALNS de Enrutamiento y Replanificación ");
        System.out.println("=========================================================\n");

        // 1. Configurar Mapa en Cuadrícula (20x20)
        MapaCuadricula mapa = new MapaCuadricula(20, 20);

        // Almacenes
        NodoCuadricula almacenCentral = new NodoCuadricula("AlmacenCentral", 10, 10, TipoNodo.ALMACEN_CENTRAL);
        NodoCuadricula almacenInter1 = new NodoCuadricula("AlmacenInter1", 5, 5, TipoNodo.ALMACEN_INTERMEDIO);
        NodoCuadricula almacenInter2 = new NodoCuadricula("AlmacenInter2", 15, 15, TipoNodo.ALMACEN_INTERMEDIO);

        // Flotas de Vehículos
        List<EstadoVehiculo> flotaInicial = new ArrayList<>();
        flotaInicial.add(new EstadoVehiculo("Auto-1", TipoVehiculo.AUTO, almacenCentral, 0.0, 7.0));
        flotaInicial.add(new EstadoVehiculo("Auto-2", TipoVehiculo.AUTO, almacenCentral, 0.0, 7.0));
        flotaInicial.add(new EstadoVehiculo("Moto-1", TipoVehiculo.MOTO, almacenCentral, 0.0, 7.0));
        flotaInicial.add(new EstadoVehiculo("Moto-2", TipoVehiculo.MOTO, almacenCentral, 0.0, 7.0));
        flotaInicial.add(new EstadoVehiculo("Bici-1", TipoVehiculo.BICI, almacenCentral, 0.0, 7.0));
        flotaInicial.add(new EstadoVehiculo("Bici-2", TipoVehiculo.BICI, almacenCentral, 0.0, 7.0));

        // Pedidos Iniciales de Clientes (t=0)
        List<Pedido> pedidosIniciales = new ArrayList<>();
        pedidosIniciales.add(new Pedido("Ped-01", new NodoCuadricula("C1", 12, 12, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidosIniciales.add(new Pedido("Ped-02", new NodoCuadricula("C2", 8, 14, TipoNodo.CLIENTE), 6, 0.0, 8.0));
        pedidosIniciales.add(new Pedido("Ped-03", new NodoCuadricula("C3", 3, 7, TipoNodo.CLIENTE), 2, 0.0, 12.0));
        pedidosIniciales.add(new Pedido("Ped-04", new NodoCuadricula("C4", 16, 8, TipoNodo.CLIENTE), 8, 0.0, 18.0));
        pedidosIniciales.add(new Pedido("Ped-05", new NodoCuadricula("C5", 14, 4, TipoNodo.CLIENTE), 3, 0.0, 36.0));
        pedidosIniciales.add(new Pedido("Ped-06", new NodoCuadricula("C6", 6, 18, TipoNodo.CLIENTE), 5, 0.0, 8.0));
        pedidosIniciales.add(new Pedido("Ped-07", new NodoCuadricula("C7", 18, 12, TipoNodo.CLIENTE), 7, 0.0, 12.0));
        pedidosIniciales.add(new Pedido("Ped-08", new NodoCuadricula("C8", 2, 2, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidosIniciales.add(new Pedido("Ped-09", new NodoCuadricula("C9", 9, 3, TipoNodo.CLIENTE), 1, 0.0, 18.0));
        pedidosIniciales.add(new Pedido("Ped-10", new NodoCuadricula("C10", 11, 17, TipoNodo.CLIENTE), 6, 0.0, 36.0));

        // Construir Contexto del Problema Inicial
        ContextoProblema contextoInicial = new ContextoProblema(0.0, mapa);
        contextoInicial.agregarAlmacen(almacenCentral);
        contextoInicial.agregarAlmacen(almacenInter1);
        contextoInicial.agregarAlmacen(almacenInter2);

        for (EstadoVehiculo ev : flotaInicial) contextoInicial.agregarVehiculo(ev);
        for (Pedido p : pedidosIniciales) contextoInicial.agregarPedido(p);

        // Configuración del Solucionador ALNS
        ConfiguracionALNS configuracion = new ConfiguracionALNS();
        configuracion.setMaxIteraciones(300);
        configuracion.setMaxSinMejora(80);
        configuracion.setIntervaloActualizacion(15);
        configuracion.setIntervaloSPP(25);

        SolucionadorALNS solucionador = new SolucionadorALNS(configuracion);

        System.out.println("---------------------------------------------------------");
        System.out.println("1. EJECUTANDO PLANIFICACIÓN INICIAL ALNS (t = 0.0h)");
        System.out.println("---------------------------------------------------------");

        long tiempoInicio = System.currentTimeMillis();
        Solucion solucionInicial = solucionador.resolver(contextoInicial);
        long tiempoFin = System.currentTimeMillis();

        imprimirSolucion(solucionInicial, (tiempoFin - tiempoInicio));

        // ---------------------------------------------------------
        // 2. ESCENARIO DE REPLANIFICACIÓN DINÁMICA (t = 2.0h)
        // ---------------------------------------------------------
        System.out.println("\n---------------------------------------------------------");
        System.out.println("2. EJECUTANDO ESCENARIO DE REPLANIFICACIÓN DINÁMICA (t = 2.0h)");
        System.out.println("---------------------------------------------------------");
        System.out.println(">> Incidencia Detectada: Tramo de calle (10,10) a (11,10) está BLOQUEADO.");
        System.out.println(">> Nuevos Pedidos Exprés Llegados: Ped-11 (plazo 4h), Ped-12 (plazo 8h).");

        // Bloquear tramo de calle en la cuadrícula
        mapa.bloquearArista(10, 10, 11, 10);

        // Posiciones actualizadas de los vehículos en t = 2.0h
        List<EstadoVehiculo> flotaActualizada = new ArrayList<>();
        flotaActualizada.add(new EstadoVehiculo("Auto-1", TipoVehiculo.AUTO, new NodoCuadricula("Pos-A1", 11, 11, TipoNodo.CLIENTE), 2.0, 7.0));
        flotaActualizada.add(new EstadoVehiculo("Auto-2", TipoVehiculo.AUTO, almacenCentral, 2.0, 7.0));
        flotaActualizada.add(new EstadoVehiculo("Moto-1", TipoVehiculo.MOTO, new NodoCuadricula("Pos-M1", 8, 14, TipoNodo.CLIENTE), 2.0, 7.0));
        flotaActualizada.add(new EstadoVehiculo("Moto-2", TipoVehiculo.MOTO, almacenCentral, 2.0, 7.0));
        flotaActualizada.add(new EstadoVehiculo("Bici-1", TipoVehiculo.BICI, almacenCentral, 2.0, 7.0));
        flotaActualizada.add(new EstadoVehiculo("Bici-2", TipoVehiculo.BICI, almacenCentral, 2.0, 7.0));

        ContextoProblema contextoReplan = new ContextoProblema(2.0, mapa);
        contextoReplan.agregarAlmacen(almacenCentral);
        contextoReplan.agregarAlmacen(almacenInter1);
        contextoReplan.agregarAlmacen(almacenInter2);

        for (EstadoVehiculo ev : flotaActualizada) contextoReplan.agregarVehiculo(ev);

        // Pedidos pendientes + nuevos pedidos exprés en t = 2.0h
        for (Pedido p : pedidosIniciales) {
            if (!p.getId().equals("Ped-01")) {
                contextoReplan.agregarPedido(p);
            }
        }
        contextoReplan.agregarPedido(new Pedido("Ped-11-EXPRES", new NodoCuadricula("C11", 13, 10, TipoNodo.CLIENTE), 3, 2.0, 4.0));
        contextoReplan.agregarPedido(new Pedido("Ped-12-EXPRES", new NodoCuadricula("C12", 7, 7, TipoNodo.CLIENTE), 2, 2.0, 8.0));

        tiempoInicio = System.currentTimeMillis();
        Solucion solucionReplan = solucionador.resolver(contextoReplan);
        tiempoFin = System.currentTimeMillis();

        imprimirSolucion(solucionReplan, (tiempoFin - tiempoInicio));

        System.out.println("\n=========================================================");
        System.out.println("   ¡Ejecución de ALNS y Replanificación Completada!      ");
        System.out.println("=========================================================");
    }

    private static void imprimirSolucion(Solucion solucion, long tiempoEjecucionMs) {
        System.out.printf("Tiempo de Ejecución : %d ms\n", tiempoEjecucionMs);
        System.out.printf("Costo Total Solución: S/ %.2f\n", solucion.calcularCostoTotal());
        System.out.printf("Pedidos No Asignados: %d\n", solucion.getPedidosNoAsignados().size());
        if (!solucion.getPedidosNoAsignados().isEmpty()) {
            System.out.println("Lista no asignados  : " + solucion.getPedidosNoAsignados());
        }
        System.out.println("\nRutas de Vehículos Generadas:");
        for (Ruta r : solucion.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) {
                System.out.printf(" - Vehículo %s (%s): %d pedidos asignados | Distancia: %.1f km | Costo: S/ %.2f\n",
                        r.getVehiculo().getId(), r.getVehiculo().getTipo().getNombre(),
                        r.getPedidosAsignados().size(), r.getDistanciaTotal(), r.getCostoTotal());
                System.out.print("   Paradas: ");
                for (int i = 0; i < r.getNodosCamino().size(); i++) {
                    System.out.printf("%s(t=%.2fh) -> ", r.getNodosCamino().get(i).getId(), r.getTiemposLlegada().get(i));
                }
                System.out.println("FIN");
            }
        }
    }
}
