package com.paqrap.solucionador.operadores;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import java.util.*;

public class DestruccionPeorCosto implements OperadorDestruccion {

    @Override
    public String getNombre() {
        return "Destrucción por Peor Costo";
    }

    @Override
    public List<Pedido> destruir(Solucion solucion, int q, ContextoProblema contexto) {
        List<Pedido> removidos = new ArrayList<>();
        Map<Pedido, Double> ahorros = new HashMap<>();

        for (Ruta ruta : solucion.getRutas()) {
            double costoOriginal = ruta.getCostoTotal();
            List<Pedido> pedidos = new ArrayList<>(ruta.getPedidosAsignados());

            for (Pedido p : pedidos) {
                Ruta rutaTemporal = ruta.copiar();
                rutaTemporal.getPedidosAsignados().remove(p);
                EvaluadorCostos.recalculareRuta(rutaTemporal, contexto.getMapa(), contexto.getConfiguracionOperacion());

                double ahorroCosto = costoOriginal - rutaTemporal.getCostoTotal();
                ahorros.put(p, ahorroCosto);
            }
        }

        if (ahorros.isEmpty()) return removidos;

        List<Map.Entry<Pedido, Double>> entradasOrdenadas = new ArrayList<>(ahorros.entrySet());
        entradasOrdenadas.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));

        int aRemover = Math.min(q, entradasOrdenadas.size());
        for (int i = 0; i < aRemover; i++) {
            Pedido p = entradasOrdenadas.get(i).getKey();
            removidos.add(p);

            for (Ruta r : solucion.getRutas()) {
                if (r.getPedidosAsignados().remove(p)) {
                    EvaluadorCostos.recalculareRuta(r, contexto.getMapa(), contexto.getConfiguracionOperacion());
                    break;
                }
            }
        }

        return removidos;
    }
}
