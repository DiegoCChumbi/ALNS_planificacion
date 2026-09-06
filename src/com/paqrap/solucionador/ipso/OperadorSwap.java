package com.paqrap.solucionador.ipso;

import com.paqrap.modelo.Pedido;
import java.util.Collections;
import java.util.List;

/**
 * Representa una operación elemental de intercambio (Swap Operator) para el IPSO discreto.
 * Modela la velocidad en espacios de permutación para problemas de enrutamiento (VRP/TSP).
 */
public class OperadorSwap {
    private final int i;
    private final int j;

    public OperadorSwap(int i, int j) {
        this.i = i;
        this.j = j;
    }

    public int getI() {
        return i;
    }

    public int getJ() {
        return j;
    }

    public void aplicar(List<Pedido> permutacion) {
        if (i >= 0 && i < permutacion.size() && j >= 0 && j < permutacion.size() && i != j) {
            Collections.swap(permutacion, i, j);
        }
    }

    @Override
    public String toString() {
        return String.format("Swap(%d, %d)", i, j);
    }
}
