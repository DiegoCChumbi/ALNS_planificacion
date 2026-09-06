package com.paqrap.configuracion;

public class ConfiguracionSemaforos {
    private double holguraVerdeHoras = 2.0;
    private double holguraAmbarHoras = 0.5;

    public double getHolguraVerdeHoras() { return holguraVerdeHoras; }
    public void setHolguraVerdeHoras(double holguraVerdeHoras) { this.holguraVerdeHoras = holguraVerdeHoras; }

    public double getHolguraAmbarHoras() { return holguraAmbarHoras; }
    public void setHolguraAmbarHoras(double holguraAmbarHoras) { this.holguraAmbarHoras = holguraAmbarHoras; }

    public String determinarColor(double holguraHoras) {
        if (holguraHoras >= holguraVerdeHoras) {
            return "VERDE";
        } else if (holguraHoras >= holguraAmbarHoras) {
            return "AMBAR";
        } else {
            return "ROJO";
        }
    }
}
