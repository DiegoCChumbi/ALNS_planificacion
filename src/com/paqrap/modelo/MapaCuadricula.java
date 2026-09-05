package com.paqrap.modelo;

import java.util.*;

public class MapaCuadricula {
    private final int ancho;
    private final int alto;
    private final Set<String> aristasBloqueadas; // Clave de arista: "x1,y1-x2,y2" ordenada

    public MapaCuadricula(int ancho, int alto) {
        this.ancho = ancho;
        this.alto = alto;
        this.aristasBloqueadas = new HashSet<>();
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public void bloquearArista(int x1, int y1, int x2, int y2) {
        aristasBloqueadas.add(generarClaveArista(x1, y1, x2, y2));
    }

    public void desbloquearArista(int x1, int y1, int x2, int y2) {
        aristasBloqueadas.remove(generarClaveArista(x1, y1, x2, y2));
    }

    public boolean estaAristaBloqueada(int x1, int y1, int x2, int y2) {
        return aristasBloqueadas.contains(generarClaveArista(x1, y1, x2, y2));
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
        if (inicio.equals(destino)) return 0.0;
        if (aristasBloqueadas.isEmpty()) {
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
                    if (!estaAristaBloqueada(cx, cy, nx, ny)) {
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
        List<NodoCuadricula> camino = new ArrayList<>();
        if (inicio.equals(destino)) {
            camino.add(inicio);
            return camino;
        }

        if (aristasBloqueadas.isEmpty()) {
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
                    if (!estaAristaBloqueada(cx, cy, nx, ny)) {
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
}
