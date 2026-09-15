package com.paqrap.evaluador;

import com.paqrap.modelo.*;

public class CalculadorDistancia {

    public static double getDistancia(MapaCuadricula mapa, NodoCuadricula n1, NodoCuadricula n2) {
        return mapa.getDistanciaMasCorta(n1, n2);
    }

    public static double getDistancia(MapaCuadricula mapa, NodoCuadricula n1, NodoCuadricula n2, double tiempoHoras) {
        return mapa.getDistanciaMasCorta(n1, n2, tiempoHoras);
    }

    public static double getTiempoViajeHoras(MapaCuadricula mapa, NodoCuadricula n1, NodoCuadricula n2, TipoVehiculo tipoVehiculo) {
        double distanciaKm = getDistancia(mapa, n1, n2);
        return distanciaKm / tipoVehiculo.getVelocidadKmH();
    }
}
