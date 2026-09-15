package com.paqrap.entrada;

import java.util.List;

/**
 * Bloqueo de una o más aristas durante un intervalo absoluto de horas.
 */
public final class BloqueoTemporal {
    public static final class Arista {
        private final int x1;
        private final int y1;
        private final int x2;
        private final int y2;

        public Arista(int x1, int y1, int x2, int y2) {
            if (x1 == x2 && y1 == y2) {
                throw new IllegalArgumentException("Una arista no puede tener extremos iguales");
            }
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        public int getX1() { return x1; }
        public int getY1() { return y1; }
        public int getX2() { return x2; }
        public int getY2() { return y2; }

        public boolean cubre(int ax1, int ay1, int ax2, int ay2) {
            boolean horizontal = y1 == y2 && ay1 == ay2 && y1 == ay1
                    && entre(ax1, x1, x2) && entre(ax2, x1, x2)
                    && Math.abs(ax1 - ax2) == 1;
            boolean vertical = x1 == x2 && ax1 == ax2 && x1 == ax1
                    && entre(ay1, y1, y2) && entre(ay2, y1, y2)
                    && Math.abs(ay1 - ay2) == 1;
            return horizontal || vertical;
        }

        private static boolean entre(int valor, int extremo1, int extremo2) {
            return valor >= Math.min(extremo1, extremo2) && valor <= Math.max(extremo1, extremo2);
        }
    }

    private final double inicioHoras;
    private final double finHoras;
    private final List<Arista> aristas;

    public BloqueoTemporal(double inicioHoras, double finHoras, List<Arista> aristas) {
        if (!Double.isFinite(inicioHoras) || !Double.isFinite(finHoras) || inicioHoras < 0 || finHoras <= inicioHoras) {
            throw new IllegalArgumentException("El intervalo de bloqueo debe ser finito y cumplir 0 <= inicio < fin");
        }
        if (aristas == null || aristas.isEmpty()) {
            throw new IllegalArgumentException("Un bloqueo debe contener al menos una arista");
        }
        this.inicioHoras = inicioHoras;
        this.finHoras = finHoras;
        this.aristas = List.copyOf(aristas);
    }

    public double getInicioHoras() { return inicioHoras; }
    public double getFinHoras() { return finHoras; }
    public List<Arista> getAristas() { return aristas; }

    public boolean estaActivo(double tiempoHoras) {
        return tiempoHoras >= inicioHoras && tiempoHoras < finHoras;
    }

    public boolean cubreArista(int x1, int y1, int x2, int y2) {
        return aristas.stream().anyMatch(a -> a.cubre(x1, y1, x2, y2));
    }
}
