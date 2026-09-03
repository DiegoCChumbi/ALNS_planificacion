package com.paqrap.solucionador.operadores;

import com.paqrap.modelo.*;
import java.util.List;

public interface OperadorReparacion {
    String getNombre();
    void reparar(Solucion solucion, List<Pedido> pedidosNoAsignados, ContextoProblema contexto);
}
