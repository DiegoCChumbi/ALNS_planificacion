package com.paqrap.solucionador;

public class ConfiguracionALNS {
    private int maxIteraciones = 200;
    private int maxSinMejora = 50;
    private int intervaloActualizacion = 10;
    private int intervaloSPP = 20;
    private double factorReaccion = 0.2;
    private int minCantidadDestruccion = 1;
    private int maxCantidadDestruccion = 4;
    private double puntajeMejorGlobal = 10.0;
    private double puntajeMejorActual = 5.0;
    private double puntajeAceptado = 2.0;

    public int getMaxIteraciones() { return maxIteraciones; }
    public void setMaxIteraciones(int maxIteraciones) { this.maxIteraciones = maxIteraciones; }

    public int getMaxSinMejora() { return maxSinMejora; }
    public void setMaxSinMejora(int maxSinMejora) { this.maxSinMejora = maxSinMejora; }

    public int getIntervaloActualizacion() { return intervaloActualizacion; }
    public void setIntervaloActualizacion(int intervaloActualizacion) { this.intervaloActualizacion = intervaloActualizacion; }

    public int getIntervaloSPP() { return intervaloSPP; }
    public void setIntervaloSPP(int intervaloSPP) { this.intervaloSPP = intervaloSPP; }

    public double getFactorReaccion() { return factorReaccion; }
    public void setFactorReaccion(double factorReaccion) { this.factorReaccion = factorReaccion; }

    public int getMinCantidadDestruccion() { return minCantidadDestruccion; }
    public int getMaxCantidadDestruccion() { return maxCantidadDestruccion; }

    public double getPuntajeMejorGlobal() { return puntajeMejorGlobal; }
    public double getPuntajeMejorActual() { return puntajeMejorActual; }
    public double getPuntajeAceptado() { return puntajeAceptado; }
}
