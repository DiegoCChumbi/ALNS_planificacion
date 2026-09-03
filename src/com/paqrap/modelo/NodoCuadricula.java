package com.paqrap.modelo;

import java.util.Objects;

public class NodoCuadricula {
    private final String id;
    private final int x;
    private final int y;
    private final TipoNodo tipo;

    public NodoCuadricula(String id, int x, int y, TipoNodo tipo) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.tipo = tipo;
    }

    public String getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public TipoNodo getTipo() {
        return tipo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NodoCuadricula que = (NodoCuadricula) o;
        return x == que.x && y == que.y && Objects.equals(id, que.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, x, y);
    }

    @Override
    public String toString() {
        return String.format("%s(%d,%d)[%s]", id, x, y, tipo);
    }
}
