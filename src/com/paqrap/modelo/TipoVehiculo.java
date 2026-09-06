package com.paqrap.modelo;

import java.util.*;

public class TipoVehiculo {
    public static final TipoVehiculo AUTO = new TipoVehiculo("AUTO", "Auto", 24, 40.0, 8.0);
    public static final TipoVehiculo MOTO = new TipoVehiculo("MOTO", "Moto", 8, 25.0, 6.0);
    public static final TipoVehiculo BICI = new TipoVehiculo("BICI", "Bicicleta", 4, 12.0, 3.0);

    private static final Map<String, TipoVehiculo> REGISTRO = new HashMap<>();

    static {
        registrar(AUTO);
        registrar(MOTO);
        registrar(BICI);
    }

    private final String id;
    private final String nombre;
    private final int capacidad;
    private final double velocidadKmH;
    private final double costoPorKm;

    public TipoVehiculo(String id, String nombre, int capacidad, double velocidadKmH, double costoPorKm) {
        this.id = id;
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.velocidadKmH = velocidadKmH;
        this.costoPorKm = costoPorKm;
    }

    public static void registrar(TipoVehiculo tv) {
        REGISTRO.put(tv.getId().toUpperCase(), tv);
    }

    public static TipoVehiculo obtener(String id) {
        if (id == null) return AUTO;
        TipoVehiculo tv = REGISTRO.get(id.toUpperCase());
        if (tv != null) return tv;
        return AUTO;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public double getVelocidadKmH() {
        return velocidadKmH;
    }

    public double getCostoPorKm() {
        return costoPorKm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TipoVehiculo that = (TipoVehiculo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return nombre;
    }
}
