package com.paqrap.modelo;

import com.paqrap.entrada.BloqueoTemporal;
import java.util.*;

public class MapaCuadricula {
    private final int ancho;
    private final int alto;
    private final Set<String> aristasBloqueadas; // Clave de arista: "x1,y1-x2,y2" ordenada
    private final List<BloqueoTemporal> bloqueosTemporales;
    private final Map<String, List<BloqueoTemporal>> bloqueosPorArista;
    private final Set<String> aristasTemporalesActivas;
    private double tiempoActual;

    public MapaCuadricula(int ancho, int alto) {
        if (ancho <= 0 || alto <= 0) {
            throw new IllegalArgumentException("Las dimensiones del mapa deben ser positivas");
        }
        this.ancho = ancho;
        this.alto = alto;
        this.aristasBloqueadas = new HashSet<>();
        this.bloqueosTemporales = new ArrayList<>();
        this.bloqueosPorArista = new HashMap<>();
        this.aristasTemporalesActivas = new HashSet<>();
        this.tiempoActual = 0.0;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public void bloquearArista(int x1, int y1, int x2, int y2) {
        validarArista(x1, y1, x2, y2);
        aristasBloqueadas.add(generarClaveArista(x1, y1, x2, y2));
    }

    public void desbloquearArista(int x1, int y1, int x2, int y2) {
        validarArista(x1, y1, x2, y2);
        aristasBloqueadas.remove(generarClaveArista(x1, y1, x2, y2));
    }

    public boolean estaAristaBloqueada(int x1, int y1, int x2, int y2) {
        validarArista(x1, y1, x2, y2);
        String clave = generarClaveArista(x1, y1, x2, y2);
        return aristasBloqueadas.contains(clave) || aristasTemporalesActivas.contains(clave);
    }

    public boolean estaAristaBloqueada(int x1, int y1, int x2, int y2, double tiempoHoras) {
        validarArista(x1, y1, x2, y2);
        if (aristasBloqueadas.contains(generarClaveArista(x1, y1, x2, y2))) return true;
        List<BloqueoTemporal> bloqueos = bloqueosPorArista.get(generarClaveArista(x1, y1, x2, y2));
        return bloqueos != null && bloqueos.stream().anyMatch(b -> b.estaActivo(tiempoHoras));
    }

    public void agregarBloqueoTemporal(BloqueoTemporal bloqueo) {
        Objects.requireNonNull(bloqueo, "bloqueo");
        for (BloqueoTemporal.Arista arista : bloqueo.getAristas()) {
            validarCoordenada(arista.getX1(), arista.getY1());
            validarCoordenada(arista.getX2(), arista.getY2());
            if (arista.getX1() != arista.getX2() && arista.getY1() != arista.getY2()) {
                throw new IllegalArgumentException("Los bloqueos temporales deben ser horizontales o verticales");
            }

        }
        bloqueosTemporales.add(bloqueo);
        for (BloqueoTemporal.Arista arista : bloqueo.getAristas()) {
            if (arista.getY1() == arista.getY2()) {
                int paso = Integer.compare(arista.getX2(), arista.getX1());
                for (int x = arista.getX1(); x != arista.getX2(); x += paso) {
                    indexarBloqueo(bloqueo, generarClaveArista(x, arista.getY1(), x + paso, arista.getY1()));
                }
            } else {
                int paso = Integer.compare(arista.getY2(), arista.getY1());
                for (int y = arista.getY1(); y != arista.getY2(); y += paso) {
                    indexarBloqueo(bloqueo, generarClaveArista(arista.getX1(), y, arista.getX1(), y + paso));
                }
            }
        }
        actualizarBloqueosTemporales(tiempoActual);
    }

    public void actualizarBloqueosTemporales(double tiempoHoras) {
        if (!Double.isFinite(tiempoHoras) || tiempoHoras < 0) {
            throw new IllegalArgumentException("El tiempo del mapa debe ser finito y no negativo");
        }
        tiempoActual = tiempoHoras;
        aristasTemporalesActivas.clear();
        for (BloqueoTemporal bloqueo : bloqueosTemporales) {
            if (!bloqueo.estaActivo(tiempoHoras)) continue;
            for (BloqueoTemporal.Arista arista : bloqueo.getAristas()) {
                if (arista.getY1() == arista.getY2()) {
                    int paso = Integer.compare(arista.getX2(), arista.getX1());
                    for (int x = arista.getX1(); x != arista.getX2(); x += paso) {
                        aristasTemporalesActivas.add(generarClaveArista(x, arista.getY1(), x + paso, arista.getY1()));
                    }
                } else {
                    int paso = Integer.compare(arista.getY2(), arista.getY1());
                    for (int y = arista.getY1(); y != arista.getY2(); y += paso) {
                        aristasTemporalesActivas.add(generarClaveArista(arista.getX1(), y, arista.getX1(), y + paso));
                    }
                }

            }
        }
    }

    public void activarBloqueosTemporales(double tiempoHoras) {
        actualizarBloqueosTemporales(tiempoHoras);
    }

    public void desactivarBloqueosTemporales(double tiempoHoras) {
        actualizarBloqueosTemporales(tiempoHoras);
    }

    public double getTiempoActual() {
        return tiempoActual;
    }

    private String generarClaveArista(int x1, int y1, int x2, int y2) {
        if (x1 < x2 || (x1 == x2 && y1 <= y2)) {
            return x1 + "," + y1 + "-" + x2 + "," + y2;
        } else {
            return x2 + "," + y2 + "-" + x1 + "," + y1;
        }
    }

    public double getDistanciaManhattan(NodoCuadricula n1, NodoCuadricula n2) {
        return Math.abs(n1.getX() - n2.getX()) + Math.abs(n1.getY() - n2.getY());
    }

    public double getDistanciaMasCorta(NodoCuadricula inicio, NodoCuadricula destino) {
        return getDistanciaMasCorta(inicio, destino, tiempoActual);
    }

    public double getDistanciaMasCorta(NodoCuadricula inicio, NodoCuadricula destino, double tiempoHoras) {
        validarNodo(inicio);
        validarNodo(destino);
        if (inicio.equals(destino)) return 0.0;
        if (aristasBloqueadas.isEmpty() && bloqueosTemporales.stream().noneMatch(b -> b.estaActivo(tiempoHoras))) {
            return getDistanciaManhattan(inicio, destino);
        }

        // Búsqueda en anchura (BFS) sobre la cuadrícula
        Queue<int[]> cola = new LinkedList<>();
        Map<String, Integer> visitados = new HashMap<>();

        cola.add(new int[]{inicio.getX(), inicio.getY()});
        visitados.put(inicio.getX() + "," + inicio.getY(), 0);

        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int cx = actual[0];
            int cy = actual[1];
            int dist = visitados.get(cx + "," + cy);

            if (cx == destino.getX() && cy == destino.getY()) {
                return (double) dist;
            }

            for (int[] dir : direcciones) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                if (nx >= 0 && nx < ancho && ny >= 0 && ny < alto) {
                    if (!estaAristaBloqueada(cx, cy, nx, ny, tiempoHoras)) {
                        String clave = nx + "," + ny;
                        if (!visitados.containsKey(clave)) {
                            visitados.put(clave, dist + 1);
                            cola.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }

        return 999999.0;
    }

    public List<NodoCuadricula> getCaminoMasCorto(NodoCuadricula inicio, NodoCuadricula destino) {
        return getCaminoMasCorto(inicio, destino, tiempoActual);
    }

    public List<NodoCuadricula> getCaminoMasCorto(NodoCuadricula inicio, NodoCuadricula destino, double tiempoHoras) {
        validarNodo(inicio);
        validarNodo(destino);
        List<NodoCuadricula> camino = new ArrayList<>();
        if (inicio.equals(destino)) {
            camino.add(inicio);
            return camino;
        }

        if (aristasBloqueadas.isEmpty() && bloqueosTemporales.stream().noneMatch(b -> b.estaActivo(tiempoHoras))) {
            int cx = inicio.getX();
            int cy = inicio.getY();
            camino.add(inicio);

            int stepX = Integer.compare(destino.getX(), cx);
            while (cx != destino.getX()) {
                cx += stepX;
                TipoNodo tipo = (cx == destino.getX() && cy == destino.getY()) ? destino.getTipo() : TipoNodo.CLIENTE;
                String id = (cx == destino.getX() && cy == destino.getY()) ? destino.getId() : ("(" + cx + "," + cy + ")");
                camino.add(new NodoCuadricula(id, cx, cy, tipo));
            }

            int stepY = Integer.compare(destino.getY(), cy);
            while (cy != destino.getY()) {
                cy += stepY;
                TipoNodo tipo = (cx == destino.getX() && cy == destino.getY()) ? destino.getTipo() : TipoNodo.CLIENTE;
                String id = (cx == destino.getX() && cy == destino.getY()) ? destino.getId() : ("(" + cx + "," + cy + ")");
                camino.add(new NodoCuadricula(id, cx, cy, tipo));
            }
            return camino;
        }

        // BFS con reconstrucción de camino
        Queue<int[]> cola = new LinkedList<>();
        Map<String, int[]> padre = new HashMap<>();

        cola.add(new int[]{inicio.getX(), inicio.getY()});
        padre.put(inicio.getX() + "," + inicio.getY(), null);

        int[][] direcciones = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        boolean encontrado = false;

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            int cx = actual[0];
            int cy = actual[1];

            if (cx == destino.getX() && cy == destino.getY()) {
                encontrado = true;
                break;
            }

            for (int[] dir : direcciones) {
                int nx = cx + dir[0];
                int ny = cy + dir[1];

                if (nx >= 0 && nx < ancho && ny >= 0 && ny < alto) {
                    if (!estaAristaBloqueada(cx, cy, nx, ny, tiempoHoras)) {
                        String clave = nx + "," + ny;
                        if (!padre.containsKey(clave)) {
                            padre.put(clave, new int[]{cx, cy});
                            cola.add(new int[]{nx, ny});
                        }
                    }
                }
            }
        }

        if (!encontrado) {
            return Collections.emptyList();
        }

        List<NodoCuadricula> recorridoInvertido = new ArrayList<>();
        int currX = destino.getX();
        int currY = destino.getY();

        while (true) {
            TipoNodo tipo = (currX == inicio.getX() && currY == inicio.getY()) ? inicio.getTipo() :
                            (currX == destino.getX() && currY == destino.getY()) ? destino.getTipo() : TipoNodo.CLIENTE;
            String id = (currX == inicio.getX() && currY == inicio.getY()) ? inicio.getId() :
                        (currX == destino.getX() && currY == destino.getY()) ? destino.getId() : ("(" + currX + "," + currY + ")");
            recorridoInvertido.add(new NodoCuadricula(id, currX, currY, tipo));

            int[] p = padre.get(currX + "," + currY);
            if (p == null) break;
            currX = p[0];
            currY = p[1];
        }

        Collections.reverse(recorridoInvertido);
        return recorridoInvertido;
    }

    public List<BloqueoTemporal> getBloqueosTemporales() {
        return Collections.unmodifiableList(bloqueosTemporales);
    }

    private void validarNodo(NodoCuadricula nodo) {
        if (nodo == null) throw new IllegalArgumentException("El nodo no puede ser nulo");
        validarCoordenada(nodo.getX(), nodo.getY());
    }

    private void validarArista(int x1, int y1, int x2, int y2) {
        validarCoordenada(x1, y1);
        validarCoordenada(x2, y2);
        if (Math.abs(x1 - x2) + Math.abs(y1 - y2) != 1) {
            throw new IllegalArgumentException("Las aristas del mapa deben unir esquinas adyacentes");
        }
    }

    private void validarCoordenada(int x, int y) {
        if (x < 0 || x >= ancho || y < 0 || y >= alto) {
            throw new IllegalArgumentException(String.format(
                    "Coordenada (%d,%d) fuera del mapa %dx%d", x, y, ancho, alto));
        }
    }

    private void indexarBloqueo(BloqueoTemporal bloqueo, String clave) {
        bloqueosPorArista.computeIfAbsent(clave, k -> new ArrayList<>()).add(bloqueo);
    }
}
