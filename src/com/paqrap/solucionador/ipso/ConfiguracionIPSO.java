package com.paqrap.solucionador.ipso;

public class ConfiguracionIPSO {
    private int tamanoEnjambre = 30;
    private int maxIteraciones = 120;
    private double c1Cognitivo = 1.5;
    private double c2Social = 1.5;
    private double probabilidadCruceOX = 0.35;
    private double probabilidadMutacionHeuristica = 0.15;
    private double umbralEstancamiento = 0.0001;

    public int getTamanoEnjambre() { return tamanoEnjambre; }
    public void setTamanoEnjambre(int tamanoEnjambre) { this.tamanoEnjambre = tamanoEnjambre; }

    public int getMaxIteraciones() { return maxIteraciones; }
    public void setMaxIteraciones(int maxIteraciones) { this.maxIteraciones = maxIteraciones; }

    public double getC1Cognitivo() { return c1Cognitivo; }
    public void setC1Cognitivo(double c1Cognitivo) { this.c1Cognitivo = c1Cognitivo; }

    public double getC2Social() { return c2Social; }
    public void setC2Social(double c2Social) { this.c2Social = c2Social; }

    public double getProbabilidadCruceOX() { return probabilidadCruceOX; }
    public void setProbabilidadCruceOX(double probabilidadCruceOX) { this.probabilidadCruceOX = probabilidadCruceOX; }

    public double getProbabilidadMutacionHeuristica() { return probabilidadMutacionHeuristica; }
    public void setProbabilidadMutacionHeuristica(double probabilidadMutacionHeuristica) { this.probabilidadMutacionHeuristica = probabilidadMutacionHeuristica; }

    public double getUmbralEstancamiento() { return umbralEstancamiento; }
    public void setUmbralEstancamiento(double umbralEstancamiento) { this.umbralEstancamiento = umbralEstancamiento; }
}
