package com.paqrap.solucionador.ipso;

import com.paqrap.modelo.Pedido;
import com.paqrap.modelo.Solucion;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una partícula en el enjambre IPSO discreto.
 * Su posición es una permutación de pedidos, y su velocidad es una secuencia de operadores swap.
 */
public class ParticulaIPSO {
    private List<Pedido> posicion;
    private List<OperadorSwap> velocidad;
    private List<Pedido> mejorPosicion;
    private double mejorFitness;
    private Solucion mejorSolucion;

    public ParticulaIPSO(List<Pedido> posicionInicial) {
        this.posicion = new ArrayList<>(posicionInicial);
        this.velocidad = new ArrayList<>();
        this.mejorPosicion = new ArrayList<>(posicionInicial);
        this.mejorFitness = -Double.MAX_VALUE;
        this.mejorSolucion = null;
    }

    public List<Pedido> getPosicion() {
        return posicion;
    }

    public void setPosicion(List<Pedido> posicion) {
        this.posicion = posicion;
    }

    public List<OperadorSwap> getVelocidad() {
        return velocidad;
    }

    public void setVelocidad(List<OperadorSwap> velocidad) {
        this.velocidad = velocidad;
    }

    public List<Pedido> getMejorPosicion() {
        return mejorPosicion;
    }

    public void setMejorPosicion(List<Pedido> mejorPosicion) {
        this.mejorPosicion = new ArrayList<>(mejorPosicion);
    }

    public double getMejorFitness() {
        return mejorFitness;
    }

    public void setMejorFitness(double mejorFitness) {
        this.mejorFitness = mejorFitness;
    }

    public Solucion getMejorSolucion() {
        return mejorSolucion;
    }

    public void setMejorSolucion(Solucion mejorSolucion) {
        this.mejorSolucion = mejorSolucion.copiar();
    }
}
