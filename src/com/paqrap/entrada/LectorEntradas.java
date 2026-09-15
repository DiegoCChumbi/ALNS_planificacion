package com.paqrap.entrada;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Fachada para cargar ventas y bloqueos en un único registro incremental. */
public final class LectorEntradas {
    private LectorEntradas() {}

    public static RegistroEntradas cargar(Path ventas, Path bloqueos) throws IOException {
        RegistroEntradas registro = new RegistroEntradas();
        if (ventas != null) LectorVentas.leer(ventas, registro);
        if (bloqueos != null) LectorBloqueos.leer(bloqueos, registro);
        return registro;
    }

    public static void cargarVentas(Path ventas, RegistroEntradas registro) throws IOException {
        Objects.requireNonNull(registro, "registro");
        LectorVentas.leer(ventas, registro);
    }

    public static void cargarBloqueos(Path bloqueos, RegistroEntradas registro) throws IOException {
        Objects.requireNonNull(registro, "registro");
        LectorBloqueos.leer(bloqueos, registro);
    }
}
