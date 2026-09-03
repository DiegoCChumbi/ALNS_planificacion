package com.paqrap.solucionador.operadores;

import com.paqrap.modelo.*;
import java.util.List;

public interface OperadorDestruccion {
    String getNombre();
    List<Pedido> destruir(Solucion solucion, int q, ContextoProblema contexto);
}
