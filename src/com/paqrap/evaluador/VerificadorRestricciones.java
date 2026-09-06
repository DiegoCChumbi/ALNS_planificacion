package com.paqrap.evaluador;

import com.paqrap.configuracion.ConfiguracionOperacion;
import com.paqrap.modelo.*;
import java.util.List;

public class VerificadorRestricciones {

    public static boolean esRutaFactible(Ruta ruta, MapaCuadricula mapa, ConfiguracionOperacion operacion) {
        if (operacion == null) operacion = new ConfiguracionOperacion();

        EstadoVehiculo ev = ruta.getVehiculo();
        TipoVehiculo tv = ev.getTipo();
        List<Pedido> pedidos = ruta.getPedidosAsignados();

        int demandaTotal = 0;
        for (Pedido p : pedidos) {
            demandaTotal += p.getCantidad();
        }

        // Verificación de capacidad
        if (demandaTotal > tv.getCapacidad()) {
            return false;
        }

        // Verificación de ventana de tiempo, refrigerio obligatorio y jornada laboral
        double tiempoActual = ev.getMarcaTiempoActual();
        NodoCuadricula posActual = ev.getUbicacionActual();
        double inicioTurno = 0.0;
        double duracionTurno = operacion.getDuracionTurnoHoras();
        double finTurno = inicioTurno + duracionTurno;
        double tiempoServicio = operacion.getTiempoServicioClienteHoras();
        double duracionRefrigerio = operacion.getDuracionRefrigerioHoras();
        double margenRefrigerio = operacion.getMargenRefrigerioHoras();

        // Ventana legal de refrigerio: al menos una hora después del inicio y una hora antes del cambio de turno
        double ventanaMinRefrigerio = inicioTurno + margenRefrigerio;
        double ventanaMaxRefrigerio = finTurno - margenRefrigerio;
        double tiempoInicioRefrigerioIdeal = duracionTurno / 2.0;
        boolean refrigerioTomado = ev.isRefrigerioTomado();

        for (Pedido pedido : pedidos) {
            // Pausa obligatoria de alimentación si se alcanza la mitad del turno
            if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerioIdeal) {
                if (tiempoActual < ventanaMinRefrigerio || (tiempoActual + duracionRefrigerio) > ventanaMaxRefrigerio) {
                    return false;
                }
                tiempoActual += duracionRefrigerio;
                refrigerioTomado = true;
            }

            double tiempoViaje = CalculadorDistancia.getTiempoViajeHoras(mapa, posActual, pedido.getDestino(), tv);
            double tiempoLlegada = tiempoActual + tiempoViaje;

            // Verificar plazo límite del cliente
            if (tiempoLlegada > pedido.getTiempoMaximoEntrega()) {
                return false;
            }

            // Tiempo de atención al cliente configurable
            double tiempoSalida = tiempoLlegada + tiempoServicio;

            // Verificar fin de jornada laboral del conductor
            if (tiempoSalida > finTurno) {
                return false;
            }

            tiempoActual = tiempoSalida;
            posActual = pedido.getDestino();
        }

        // 3. Verificación de retorno al almacén base antes de finalizar el turno
        if (!pedidos.isEmpty() && ev.getAlmacenBase() != null) {
            if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerioIdeal) {
                if (tiempoActual < ventanaMinRefrigerio || (tiempoActual + duracionRefrigerio) > ventanaMaxRefrigerio) {
                    return false;
                }
                tiempoActual += duracionRefrigerio;
                refrigerioTomado = true;
            }

            double distRetorno = CalculadorDistancia.getDistancia(mapa, posActual, ev.getAlmacenBase());
            double tiempoViajeRetorno = distRetorno / tv.getVelocidadKmH();
            double tiempoLlegadaAlmacen = tiempoActual + tiempoViajeRetorno;

            if (tiempoLlegadaAlmacen > finTurno) {
                return false; // El vehículo no alcanza a regresar a base dentro de las 8h del turno
            }
        }

        return true;
    }

    public static boolean esSolucionFactible(Solucion solucion, MapaCuadricula mapa, ConfiguracionOperacion operacion, ContextoProblema contexto) {
        if (solucion == null) return false;

        java.util.Map<String, Integer> demandaPorAlmacen = new java.util.HashMap<>();
        for (Ruta ruta : solucion.getRutas()) {
            if (!esRutaFactible(ruta, mapa, operacion)) {
                return false;
            }
            if (ruta.getVehiculo().getAlmacenBase() != null) {
                String almId = ruta.getVehiculo().getAlmacenBase().getId();
                int demandaRuta = 0;
                for (Pedido p : ruta.getPedidosAsignados()) {
                    demandaRuta += p.getCantidad();
                }
                demandaPorAlmacen.put(almId, demandaPorAlmacen.getOrDefault(almId, 0) + demandaRuta);
            }
        }

        // Validar tope de stock en almacenes intermedios (máximo 1,000 unidades)
        if (contexto != null) {
            for (NodoCuadricula n : contexto.getAlmacenes()) {
                if (n instanceof Almacen alm && !alm.esCentral()) {
                    int demandaDespachada = demandaPorAlmacen.getOrDefault(alm.getId(), 0);
                    if (demandaDespachada > alm.getStockDisponible()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean esRutaFactible(Ruta ruta, MapaCuadricula mapa) {
        return esRutaFactible(ruta, mapa, new ConfiguracionOperacion());
    }
}
