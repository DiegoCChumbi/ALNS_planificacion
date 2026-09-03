package com.paqrap.modelo;

public enum TipoVehiculo {
    AUTO("Auto", 24, 40.0, 8.0),
    MOTO("Moto", 8, 25.0, 6.0),
    BICI("Bicicleta", 4, 12.0, 3.0);

    private final String nombre;
    private final int capacidad;
    private final double velocidadKmH;
    private final double costoPorKm;

    TipoVehiculo(String nombre, int capacidad, double velocidadKmH, double costoPorKm) {
        this.nombre = nombre;
        this.capacidad = capacidad;
        this.velocidadKmH = velocidadKmH;
        this.costoPorKm = costoPorKm;
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
}
