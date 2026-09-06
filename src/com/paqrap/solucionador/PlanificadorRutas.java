package com.paqrap.solucionador;

import com.paqrap.modelo.ContextoProblema;
import com.paqrap.modelo.Solucion;

/**
 * Interfaz común para los algoritmos metaheurísticos del componente planificador de PaqRap.
 * Cumple con los requisitos no funcionales RNF-a y RNF-b para experimentación numérica comparativa.
 */
public interface PlanificadorRutas {
    String getNombre();
    Solucion resolver(ContextoProblema contexto);
}
