package com.paqrap.entrada;

import com.paqrap.modelo.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lector de archivos de ventas DDdHHhMMm:x,y,cliente,cantidad,sla. */
public final class LectorVentas {
    private static final Pattern HORA = Pattern.compile("^(\\d{2})d(\\d{2})h(\\d{2})m$");

    private LectorVentas() {}

    public static RegistroEntradas leer(Path archivoOCarpeta) throws IOException {
        RegistroEntradas registro = new RegistroEntradas();
        leer(archivoOCarpeta, registro);
        return registro;
    }

    public static void leer(Path archivoOCarpeta, RegistroEntradas registro) throws IOException {
        Objects.requireNonNull(archivoOCarpeta, "archivoOCarpeta");
        Objects.requireNonNull(registro, "registro");
        for (Path archivo : archivos(archivoOCarpeta)) {
            int numeroLinea = 0;
            try (BufferedReader br = Files.newBufferedReader(archivo, StandardCharsets.UTF_8)) {
                String linea;
                while ((linea = br.readLine()) != null) {
                    numeroLinea++;
                    linea = linea.trim();
                    if (linea.isEmpty() || linea.startsWith("#")) continue;
                    Pedido pedido = parsearLinea(linea, archivo.toString(), numeroLinea);
                    String id = pedido.getId();
                    int repeticion = 1;
                    while (registro.tienePedidoId(id)) {
                        repeticion++;
                        id = pedido.getId() + "#" + repeticion;
                    }
                    if (!id.equals(pedido.getId())) {
                        pedido = new Pedido(id, pedido.getDestino(), pedido.getCantidad(),
                                pedido.getTiempoLiberacion(), pedido.getPlazoHoras());
                    }
                    registro.registrarPedido(pedido);
                }
            }
            registro.incrementarArchivosVentas();
        }
    }

    public static Pedido parsearLinea(String linea) {
        return parsearLinea(linea, "<cadena>", 1);
    }

    private static Pedido parsearLinea(String linea, String fuente, int numeroLinea) {
        String[] partes = linea.split(":", 2);
        if (partes.length != 2) throw error(fuente, numeroLinea, linea, "se esperaba hora:datos");
        Matcher hora = HORA.matcher(partes[0].trim());
        if (!hora.matches()) throw error(fuente, numeroLinea, linea, "hora inválida; use DDdHHhMMm");
        int dia = entero(hora.group(1), fuente, numeroLinea, linea, "día");
        int horas = entero(hora.group(2), fuente, numeroLinea, linea, "hora");
        int minutos = entero(hora.group(3), fuente, numeroLinea, linea, "minutos");
        if (dia < 1 || horas > 23 || minutos > 59) {
            throw error(fuente, numeroLinea, linea, "día/hora/minutos fuera de rango");
        }

        String[] datos = partes[1].split(",", -1);
        if (datos.length != 5) throw error(fuente, numeroLinea, linea, "se esperaban x,y,cliente,cantidad,sla");
        int x = entero(datos[0], fuente, numeroLinea, linea, "x");
        int y = entero(datos[1], fuente, numeroLinea, linea, "y");
        String cliente = datos[2].trim();
        if (x < 0 || y < 0 || cliente.isEmpty()) {
            throw error(fuente, numeroLinea, linea, "coordenadas no negativas y cliente no vacío requeridos");
        }
        int cantidad = entero(datos[3], fuente, numeroLinea, linea, "cantidad");
        double sla = decimal(datos[4], fuente, numeroLinea, linea, "sla");
        if (cantidad <= 0 || !Double.isFinite(sla) || sla <= 0) {
            throw error(fuente, numeroLinea, linea, "cantidad y sla deben ser positivos");
        }
        double tiempo = (dia - 1) * 24.0 + horas + minutos / 60.0;
        return new Pedido(cliente, new NodoCuadricula(cliente, x, y, TipoNodo.CLIENTE), cantidad, tiempo, sla);
    }

    private static List<Path> archivos(Path entrada) throws IOException {
        if (!Files.exists(entrada)) throw new FileNotFoundException("No existe la entrada de ventas: " + entrada);
        if (Files.isRegularFile(entrada)) return List.of(entrada);
        try (var stream = Files.list(entrada)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static int entero(String valor, String fuente, int linea, String contenido, String campo) {
        try { return Integer.parseInt(valor.trim()); }
        catch (NumberFormatException e) { throw error(fuente, linea, contenido, campo + " entero inválido"); }
    }

    private static double decimal(String valor, String fuente, int linea, String contenido, String campo) {
        try { return Double.parseDouble(valor.trim()); }
        catch (NumberFormatException e) { throw error(fuente, linea, contenido, campo + " numérico inválido"); }
    }

    private static EntradaInvalidaException error(String fuente, int linea, String contenido, String detalle) {
        return new EntradaInvalidaException(fuente, linea, contenido, detalle);
    }
}
