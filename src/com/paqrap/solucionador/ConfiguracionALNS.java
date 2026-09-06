package com.paqrap.solucionador;

public class ConfiguracionALNS {
    private int maxIteraciones = 350;
    private int maxSinMejora = 90;
    private int intervaloActualizacion = 15;
    private int intervaloSPP = 20;
    private double factorReaccion = 0.2;
    private int minCantidadDestruccion = 1;
    private int maxCantidadDestruccion = 5;
    private double puntajeMejorGlobal = 10.0;
    private double puntajeMejorActual = 5.0;
    private double puntajeAceptado = 2.0;
    private double temperaturaAceptacionSA = 100.0;
    private double tasaEnfriamientoSA = 0.995;
    private double pesoDistanciaShaw = 1.0;
    private double pesoTiempoShaw = 2.0;
    private double epsilonMejoraRVND = 0.01;

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
    public void setMinCantidadDestruccion(int minCantidadDestruccion) { this.minCantidadDestruccion = minCantidadDestruccion; }

    public int getMaxCantidadDestruccion() { return maxCantidadDestruccion; }
    public void setMaxCantidadDestruccion(int maxCantidadDestruccion) { this.maxCantidadDestruccion = maxCantidadDestruccion; }

    public double getPuntajeMejorGlobal() { return puntajeMejorGlobal; }
    public void setPuntajeMejorGlobal(double puntajeMejorGlobal) { this.puntajeMejorGlobal = puntajeMejorGlobal; }

    public double getPuntajeMejorActual() { return puntajeMejorActual; }
    public void setPuntajeMejorActual(double puntajeMejorActual) { this.puntajeMejorActual = puntajeMejorActual; }

    public double getPuntajeAceptado() { return puntajeAceptado; }
    public void setPuntajeAceptado(double puntajeAceptado) { this.puntajeAceptado = puntajeAceptado; }

    public double getTemperaturaAceptacionSA() { return temperaturaAceptacionSA; }
    public void setTemperaturaAceptacionSA(double temperaturaAceptacionSA) { this.temperaturaAceptacionSA = temperaturaAceptacionSA; }

    public double getTasaEnfriamientoSA() { return tasaEnfriamientoSA; }
    public void setTasaEnfriamientoSA(double tasaEnfriamientoSA) { this.tasaEnfriamientoSA = tasaEnfriamientoSA; }

    public double getPesoDistanciaShaw() { return pesoDistanciaShaw; }
    public void setPesoDistanciaShaw(double pesoDistanciaShaw) { this.pesoDistanciaShaw = pesoDistanciaShaw; }

    public double getPesoTiempoShaw() { return pesoTiempoShaw; }
    public void setPesoTiempoShaw(double pesoTiempoShaw) { this.pesoTiempoShaw = pesoTiempoShaw; }

    public double getEpsilonMejoraRVND() { return epsilonMejoraRVND; }
    public void setEpsilonMejoraRVND(double epsilonMejoraRVND) { this.epsilonMejoraRVND = epsilonMejoraRVND; }
}
