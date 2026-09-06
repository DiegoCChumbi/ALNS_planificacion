package com.paqrap.configuracion;

public class ConfiguracionOperacion {
    private double duracionTurnoHoras = 8.0;
    private double horaInicioTurno = 7.0;
    private double tiempoServicioClienteHoras = 1.0;
    private double duracionRefrigerioHoras = 1.0;
    private double margenRefrigerioHoras = 1.0;
    private double tiempoCargaAlmacenHoras = 0.0;
    private double penalizacionPedidoNoAsignado = 10000.0;

    public double getDuracionTurnoHoras() { return duracionTurnoHoras; }
    public void setDuracionTurnoHoras(double duracionTurnoHoras) { this.duracionTurnoHoras = duracionTurnoHoras; }

    public double getHoraInicioTurno() { return horaInicioTurno; }
    public void setHoraInicioTurno(double horaInicioTurno) { this.horaInicioTurno = horaInicioTurno; }

    public double getTiempoServicioClienteHoras() { return tiempoServicioClienteHoras; }
    public void setTiempoServicioClienteHoras(double tiempoServicioClienteHoras) { this.tiempoServicioClienteHoras = tiempoServicioClienteHoras; }

    public double getDuracionRefrigerioHoras() { return duracionRefrigerioHoras; }
    public void setDuracionRefrigerioHoras(double duracionRefrigerioHoras) { this.duracionRefrigerioHoras = duracionRefrigerioHoras; }

    public double getMargenRefrigerioHoras() { return margenRefrigerioHoras; }
    public void setMargenRefrigerioHoras(double margenRefrigerioHoras) { this.margenRefrigerioHoras = margenRefrigerioHoras; }

    public double getTiempoCargaAlmacenHoras() { return tiempoCargaAlmacenHoras; }
    public void setTiempoCargaAlmacenHoras(double tiempoCargaAlmacenHoras) { this.tiempoCargaAlmacenHoras = tiempoCargaAlmacenHoras; }

    public double getPenalizacionPedidoNoAsignado() { return penalizacionPedidoNoAsignado; }
    public void setPenalizacionPedidoNoAsignado(double penalizacionPedidoNoAsignado) { this.penalizacionPedidoNoAsignado = penalizacionPedidoNoAsignado; }
}
