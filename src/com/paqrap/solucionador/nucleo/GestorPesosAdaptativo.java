package com.paqrap.solucionador.nucleo;

import com.paqrap.solucionador.ConfiguracionALNS;
import java.util.*;

public class GestorPesosAdaptativo<T> {
    private final List<T> operadores;
    private final double[] pesos;
    private final double[] puntajes;
    private final int[] vecesUtilizado;
    private final Random random;

    public GestorPesosAdaptativo(List<T> operadores) {
        this.operadores = operadores;
        int n = operadores.size();
        this.pesos = new double[n];
        this.puntajes = new double[n];
        this.vecesUtilizado = new int[n];
        this.random = new Random();

        for (int i = 0; i < n; i++) {
            pesos[i] = 1.0;
            puntajes[i] = 0.0;
            vecesUtilizado[i] = 0;
        }
    }

    public int seleccionarIndiceOperador() {
        double pesoTotal = 0.0;
        for (double w : pesos) pesoTotal += w;

        double r = random.nextDouble() * pesoTotal;
        double suma = 0.0;

        for (int i = 0; i < pesos.length; i++) {
            suma += pesos[i];
            if (r <= suma) {
                vecesUtilizado[i]++;
                return i;
            }
        }
        vecesUtilizado[pesos.length - 1]++;
        return pesos.length - 1;
    }

    public T obtenerOperador(int indice) {
        return operadores.get(indice);
    }

    public T seleccionarOperador() {
        return operadores.get(seleccionarIndiceOperador());
    }

    public void agregarPuntaje(int indice, double puntaje) {
        puntajes[indice] += puntaje;
    }

    public void actualizarPesos(ConfiguracionALNS configuracion) {
        double r = configuracion.getFactorReaccion();
        for (int i = 0; i < pesos.length; i++) {
            if (vecesUtilizado[i] > 0) {
                double puntajePromedio = puntajes[i] / vecesUtilizado[i];
                pesos[i] = (1 - r) * pesos[i] + r * puntajePromedio;
            }
            puntajes[i] = 0.0;
            vecesUtilizado[i] = 0;
        }
    }
}
