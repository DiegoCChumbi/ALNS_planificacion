package com.paqrap.configuracion;

public class ConfiguracionSimulacion {
    private String directorioLogs = "logs";
    private String archivoLogTexto = "simulacion_movimientos.log";
    private String archivoLogJson = "simulacion_movimientos.json";
    private boolean imprimirEnConsola = false;

    public String getDirectorioLogs() { return directorioLogs; }
    public void setDirectorioLogs(String directorioLogs) { this.directorioLogs = directorioLogs; }

    public String getArchivoLogTexto() { return archivoLogTexto; }
    public void setArchivoLogTexto(String archivoLogTexto) { this.archivoLogTexto = archivoLogTexto; }

    public String getArchivoLogJson() { return archivoLogJson; }
    public void setArchivoLogJson(String archivoLogJson) { this.archivoLogJson = archivoLogJson; }

    public boolean isImprimirEnConsola() { return imprimirEnConsola; }
    public void setImprimirEnConsola(boolean imprimirEnConsola) { this.imprimirEnConsola = imprimirEnConsola; }
}
