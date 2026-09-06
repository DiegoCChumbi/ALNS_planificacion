package com.paqrap.configuracion;

import com.paqrap.modelo.*;
import com.paqrap.solucionador.ConfiguracionALNS;
import java.io.*;
import java.util.*;

public class ConfiguracionSistema {
    private ConfiguracionEntorno entorno = new ConfiguracionEntorno();
    private ConfiguracionOperacion operacion = new ConfiguracionOperacion();
    private NodoCuadricula almacenCentral = new NodoCuadricula("AlmacenCentral", 15, 15, TipoNodo.ALMACEN_CENTRAL);
    private List<NodoCuadricula> almacenesIntermedios = new ArrayList<>();
    private Map<String, Integer> capacidadAlmacenes = new HashMap<>();
    private List<TipoVehiculo> tiposVehiculo = new ArrayList<>();
    private List<EstadoVehiculo> flota = new ArrayList<>();
    private ConfiguracionALNS alns = new ConfiguracionALNS();
    private ConfiguracionSemaforos semaforos = new ConfiguracionSemaforos();
    private ConfiguracionSimulacion simulacion = new ConfiguracionSimulacion();
    private List<Pedido> pedidosIniciales = new ArrayList<>();
    private List<Map<String, Object>> disrupciones = new ArrayList<>();

    public static ConfiguracionSistema cargarDesdeArchivo(File archivo) throws IOException {
        Map<String, Object> root = LectorJson.parseObjeto(archivo);
        ConfiguracionSistema config = new ConfiguracionSistema();

        // 1. Entorno
        Map<String, Object> mEntorno = LectorJson.getObject(root, "entorno");
        if (!mEntorno.isEmpty()) {
            config.entorno.setAnchoMapa(LectorJson.getInt(mEntorno, "anchoMapa", 30));
            config.entorno.setAltoMapa(LectorJson.getInt(mEntorno, "altoMapa", 30));
            config.entorno.setKmPorCuadra(LectorJson.getDouble(mEntorno, "kmPorCuadra", 1.0));
            config.entorno.setCallesDobleSentido(LectorJson.getBoolean(mEntorno, "callesDobleSentido", true));
            config.entorno.setPenalizacionInalcanzable(LectorJson.getDouble(mEntorno, "penalizacionInalcanzable", 999999.0));
        }

        // 2. Operación
        Map<String, Object> mOper = LectorJson.getObject(root, "operacion");
        if (!mOper.isEmpty()) {
            config.operacion.setDuracionTurnoHoras(LectorJson.getDouble(mOper, "duracionTurnoHoras", 8.0));
            config.operacion.setHoraInicioTurno(LectorJson.getDouble(mOper, "horaInicioTurno", 7.0));
            config.operacion.setTiempoServicioClienteHoras(LectorJson.getDouble(mOper, "tiempoServicioClienteHoras", 1.0));
            config.operacion.setDuracionRefrigerioHoras(LectorJson.getDouble(mOper, "duracionRefrigerioHoras", 1.0));
            config.operacion.setMargenRefrigerioHoras(LectorJson.getDouble(mOper, "margenRefrigerioHoras", 1.0));
            config.operacion.setTiempoCargaAlmacenHoras(LectorJson.getDouble(mOper, "tiempoCargaAlmacenHoras", 0.0));
            config.operacion.setPenalizacionPedidoNoAsignado(LectorJson.getDouble(mOper, "penalizacionPedidoNoAsignado", 10000.0));
        }

        // 3. Almacenes
        Map<String, Object> mAlm = LectorJson.getObject(root, "almacenes");
        Map<String, NodoCuadricula> mapaAlmacenes = new HashMap<>();

        Map<String, Object> mCentral = LectorJson.getObject(mAlm, "central");
        if (!mCentral.isEmpty()) {
            String id = LectorJson.getString(mCentral, "id", "AlmacenCentral");
            int x = LectorJson.getInt(mCentral, "x", 15);
            int y = LectorJson.getInt(mCentral, "y", 15);
            config.almacenCentral = new Almacen(id, "Almacén Central", x, y, true);
            mapaAlmacenes.put(id, config.almacenCentral);
        }

        List<Object> lInter = LectorJson.getList(mAlm, "intermedios");
        config.almacenesIntermedios.clear();
        for (Object obj : lInter) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) obj;
                String id = LectorJson.getString(m, "id", "AlmacenInter");
                int x = LectorJson.getInt(m, "x", 0);
                int y = LectorJson.getInt(m, "y", 0);
                int cap = LectorJson.getInt(m, "capacidad", 1000);
                Almacen nodo = new Almacen(id, id, x, y, false, cap, cap);
                config.almacenesIntermedios.add(nodo);
                config.capacidadAlmacenes.put(id, cap);
                mapaAlmacenes.put(id, nodo);
            }
        }

        // 4. Tipos de Vehículo
        List<Object> lTipos = LectorJson.getList(root, "tiposVehiculo");
        config.tiposVehiculo.clear();
        for (Object obj : lTipos) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) obj;
                String id = LectorJson.getString(m, "id", "AUTO");
                String nom = LectorJson.getString(m, "nombre", "Auto");
                int cap = LectorJson.getInt(m, "capacidad", 24);
                double vel = LectorJson.getDouble(m, "velocidadKmH", 40.0);
                double costo = LectorJson.getDouble(m, "costoPorKm", 8.0);
                TipoVehiculo tv = new TipoVehiculo(id, nom, cap, vel, costo);
                TipoVehiculo.registrar(tv);
                config.tiposVehiculo.add(tv);
            }
        }

        // 5. Flota
        List<Object> lFlota = LectorJson.getList(root, "flota");
        config.flota.clear();
        for (Object obj : lFlota) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) obj;
                String id = LectorJson.getString(m, "id", "Vehiculo-1");
                String tipoStr = LectorJson.getString(m, "tipo", "AUTO");
                String almId = LectorJson.getString(m, "almacenInicio", "AlmacenCentral");
                NodoCuadricula almNodo = mapaAlmacenes.getOrDefault(almId, config.almacenCentral);
                TipoVehiculo tv = TipoVehiculo.obtener(tipoStr);
                config.flota.add(new EstadoVehiculo(id, tv, almNodo, almNodo, 0.0, config.operacion.getHoraInicioTurno()));
            }
        }

        // 6. ALNS
        Map<String, Object> mAlns = LectorJson.getObject(root, "alns");
        if (!mAlns.isEmpty()) {
            config.alns.setMaxIteraciones(LectorJson.getInt(mAlns, "maxIteraciones", 350));
            config.alns.setMaxSinMejora(LectorJson.getInt(mAlns, "maxSinMejora", 90));
            config.alns.setIntervaloActualizacion(LectorJson.getInt(mAlns, "intervaloActualizacion", 15));
            config.alns.setIntervaloSPP(LectorJson.getInt(mAlns, "intervaloSPP", 20));
            config.alns.setFactorReaccion(LectorJson.getDouble(mAlns, "factorReaccion", 0.2));
            config.alns.setMinCantidadDestruccion(LectorJson.getInt(mAlns, "minCantidadDestruccion", 1));
            config.alns.setMaxCantidadDestruccion(LectorJson.getInt(mAlns, "maxCantidadDestruccion", 5));
            config.alns.setPuntajeMejorGlobal(LectorJson.getDouble(mAlns, "puntajeMejorGlobal", 10.0));
            config.alns.setPuntajeMejorActual(LectorJson.getDouble(mAlns, "puntajeMejorActual", 5.0));
            config.alns.setPuntajeAceptado(LectorJson.getDouble(mAlns, "puntajeAceptado", 2.0));
            config.alns.setTemperaturaAceptacionSA(LectorJson.getDouble(mAlns, "temperaturaAceptacionSA", 100.0));
            config.alns.setTasaEnfriamientoSA(LectorJson.getDouble(mAlns, "tasaEnfriamientoSA", 0.995));
            config.alns.setPesoDistanciaShaw(LectorJson.getDouble(mAlns, "pesoDistanciaShaw", 1.0));
            config.alns.setPesoTiempoShaw(LectorJson.getDouble(mAlns, "pesoTiempoShaw", 2.0));
            config.alns.setEpsilonMejoraRVND(LectorJson.getDouble(mAlns, "epsilonMejoraRVND", 0.01));
        }

        // 7. Semáforos
        Map<String, Object> mSem = LectorJson.getObject(root, "semaforos");
        if (!mSem.isEmpty()) {
            config.semaforos.setHolguraVerdeHoras(LectorJson.getDouble(mSem, "holguraVerdeHoras", 2.0));
            config.semaforos.setHolguraAmbarHoras(LectorJson.getDouble(mSem, "holguraAmbarHoras", 0.5));
        }

        // 8. Simulación
        Map<String, Object> mSim = LectorJson.getObject(root, "simulacion");
        if (!mSim.isEmpty()) {
            config.simulacion.setDirectorioLogs(LectorJson.getString(mSim, "directorioLogs", "logs"));
            config.simulacion.setArchivoLogTexto(LectorJson.getString(mSim, "archivoLogTexto", "simulacion_movimientos.log"));
            config.simulacion.setArchivoLogJson(LectorJson.getString(mSim, "archivoLogJson", "simulacion_movimientos.json"));
            config.simulacion.setImprimirEnConsola(LectorJson.getBoolean(mSim, "imprimirEnConsola", false));
        }

        // 9. Pedidos Iniciales
        List<Object> lPedidos = LectorJson.getList(root, "pedidosIniciales");
        config.pedidosIniciales.clear();
        for (Object obj : lPedidos) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) obj;
                String id = LectorJson.getString(m, "id", "Ped-01");
                String destId = LectorJson.getString(m, "destinoId", "C01");
                int x = LectorJson.getInt(m, "x", 0);
                int y = LectorJson.getInt(m, "y", 0);
                int cant = LectorJson.getInt(m, "cantidad", 1);
                double tLib = LectorJson.getDouble(m, "tiempoLiberacion", 0.0);
                double plazo = LectorJson.getDouble(m, "plazoHoras", 4.0);
                config.pedidosIniciales.add(new Pedido(id, new NodoCuadricula(destId, x, y, TipoNodo.CLIENTE), cant, tLib, plazo));
            }
        }

        // 10. Disrupciones
        List<Object> lDis = LectorJson.getList(root, "disrupciones");
        config.disrupciones.clear();
        for (Object obj : lDis) {
            if (obj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) obj;
                config.disrupciones.add(m);
            }
        }

        return config;
    }

    public MapaCuadricula construirMapa() {
        return new MapaCuadricula(entorno.getAnchoMapa(), entorno.getAltoMapa());
    }

    public ContextoProblema construirContextoInicial(MapaCuadricula mapa) {
        ContextoProblema ctx = new ContextoProblema(0.0, mapa, operacion);
        ctx.agregarAlmacen(almacenCentral);
        for (NodoCuadricula ai : almacenesIntermedios) ctx.agregarAlmacen(ai);
        for (EstadoVehiculo ev : flota) ctx.agregarVehiculo(ev.copiar());
        for (Pedido p : pedidosIniciales) ctx.agregarPedido(p);
        return ctx;
    }

    public ConfiguracionEntorno getEntorno() { return entorno; }
    public ConfiguracionOperacion getOperacion() { return operacion; }
    public NodoCuadricula getAlmacenCentral() { return almacenCentral; }
    public List<NodoCuadricula> getAlmacenesIntermedios() { return almacenesIntermedios; }
    public Map<String, Integer> getCapacidadAlmacenes() { return capacidadAlmacenes; }
    public List<TipoVehiculo> getTiposVehiculo() { return tiposVehiculo; }
    public List<EstadoVehiculo> getFlota() { return flota; }
    public ConfiguracionALNS getAlns() { return alns; }
    public ConfiguracionSemaforos getSemaforos() { return semaforos; }
    public ConfiguracionSimulacion getSimulacion() { return simulacion; }
    public List<Pedido> getPedidosIniciales() { return pedidosIniciales; }
    public List<Map<String, Object>> getDisrupciones() { return disrupciones; }
}
