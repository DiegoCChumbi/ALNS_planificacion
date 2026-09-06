package com.paqrap.visor;

/**
 * Representa una calle bloqueada en la cuadrícula vial.
 */
public class TramoBloqueado {
    private final int x1;
    private final int y1;
    private final int x2;
    private final int y2;
    private final double tiempoInicio;
    private final String motivo;

    public TramoBloqueado(int x1, int y1, int x2, int y2, double tiempoInicio, String motivo) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.tiempoInicio = tiempoInicio;
        this.motivo = (motivo != null) ? motivo : "Bloqueo vial";
    }

    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public double getTiempoInicio() { return tiempoInicio; }
    public String getMotivo() { return motivo; }

    public boolean conecta(int ax, int ay, int bx, int by) {
        return (x1 == ax && y1 == ay && x2 == bx && y2 == by) ||
               (x1 == bx && y1 == by && x2 == ax && y2 == ay);
    }
}
