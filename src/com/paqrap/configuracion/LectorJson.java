package com.paqrap.configuracion;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Parser JSON ligero y autocontenido en Java puro (sin dependencias externas).
 * Parsea objetos y listas JSON a Map y List estándar de Java.
 */
public class LectorJson {

    public static Object parse(File archivo) throws IOException {
        String contenido = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
        return parse(contenido);
    }

    public static Object parse(String json) {
        if (json == null) return null;
        return new Parser(json.trim()).parseValor();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObjeto(File archivo) throws IOException {
        Object res = parse(archivo);
        if (res instanceof Map) {
            return (Map<String, Object>) res;
        }
        throw new IllegalArgumentException("El JSON raíz no es un objeto.");
    }

    // Métodos utilitarios de extracción tipada
    public static String getString(Map<String, Object> map, String clave, String porDefecto) {
        if (map == null || !map.containsKey(clave) || map.get(clave) == null) return porDefecto;
        return map.get(clave).toString();
    }

    public static double getDouble(Map<String, Object> map, String clave, double porDefecto) {
        if (map == null || !map.containsKey(clave) || map.get(clave) == null) return porDefecto;
        Object val = map.get(clave);
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public static int getInt(Map<String, Object> map, String clave, int porDefecto) {
        if (map == null || !map.containsKey(clave) || map.get(clave) == null) return porDefecto;
        Object val = map.get(clave);
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public static boolean getBoolean(Map<String, Object> map, String clave, boolean porDefecto) {
        if (map == null || !map.containsKey(clave) || map.get(clave) == null) return porDefecto;
        Object val = map.get(clave);
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(val.toString());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> map, String clave) {
        if (map == null || !map.containsKey(clave)) return Collections.emptyMap();
        Object val = map.get(clave);
        if (val instanceof Map) return (Map<String, Object>) val;
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> getList(Map<String, Object> map, String clave) {
        if (map == null || !map.containsKey(clave)) return Collections.emptyList();
        Object val = map.get(clave);
        if (val instanceof List) return (List<Object>) val;
        return Collections.emptyList();
    }

    private static class Parser {
        private final String src;
        private int idx = 0;

        Parser(String src) {
            this.src = src;
        }

        Object parseValor() {
            saltarEspacios();
            if (idx >= src.length()) return null;

            char c = src.charAt(idx);
            if (c == '{') return parseObjeto();
            if (c == '[') return parseArreglo();
            if (c == '"') return parseCadena();
            if (c == 't' || c == 'f') return parseBooleano();
            if (c == 'n') return parseNull();
            if (c == '-' || Character.isDigit(c)) return parseNumero();

            throw new IllegalArgumentException("Carácter inesperado en JSON pos " + idx + ": " + c);
        }

        private Map<String, Object> parseObjeto() {
            Map<String, Object> map = new LinkedHashMap<>();
            idx++; // salta '{'
            saltarEspacios();

            if (idx < src.length() && src.charAt(idx) == '}') {
                idx++;
                return map;
            }

            while (idx < src.length()) {
                saltarEspacios();
                String clave = parseCadena();
                saltarEspacios();

                if (idx >= src.length() || src.charAt(idx) != ':') {
                    throw new IllegalArgumentException("Se esperaba ':' tras clave " + clave + " en pos " + idx);
                }
                idx++; // salta ':'

                Object valor = parseValor();
                map.put(clave, valor);

                saltarEspacios();
                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == '}') {
                    idx++;
                    break;
                } else {
                    throw new IllegalArgumentException("Se esperaba ',' o '}' en pos " + idx);
                }
            }
            return map;
        }

        private List<Object> parseArreglo() {
            List<Object> list = new ArrayList<>();
            idx++; // salta '['
            saltarEspacios();

            if (idx < src.length() && src.charAt(idx) == ']') {
                idx++;
                return list;
            }

            while (idx < src.length()) {
                Object val = parseValor();
                list.add(val);
                saltarEspacios();

                if (idx < src.length() && src.charAt(idx) == ',') {
                    idx++;
                } else if (idx < src.length() && src.charAt(idx) == ']') {
                    idx++;
                    break;
                } else {
                    throw new IllegalArgumentException("Se esperaba ',' o ']' en arreglo en pos " + idx);
                }
            }
            return list;
        }

        private String parseCadena() {
            if (src.charAt(idx) != '"') throw new IllegalArgumentException("Se esperaba '\"' en pos " + idx);
            idx++;
            StringBuilder sb = new StringBuilder();

            while (idx < src.length()) {
                char c = src.charAt(idx++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (idx >= src.length()) break;
                    char esc = src.charAt(idx++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (idx + 4 <= src.length()) {
                                String hex = src.substring(idx, idx + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                idx += 4;
                            }
                            break;
                        default: sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw new IllegalArgumentException("Cadena no terminada en pos " + idx);
        }

        private Boolean parseBooleano() {
            if (src.startsWith("true", idx)) {
                idx += 4;
                return Boolean.TRUE;
            }
            if (src.startsWith("false", idx)) {
                idx += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Booleano inválido en pos " + idx);
        }

        private Object parseNull() {
            if (src.startsWith("null", idx)) {
                idx += 4;
                return null;
            }
            throw new IllegalArgumentException("Token nulo inválido en pos " + idx);
        }

        private Number parseNumero() {
            int inicio = idx;
            if (src.charAt(idx) == '-') idx++;
            while (idx < src.length() && (Character.isDigit(src.charAt(idx)) || src.charAt(idx) == '.' || src.charAt(idx) == 'e' || src.charAt(idx) == 'E' || src.charAt(idx) == '+')) {
                idx++;
            }
            String numStr = src.substring(inicio, idx);
            if (numStr.contains(".") || numStr.contains("e") || numStr.contains("E")) {
                return Double.parseDouble(numStr);
            }
            try {
                return Long.parseLong(numStr);
            } catch (NumberFormatException e) {
                return Double.parseDouble(numStr);
            }
        }

        private void saltarEspacios() {
            while (idx < src.length() && Character.isWhitespace(src.charAt(idx))) {
                idx++;
            }
        }
    }
}
