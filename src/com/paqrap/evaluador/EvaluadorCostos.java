package com.paqrap.evaluador;

import com.paqrap.configuracion.ConfiguracionOperacion;
import com.paqrap.modelo.*;
import java.util.List;

public class EvaluadorCostos {

    public static void recalculareRuta(Ruta ruta, MapaCuadricula mapa, ConfiguracionOperacion operacion) {
        EstadoVehiculo ev = ruta.getVehiculo();
        TipoVehiculo tv = ev.getTipo();
        List<Pedido> pedidos = ruta.getPedidosAsignados();
        List<NodoCuadricula> camino = ruta.getNodosCamino();
        List<Double> llegadas = ruta.getTiemposLlegada();

        camino.clear();
        llegadas.clear();

        NodoCuadricula posActual = ev.getUbicacionActual();
        double tiempoActual = ev.getMarcaTiempoActual();
        double distTotal = 0.0;
        double tiempoServicio = (operacion != null) ? operacion.getTiempoServicioClienteHoras() : 1.0;
        double duracionRefrigerio = (operacion != null) ? operacion.getDuracionRefrigerioHoras() : 1.0;
        double duracionTurno = (operacion != null) ? operacion.getDuracionTurnoHoras() : 8.0;
        double tiempoInicioRefrigerio = duracionTurno / 2.0;
        boolean refrigerioTomado = ev.isRefrigerioTomado();

        camino.add(posActual);
        llegadas.add(tiempoActual);

        for (Pedido pedido : pedidos) {
            // Si corresponde refrigerio antes de iniciar el siguiente tramo
            if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerio) {
                tiempoActual += duracionRefrigerio;
                refrigerioTomado = true;
            }

            double dist = CalculadorDistancia.getDistancia(mapa, posActual, pedido.getDestino());
            double tiempoViaje = dist / tv.getVelocidadKmH();
            double tiempoLlegada = tiempoActual + tiempoViaje;

            distTotal += dist;
            camino.add(pedido.getDestino());
            llegadas.add(tiempoLlegada);

            tiempoActual = tiempoLlegada + tiempoServicio;
            posActual = pedido.getDestino();
        }

        // Retorno al almacén base si la ruta tiene pedidos asignados
        if (!pedidos.isEmpty() && ev.getAlmacenBase() != null) {
            if (!refrigerioTomado && tiempoActual >= tiempoInicioRefrigerio) {
                tiempoActual += duracionRefrigerio;
                refrigerioTomado = true;
            }

            double distRetorno = CalculadorDistancia.getDistancia(mapa, posActual, ev.getAlmacenBase());
            double tiempoViajeRetorno = distRetorno / tv.getVelocidadKmH();
            double tiempoLlegadaBase = tiempoActual + tiempoViajeRetorno;

            distTotal += distRetorno;
            camino.add(ev.getAlmacenBase());
            llegadas.add(tiempoLlegadaBase);
        }

        ruta.setDistanciaTotal(distTotal);
    }

    public static void recalculareRuta(Ruta ruta, MapaCuadricula mapa) {
        recalculareRuta(ruta, mapa, new ConfiguracionOperacion());
    }
}
