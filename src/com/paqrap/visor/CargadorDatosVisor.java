package com.paqrap.visor;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.configuracion.LectorJson;
import com.paqrap.simulador.EventoSimulacion;
import com.paqrap.simulador.TipoEvento;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Carga la configuración del sistema y la secuencia de eventos de simulación
 * desde los archivos JSON correspondientes.
 */
public class CargadorDatosVisor {

    public static List<EventoSimulacion> cargarEventosDesdeJson(File archivoJson, double horaInicioBase) throws Exception {
        List<EventoSimulacion> lista = new ArrayList<>();
        if (!archivoJson.exists()) {
            return lista;
        }

        Map<String, Object> raiz = LectorJson.parseObjeto(archivoJson);
        List<Object> listaObj = LectorJson.getList(raiz, "eventos");

        for (Object obj : listaObj) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> mapEv = (Map<String, Object>) obj;

            double tiempoHoras = LectorJson.getDouble(mapEv, "tiempoHoras", 0.0);
            String tipoStr = LectorJson.getString(mapEv, "tipo", "MOVIMIENTO_TRAMO");
            TipoEvento tipo;
            try {
                tipo = TipoEvento.valueOf(tipoStr);
            } catch (Exception e) {
                tipo = TipoEvento.MOVIMIENTO_TRAMO;
            }

            String vehiculoId = LectorJson.getString(mapEv, "vehiculoId", null);
            if ("null".equalsIgnoreCase(vehiculoId)) vehiculoId = null;

            String pedidoId = LectorJson.getString(mapEv, "pedidoId", null);
            if ("null".equalsIgnoreCase(pedidoId)) pedidoId = null;

            int x = LectorJson.getInt(mapEv, "x", -1);
            int y = LectorJson.getInt(mapEv, "y", -1);
            String descripcion = LectorJson.getString(mapEv, "descripcion", "");
            String datosAdicionales = LectorJson.getString(mapEv, "datosAdicionales", "");

            lista.add(new EventoSimulacion(
                    tiempoHoras,
                    horaInicioBase,
                    tipo,
                    vehiculoId,
                    pedidoId,
                    x,
                    y,
                    descripcion,
                    datosAdicionales
            ));
        }

        return lista;
    }
}
