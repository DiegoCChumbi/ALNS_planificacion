package com.paqrap.evaluador;

import com.paqrap.modelo.*;
import java.util.List;

public class VerificadorRestricciones {

    public static boolean esRutaFactible(Ruta ruta, MapaCuadricula mapa) {
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

        // Verificación de ventana de tiempo y jornada laboral
        double tiempoActual = ev.getMarcaTiempoActual();
        NodoCuadricula posActual = ev.getUbicacionActual();
        double inicioTurno = ev.getTiempoInicioTurno();
        double finTurno = inicioTurno + 8.0;

        for (Pedido pedido : pedidos) {
            double tiempoViaje = CalculadorDistancia.getTiempoViajeHoras(mapa, posActual, pedido.getDestino(), tv);
            double tiempoLlegada = tiempoActual + tiempoViaje;

            // Tiempo de atención al cliente = 1 hora
            double tiempoSalida = tiempoLlegada + 1.0;

            // Verificar plazo límite del cliente
            if (tiempoLlegada > pedido.getTiempoMaximoEntrega()) {
                return false;
            }

            // Verificar fin de jornada laboral del conductor
            if (tiempoSalida > finTurno) {
                return false;
            }

            tiempoActual = tiempoSalida;
            posActual = pedido.getDestino();
        }

        return true;
    }
}
