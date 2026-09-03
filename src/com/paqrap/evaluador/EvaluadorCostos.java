package com.paqrap.evaluador;

import com.paqrap.modelo.*;
import java.util.List;

public class EvaluadorCostos {

    public static void recalculareRuta(Ruta ruta, MapaCuadricula mapa) {
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

        camino.add(posActual);
        llegadas.add(tiempoActual);

        for (Pedido pedido : pedidos) {
            double dist = CalculadorDistancia.getDistancia(mapa, posActual, pedido.getDestino());
            double tiempoViaje = dist / tv.getVelocidadKmH();
            double tiempoLlegada = tiempoActual + tiempoViaje;

            distTotal += dist;
            camino.add(pedido.getDestino());
            llegadas.add(tiempoLlegada);

            tiempoActual = tiempoLlegada + 1.0; // 1 hora de tiempo de atención
            posActual = pedido.getDestino();
        }

        ruta.setDistanciaTotal(distTotal);
    }
}
