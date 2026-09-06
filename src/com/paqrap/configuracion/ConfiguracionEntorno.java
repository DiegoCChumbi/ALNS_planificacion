package com.paqrap.configuracion;

public class ConfiguracionEntorno {
    private int anchoMapa = 30;
    private int altoMapa = 30;
    private double kmPorCuadra = 1.0;
    private boolean callesDobleSentido = true;
    private double penalizacionInalcanzable = 999999.0;

    public int getAnchoMapa() { return anchoMapa; }
    public void setAnchoMapa(int anchoMapa) { this.anchoMapa = anchoMapa; }

    public int getAltoMapa() { return altoMapa; }
    public void setAltoMapa(int altoMapa) { this.altoMapa = altoMapa; }

    public double getKmPorCuadra() { return kmPorCuadra; }
    public void setKmPorCuadra(double kmPorCuadra) { this.kmPorCuadra = kmPorCuadra; }

    public boolean isCallesDobleSentido() { return callesDobleSentido; }
    public void setCallesDobleSentido(boolean callesDobleSentido) { this.callesDobleSentido = callesDobleSentido; }

    public double getPenalizacionInalcanzable() { return penalizacionInalcanzable; }
    public void setPenalizacionInalcanzable(double penalizacionInalcanzable) { this.penalizacionInalcanzable = penalizacionInalcanzable; }
}
