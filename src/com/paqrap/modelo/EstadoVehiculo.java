package com.paqrap.modelo;

public class EstadoVehiculo {
    private final String id;
    private final TipoVehiculo tipo;
    private final NodoCuadricula almacenBase;
    private NodoCuadricula ubicacionActual;
    private double marcaTiempoActual;
    private int capacidadDisponible;
    private double tiempoInicioTurno;
    private boolean refrigerioTomado;

    public EstadoVehiculo(String id, TipoVehiculo tipo, NodoCuadricula ubicacionInicial, double marcaTiempoActual, double tiempoInicioTurno) {
        this(id, tipo, ubicacionInicial, ubicacionInicial, marcaTiempoActual, tiempoInicioTurno);
    }

    public EstadoVehiculo(String id, TipoVehiculo tipo, NodoCuadricula ubicacionActual, NodoCuadricula almacenBase, double marcaTiempoActual, double tiempoInicioTurno) {
        this.id = id;
        this.tipo = tipo;
        this.ubicacionActual = ubicacionActual;
        this.almacenBase = almacenBase;
        this.marcaTiempoActual = marcaTiempoActual;
        this.capacidadDisponible = tipo.getCapacidad();
        this.tiempoInicioTurno = tiempoInicioTurno;
        this.refrigerioTomado = false;
    }

    public EstadoVehiculo copiar() {
        EstadoVehiculo ev = new EstadoVehiculo(id, tipo, ubicacionActual, almacenBase, marcaTiempoActual, tiempoInicioTurno);
        ev.capacidadDisponible = this.capacidadDisponible;
        ev.refrigerioTomado = this.refrigerioTomado;
        return ev;
    }

    public NodoCuadricula getAlmacenBase() {
        return almacenBase;
    }

    public String getId() {
        return id;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public NodoCuadricula getUbicacionActual() {
        return ubicacionActual;
    }

    public void setUbicacionActual(NodoCuadricula ubicacionActual) {
        this.ubicacionActual = ubicacionActual;
    }

    public double getMarcaTiempoActual() {
        return marcaTiempoActual;
    }

    public void setMarcaTiempoActual(double marcaTiempoActual) {
        this.marcaTiempoActual = marcaTiempoActual;
    }

    public int getCapacidadDisponible() {
        return capacidadDisponible;
    }

    public void setCapacidadDisponible(int capacidadDisponible) {
        this.capacidadDisponible = capacidadDisponible;
    }

    public double getTiempoInicioTurno() {
        return tiempoInicioTurno;
    }

    public boolean isRefrigerioTomado() {
        return refrigerioTomado;
    }

    public void setRefrigerioTomado(boolean refrigerioTomado) {
        this.refrigerioTomado = refrigerioTomado;
    }

    @Override
    public String toString() {
        return String.format("%s[%s, Cap:%d/%d, Pos:%s]", id, tipo.getNombre(), capacidadDisponible, tipo.getCapacidad(), ubicacionActual.getId());
    }
}
