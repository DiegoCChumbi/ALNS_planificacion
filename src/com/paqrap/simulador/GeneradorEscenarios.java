package com.paqrap.simulador;

import com.paqrap.modelo.*;
import java.util.*;

public class GeneradorEscenarios {

    public static class DefinicionEscenario {
        private final String nombre;
        private final MapaCuadricula mapa;
        private final NodoCuadricula almacenCentral;
        private final List<NodoCuadricula> almacenesIntermedios;
        private final List<EstadoVehiculo> flota;
        private final List<Pedido> pedidos;

        public DefinicionEscenario(String nombre, MapaCuadricula mapa, NodoCuadricula almacenCentral,
                                   List<NodoCuadricula> almacenesIntermedios,
                                   List<EstadoVehiculo> flota, List<Pedido> pedidos) {
            this.nombre = nombre;
            this.mapa = mapa;
            this.almacenCentral = almacenCentral;
            this.almacenesIntermedios = almacenesIntermedios;
            this.flota = flota;
            this.pedidos = pedidos;
        }

        public String getNombre() { return nombre; }
        public MapaCuadricula getMapa() { return mapa; }
        public NodoCuadricula getAlmacenCentral() { return almacenCentral; }
        public List<NodoCuadricula> getAlmacenesIntermedios() { return almacenesIntermedios; }
        public List<EstadoVehiculo> getFlota() { return flota; }
        public List<Pedido> getPedidos() { return pedidos; }

        public ContextoProblema construirContexto(double tiempoActual) {
            ContextoProblema ctx = new ContextoProblema(tiempoActual, mapa);
            ctx.agregarAlmacen(almacenCentral);
            for (NodoCuadricula ai : almacenesIntermedios) ctx.agregarAlmacen(ai);
            for (EstadoVehiculo ev : flota) ctx.agregarVehiculo(ev);
            for (Pedido p : pedidos) ctx.agregarPedido(p);
            return ctx;
        }
    }

    public static DefinicionEscenario crearEscenarioComplejo() {
        int ancho = 30;
        int alto = 30;
        MapaCuadricula mapa = new MapaCuadricula(ancho, alto);

        // Almacenes
        NodoCuadricula ac = new NodoCuadricula("AlmacenCentral", 15, 15, TipoNodo.ALMACEN_CENTRAL);
        NodoCuadricula ai1 = new NodoCuadricula("AlmacenInter-1", 6, 8, TipoNodo.ALMACEN_INTERMEDIO);
        NodoCuadricula ai2 = new NodoCuadricula("AlmacenInter-2", 24, 22, TipoNodo.ALMACEN_INTERMEDIO);
        List<NodoCuadricula> almacenesInter = Arrays.asList(ai1, ai2);

        // Flota ampliada (16 vehículos distribuidos estratégicamente en los 3 almacenes)
        List<EstadoVehiculo> flota = new ArrayList<>();
        // Almacén Central (7 vehículos)
        flota.add(new EstadoVehiculo("Auto-1", TipoVehiculo.AUTO, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Auto-2", TipoVehiculo.AUTO, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Auto-5", TipoVehiculo.AUTO, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Moto-1", TipoVehiculo.MOTO, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Moto-2", TipoVehiculo.MOTO, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Bici-1", TipoVehiculo.BICI, ac, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Bici-2", TipoVehiculo.BICI, ac, 0.0, 7.0));

        // Almacén Intermedio 1 (Sector Noroeste - 5 vehículos)
        flota.add(new EstadoVehiculo("Auto-3", TipoVehiculo.AUTO, ai1, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Moto-3", TipoVehiculo.MOTO, ai1, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Moto-5", TipoVehiculo.MOTO, ai1, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Bici-3", TipoVehiculo.BICI, ai1, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Bici-5", TipoVehiculo.BICI, ai1, 0.0, 7.0));

        // Almacén Intermedio 2 (Sector Sureste - 4 vehículos)
        flota.add(new EstadoVehiculo("Auto-4", TipoVehiculo.AUTO, ai2, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Auto-6", TipoVehiculo.AUTO, ai2, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Moto-4", TipoVehiculo.MOTO, ai2, 0.0, 7.0));
        flota.add(new EstadoVehiculo("Bici-4", TipoVehiculo.BICI, ai2, 0.0, 7.0));

        // 35 Pedidos Heterogéneos en los 4 cuadrantes
        List<Pedido> pedidos = new ArrayList<>();
        // Cuadrante 1 (Noroeste)
        pedidos.add(new Pedido("Ped-01", new NodoCuadricula("C01", 3, 4, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-02", new NodoCuadricula("C02", 8, 2, TipoNodo.CLIENTE), 6, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-03", new NodoCuadricula("C03", 5, 11, TipoNodo.CLIENTE), 2, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-04", new NodoCuadricula("C04", 2, 9, TipoNodo.CLIENTE), 3, 0.0, 12.0));
        pedidos.add(new Pedido("Ped-05", new NodoCuadricula("C05", 11, 5, TipoNodo.CLIENTE), 8, 0.0, 18.0));
        pedidos.add(new Pedido("Ped-06", new NodoCuadricula("C06", 7, 13, TipoNodo.CLIENTE), 1, 0.0, 36.0));
        pedidos.add(new Pedido("Ped-07", new NodoCuadricula("C07", 12, 12, TipoNodo.CLIENTE), 5, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-08", new NodoCuadricula("C08", 4, 14, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-09", new NodoCuadricula("C09", 9, 8, TipoNodo.CLIENTE), 7, 0.0, 12.0));

        // Cuadrante 2 (Noreste)
        pedidos.add(new Pedido("Ped-10", new NodoCuadricula("C10", 18, 3, TipoNodo.CLIENTE), 6, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-11", new NodoCuadricula("C11", 22, 5, TipoNodo.CLIENTE), 3, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-12", new NodoCuadricula("C12", 26, 2, TipoNodo.CLIENTE), 5, 0.0, 12.0));
        pedidos.add(new Pedido("Ped-13", new NodoCuadricula("C13", 28, 8, TipoNodo.CLIENTE), 2, 0.0, 18.0));
        pedidos.add(new Pedido("Ped-14", new NodoCuadricula("C14", 19, 10, TipoNodo.CLIENTE), 8, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-15", new NodoCuadricula("C15", 24, 11, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-16", new NodoCuadricula("C16", 27, 13, TipoNodo.CLIENTE), 12, 0.0, 36.0));
        pedidos.add(new Pedido("Ped-17", new NodoCuadricula("C17", 17, 14, TipoNodo.CLIENTE), 3, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-18", new NodoCuadricula("C18", 21, 9, TipoNodo.CLIENTE), 6, 0.0, 12.0));

        // Cuadrante 3 (Suroeste)
        pedidos.add(new Pedido("Ped-19", new NodoCuadricula("C19", 3, 18, TipoNodo.CLIENTE), 4, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-20", new NodoCuadricula("C20", 7, 20, TipoNodo.CLIENTE), 8, 0.0, 18.0));
        pedidos.add(new Pedido("Ped-21", new NodoCuadricula("C21", 2, 24, TipoNodo.CLIENTE), 3, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-22", new NodoCuadricula("C22", 8, 26, TipoNodo.CLIENTE), 6, 0.0, 12.0));
        pedidos.add(new Pedido("Ped-23", new NodoCuadricula("C23", 12, 22, TipoNodo.CLIENTE), 2, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-24", new NodoCuadricula("C24", 5, 28, TipoNodo.CLIENTE), 5, 0.0, 36.0));
        pedidos.add(new Pedido("Ped-25", new NodoCuadricula("C25", 10, 27, TipoNodo.CLIENTE), 4, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-26", new NodoCuadricula("C26", 13, 18, TipoNodo.CLIENTE), 7, 0.0, 12.0));

        // Cuadrante 4 (Sureste)
        pedidos.add(new Pedido("Ped-27", new NodoCuadricula("C27", 18, 19, TipoNodo.CLIENTE), 5, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-28", new NodoCuadricula("C28", 22, 17, TipoNodo.CLIENTE), 8, 0.0, 12.0));
        pedidos.add(new Pedido("Ped-29", new NodoCuadricula("C29", 25, 20, TipoNodo.CLIENTE), 3, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-30", new NodoCuadricula("C30", 28, 24, TipoNodo.CLIENTE), 6, 0.0, 18.0));
        pedidos.add(new Pedido("Ped-31", new NodoCuadricula("C31", 19, 25, TipoNodo.CLIENTE), 4, 0.0, 8.0));
        pedidos.add(new Pedido("Ped-32", new NodoCuadricula("C32", 23, 28, TipoNodo.CLIENTE), 14, 0.0, 36.0));
        pedidos.add(new Pedido("Ped-33", new NodoCuadricula("C33", 27, 27, TipoNodo.CLIENTE), 2, 0.0, 4.0));
        pedidos.add(new Pedido("Ped-34", new NodoCuadricula("C34", 17, 28, TipoNodo.CLIENTE), 5, 0.0, 12.0));
        pedidos.add(new Pedido("Ped-35", new NodoCuadricula("C35", 21, 22, TipoNodo.CLIENTE), 6, 0.0, 8.0));

        return new DefinicionEscenario("Escenario Complejo Metropolitano (30x30, 12 Vehículos, 35 Pedidos)",
                mapa, ac, almacenesInter, flota, pedidos);
    }
}
