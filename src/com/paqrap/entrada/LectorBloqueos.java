package com.paqrap.entrada;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Lector de bloqueos temporales inicio-fin:x1,y1,x2,y2,... */
public final class LectorBloqueos {
    private static final Pattern HORA = Pattern.compile("^(\\d{2})d(\\d{2})h(\\d{2})m$");

    private LectorBloqueos() {}

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
                    registro.registrarBloqueo(parsearLinea(linea, archivo.toString(), numeroLinea));
                }
            }
            registro.incrementarArchivosBloqueos();
        }
    }

    public static BloqueoTemporal parsearLinea(String linea) {
        return parsearLinea(linea, "<cadena>", 1);
    }

    private static BloqueoTemporal parsearLinea(String linea, String fuente, int numeroLinea) {
        String[] partes = linea.split(":", 2);
        if (partes.length != 2) throw error(fuente, numeroLinea, linea, "se esperaba inicio-fin:coordenadas");
        String[] intervalo = partes[0].split("-", 2);
        if (intervalo.length != 2) throw error(fuente, numeroLinea, linea, "intervalo inválido");
        double inicio = parsearHora(intervalo[0].trim(), fuente, numeroLinea, linea);
        double fin = parsearHora(intervalo[1].trim(), fuente, numeroLinea, linea);
        if (fin <= inicio) throw error(fuente, numeroLinea, linea, "el fin debe ser posterior al inicio");

        String[] valores = partes[1].split(",", -1);
        if (valores.length < 4 || valores.length % 2 != 0) {
            throw error(fuente, numeroLinea, linea, "se requieren al menos dos puntos (x,y) y pares completos");
        }
        List<BloqueoTemporal.Arista> aristas = new ArrayList<>();
        int xAnterior = entero(valores[0], fuente, numeroLinea, linea, "x");
        int yAnterior = entero(valores[1], fuente, numeroLinea, linea, "y");
        validarCoordenada(xAnterior, yAnterior, fuente, numeroLinea, linea);
        for (int i = 2; i < valores.length; i += 2) {
            int x = entero(valores[i], fuente, numeroLinea, linea, "x");
            int y = entero(valores[i + 1], fuente, numeroLinea, linea, "y");
            validarCoordenada(x, y, fuente, numeroLinea, linea);
            if (x == xAnterior && y == yAnterior) {
                throw error(fuente, numeroLinea, linea, "puntos consecutivos iguales");
            }
            if (x != xAnterior && y != yAnterior) {
                throw error(fuente, numeroLinea, linea, "cada arista debe ser horizontal o vertical");
            }
            aristas.add(new BloqueoTemporal.Arista(xAnterior, yAnterior, x, y));
            xAnterior = x;
            yAnterior = y;
        }
        return new BloqueoTemporal(inicio, fin, aristas);
    }

    private static double parsearHora(String valor, String fuente, int linea, String contenido) {
        Matcher m = HORA.matcher(valor);
        if (!m.matches()) throw error(fuente, linea, contenido, "hora inválida; use DDdHHhMMm");
        int dia = entero(m.group(1), fuente, linea, contenido, "día");
        int hora = entero(m.group(2), fuente, linea, contenido, "hora");
        int minuto = entero(m.group(3), fuente, linea, contenido, "minutos");
        if (dia < 1 || hora > 23 || minuto > 59) throw error(fuente, linea, contenido, "día/hora/minutos fuera de rango");
        return (dia - 1) * 24.0 + hora + minuto / 60.0;
    }

    private static void validarCoordenada(int x, int y, String fuente, int linea, String contenido) {
        if (x < 0 || y < 0) throw error(fuente, linea, contenido, "las coordenadas no pueden ser negativas");
    }

    private static List<Path> archivos(Path entrada) throws IOException {
        if (!Files.exists(entrada)) throw new FileNotFoundException("No existe la entrada de bloqueos: " + entrada);
        if (Files.isRegularFile(entrada)) return List.of(entrada);
        try (var stream = Files.list(entrada)) {
            return stream.filter(Files::isRegularFile).sorted().toList();
        }
    }

    private static int entero(String valor, String fuente, int linea, String contenido, String campo) {
        try { return Integer.parseInt(valor.trim()); }
        catch (NumberFormatException e) { throw error(fuente, linea, contenido, campo + " entero inválido"); }
    }

    private static EntradaInvalidaException error(String fuente, int linea, String contenido, String detalle) {
        return new EntradaInvalidaException(fuente, linea, contenido, detalle);
    }
}
