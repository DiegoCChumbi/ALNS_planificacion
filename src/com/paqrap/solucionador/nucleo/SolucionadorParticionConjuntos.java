package com.paqrap.solucionador.nucleo;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

public class SolucionadorParticionConjuntos {

    public Solucion resolver(List<Ruta> poolRutas, ContextoProblema contexto) {
        if (poolRutas.isEmpty()) {
            return new Solucion();
        }

        List<Pedido> todosPedidos = contexto.getPedidosPendientes();
        Set<Pedido> pedidosCubiertos = new HashSet<>();
        List<Ruta> rutasSeleccionadas = new ArrayList<>();
        Set<String> vehiculosUsados = new HashSet<>();

        List<Ruta> poolOrdenado = new ArrayList<>(poolRutas);
        poolOrdenado.sort(Comparator.comparingDouble(r -> {
            if (r.getPedidosAsignados().isEmpty()) return Double.MAX_VALUE;
            return r.getCostoTotal() / r.getPedidosAsignados().size();
        }));

        for (Ruta candidata : poolOrdenado) {
            String idVehiculo = candidata.getVehiculo().getId();
            if (vehiculosUsados.contains(idVehiculo)) continue;

            boolean haySolapamiento = false;
            for (Pedido p : candidata.getPedidosAsignados()) {
                if (pedidosCubiertos.contains(p)) {
                    haySolapamiento = true;
                    break;
                }
            }

            if (!haySolapamiento && !candidata.getPedidosAsignados().isEmpty()) {
                Ruta rCopia = candidata.copiar();
                EvaluadorCostos.recalculareRuta(rCopia, contexto.getMapa());
                rutasSeleccionadas.add(rCopia);
                pedidosCubiertos.addAll(rCopia.getPedidosAsignados());
                vehiculosUsados.add(idVehiculo);
            }
        }

        for (EstadoVehiculo ev : contexto.getVehiculosActivos()) {
            if (!vehiculosUsados.contains(ev.getId())) {
                Ruta rutaVacia = new Ruta(ev);
                EvaluadorCostos.recalculareRuta(rutaVacia, contexto.getMapa());
                rutasSeleccionadas.add(rutaVacia);
            }
        }

        Solucion resultado = new Solucion();
        resultado.getRutas().addAll(rutasSeleccionadas);

        for (Pedido p : todosPedidos) {
            if (!pedidosCubiertos.contains(p)) {
                resultado.getPedidosNoAsignados().add(p);
            }
        }

        return resultado;
    }
}
