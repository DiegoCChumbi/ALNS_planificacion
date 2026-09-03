package com.paqrap.solucionador;

import com.paqrap.evaluador.EvaluadorCostos;
import com.paqrap.modelo.*;
import com.paqrap.solucionador.busquedalocal.BusquedaLocalRVND;
import com.paqrap.solucionador.nucleo.GestorPesosAdaptativo;
import com.paqrap.solucionador.nucleo.SolucionadorParticionConjuntos;
import com.paqrap.solucionador.operadores.*;
import java.util.*;

public class SolucionadorALNS {

    private final ConfiguracionALNS configuracion;
    private final GestorPesosAdaptativo<OperadorDestruccion> gestorDestruccion;
    private final GestorPesosAdaptativo<OperadorReparacion> gestorReparacion;
    private final BusquedaLocalRVND busquedaLocal;
    private final SolucionadorParticionConjuntos solucionadorSPP;

    public SolucionadorALNS(ConfiguracionALNS configuracion) {
        this.configuracion = configuracion;

        List<OperadorDestruccion> operadoresDestruccion = Arrays.asList(
                new DestruccionAleatoria(),
                new DestruccionPeorCosto(),
                new DestruccionShaw()
        );

        List<OperadorReparacion> operadoresReparacion = Arrays.asList(
                new ReparacionVoraz(),
                new ReparacionRegret(2),
                new ReparacionRegret(3)
        );

        this.gestorDestruccion = new GestorPesosAdaptativo<>(operadoresDestruccion);
        this.gestorReparacion = new GestorPesosAdaptativo<>(operadoresReparacion);
        this.busquedaLocal = new BusquedaLocalRVND();
        this.solucionadorSPP = new SolucionadorParticionConjuntos();
    }

    public Solucion resolver(ContextoProblema contexto) {
        // 1. Solución Inicial mediante inserción Regret-2
        Solucion solucionActual = construirSolucionInicial(contexto);
        Solucion mejorSolucion = solucionActual.copiar();

        List<Ruta> poolRutas = new ArrayList<>();
        agregarRutasAlPool(poolRutas, solucionActual);

        Random random = new Random();
        int iteracion = 1;
        int contadorSinMejora = 0;

        while (iteracion <= configuracion.getMaxIteraciones() && contadorSinMejora < configuracion.getMaxSinMejora()) {
            Solucion vecina = solucionActual.copiar();

            // 2. Seleccionar operadores de destrucción y reparación
            int idxDestruccion = gestorDestruccion.seleccionarIndiceOperador();
            int idxReparacion = gestorReparacion.seleccionarIndiceOperador();

            OperadorDestruccion opDestruccion = gestorDestruccion.seleccionarOperador();
            OperadorReparacion opReparacion = gestorReparacion.seleccionarOperador();

            int q = configuracion.getMinCantidadDestruccion() + random.nextInt(configuracion.getMaxCantidadDestruccion() - configuracion.getMinCantidadDestruccion() + 1);

            // 3. Ruina y Creación
            List<Pedido> pedidosRemovidos = opDestruccion.destruir(vecina, q, contexto);
            vecina.getPedidosNoAsignados().addAll(pedidosRemovidos);

            opReparacion.reparar(vecina, vecina.getPedidosNoAsignados(), contexto);

            // Recalcular costos
            for (Ruta r : vecina.getRutas()) {
                EvaluadorCostos.recalculareRuta(r, contexto.getMapa());
            }

            // 4. Búsqueda Local Embebida (RVND)
            if (vecina.calcularCostoTotal() < mejorSolucion.calcularCostoTotal()) {
                vecina = busquedaLocal.aplicar(vecina, contexto);
            }

            agregarRutasAlPool(poolRutas, vecina);

            double costoActual = solucionActual.calcularCostoTotal();
            double costoVecina = vecina.calcularCostoTotal();
            double mejorCosto = mejorSolucion.calcularCostoTotal();

            // 5. Criterio de Aceptación y Asignación de Puntajes
            if (costoVecina < mejorCosto) {
                mejorSolucion = vecina.copiar();
                solucionActual = vecina.copiar();
                gestorDestruccion.agregarPuntaje(idxDestruccion, configuracion.getPuntajeMejorGlobal());
                gestorReparacion.agregarPuntaje(idxReparacion, configuracion.getPuntajeMejorGlobal());
                contadorSinMejora = 0;
            } else if (costoVecina < costoActual) {
                solucionActual = vecina.copiar();
                gestorDestruccion.agregarPuntaje(idxDestruccion, configuracion.getPuntajeMejorActual());
                gestorReparacion.agregarPuntaje(idxReparacion, configuracion.getPuntajeMejorActual());
                contadorSinMejora++;
            } else {
                double probabilidadAceptacion = Math.exp((costoActual - costoVecina) / 100.0);
                if (random.nextDouble() < probabilidadAceptacion) {
                    solucionActual = vecina.copiar();
                    gestorDestruccion.agregarPuntaje(idxDestruccion, configuracion.getPuntajeAceptado());
                    gestorReparacion.agregarPuntaje(idxReparacion, configuracion.getPuntajeAceptado());
                }
                contadorSinMejora++;
            }

            // 6. Recombinación Periódica de Partición de Conjuntos (SPP)
            if (iteracion % configuracion.getIntervaloSPP() == 0) {
                Solucion solucionSPP = solucionadorSPP.resolver(poolRutas, contexto);
                if (solucionSPP.calcularCostoTotal() < mejorSolucion.calcularCostoTotal()) {
                    mejorSolucion = solucionSPP.copiar();
                    solucionActual = solucionSPP.copiar();
                }
            }

            // 7. Actualización Adaptativa de Pesos
            if (iteracion % configuracion.getIntervaloActualizacion() == 0) {
                gestorDestruccion.actualizarPesos(configuracion);
                gestorReparacion.actualizarPesos(configuracion);
            }

            iteracion++;
        }

        return mejorSolucion;
    }

    private Solucion construirSolucionInicial(ContextoProblema contexto) {
        Solucion sol = new Solucion();
        for (EstadoVehiculo ev : contexto.getVehiculosActivos()) {
            Ruta r = new Ruta(ev);
            EvaluadorCostos.recalculareRuta(r, contexto.getMapa());
            sol.getRutas().add(r);
        }

        List<Pedido> noAsignados = new ArrayList<>(contexto.getPedidosPendientes());
        OperadorReparacion regret2 = new ReparacionRegret(2);
        regret2.reparar(sol, noAsignados, contexto);

        sol.getPedidosNoAsignados().addAll(noAsignados);
        return sol;
    }

    private void agregarRutasAlPool(List<Ruta> pool, Solucion solucion) {
        for (Ruta r : solucion.getRutas()) {
            if (!r.getPedidosAsignados().isEmpty()) {
                pool.add(r.copiar());
            }
        }
    }
}
