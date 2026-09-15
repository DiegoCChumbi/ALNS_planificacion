# PaqRap - Sistema de Planificación y Enrutamiento Logístico con Algoritmo ALNS

Sistema integral de optimización de rutas de última milla basado en el algoritmo **ALNS (Adaptive Large Neighborhood Search)** con flota heterogénea, ventanas de tiempo estrictas (SLA), pausas obligatorias de almuerzo, límites de jornada laboral y replanificación dinámica ante contingencias viales en tiempo real.

Implementado en **Java 21 puro** utilizando exclusivamente la biblioteca estándar del lenguaje (sin dependencias externas ni gestores pesados).

---

## 📋 Características del Algoritmo ALNS

- **Metaheurística Principal ALNS**:
  - **5 Operadores de Destrucción (Ruina)**: Aleatoria, Peor Costo, Shaw (afinidad espacio-temporal), Ruta Completa (*Route Removal*) y Clúster (*Radial Removal*).
  - **3 Operadores de Reparación (Creación)**: Inserción Voraz (*Greedy*), Regret-$k$ determinista y Regret-$k$ con perturbación estocástica de ruido para máxima diversificación.
  - **Búsqueda Local RVND (*Random Variable Neighborhood Descent*)**: 5 estructuras de vecindario exploradas en orden aleatorio (*Relocate*, *Swap 1-1*, *2-Opt intra*, *Swap 2-1 inter*, *2-Opt\* inter*).
  - **Recombinador SPP (*Set Partitioning Problem*)**: Extracción y ensamblado óptimo de rutas élite desde el pool histórico de rutas factibles ($R_{\text{pool}}$).
  - **Criterio de Aceptación Metropolis**: Simulación de Recocido (*Simulated Annealing*) con enfriamiento geométrico ($T_{k+1} = \alpha \cdot T_k$).
- **Reglas de Negocio Estrictas**:
  - Pausa obligatoria de **refrigerio de 1 hora** programada dentro del intervalo legal $[t_{\text{inicio}} + 1.0\text{h}, t_{\text{fin}} - 1.0\text{h}]$.
  - **Retorno obligatorio a almacén base** antes de cumplir las 8 horas de turno laboral ($t \le 8.0\text{h}$).
  - **Almacén Central** con capacidad infinita y **Almacenes Intermedios** con capacidad física de 1,000 paquetes y recarga instantánea diaria a las `23:59:59`.
- **Motor de Replanificación Dinámica ante Disrupciones**:
  - Detección y desvío automático ante calles bloqueadas mediante búsqueda en anchura (**BFS**).
  - Reasignación de pedidos ante averías mecánicas imprevistas de vehículos en ruta.
  - Inserción en tiempo real de nuevos pedidos urgentes (exprés).
- **Simulación Multidiaria y Estrés Logístico**:
  - Simulación continua de **5 días operativos** (15 turnos continuos de 8 horas).
  - Análisis de curva de **colapso logístico** que determina el límite de saturación de la flota.
- **Configuración Parametrizada por JSON**:
  - Parser propio en Java puro (`LectorJson`) sin dependencias de librerías de terceros.
  - Todos los parámetros de red, almacenes, flota, ALNS y disrupciones configurables en `config/configuracion.json`.

---

## 🚀 Compilación y Ejecución

### 1. Compilación
El código compila directamente con el compilador estándar de OpenJDK:
```bash
mkdir -p bin
javac --release 21 -encoding UTF-8 -d bin $(find src -type f -name '*.java' | sort)
```

### 2. Ejecución de la Simulación Operativa Principal (1 Día con Disrupciones y Replanificación)
Ejecuta el ciclo de despacho inicial, simulación cinemática tramo a tramo, inyección de disrupciones en $t = 2.5\text{h}$ (bloqueos, avería de Moto-1 y 5 pedidos exprés) y replanificación dinámica con ALNS:
```bash
java -cp bin com.paqrap.Main
```

Para ejecutar con un archivo JSON de configuración alternativo:
```bash
java -cp bin com.paqrap.Main config/mi_configuracion.json
```

### 3. Ejecución con datos externos de ventas y bloqueos

Los lectores del paquete `com.paqrap.entrada` aceptan tanto un archivo individual
como una carpeta. Las ventas tienen el formato
`DDdHHhMMm:x,y,cliente,cantidad,sla`; el tiempo se convierte a horas absolutas
(el día 1 comienza en `t=0`). Los bloqueos usan
`inicio-fin:x1,y1,x2,y2,...`; cada par consecutivo de puntos define una arista
horizontal o vertical y permanece activo en `[inicio, fin)`.

```bash
java -cp bin com.paqrap.Main \
  --inputs Inputs/ventas.v20260909/ventas.202601.txt \
  --bloqueos Inputs/bloqueos.v20260909/bloqueo.2601.txt \
  --config config/configuracion.json
```

También se pueden pasar las carpetas completas. Para una prueba rápida o una
ejecución exploratoria se puede limitar el número de pedidos y el horizonte:

```bash
java -cp bin com.paqrap.Main \
  --inputs Inputs/ventas.v20260909 --bloqueos Inputs/bloqueos.v20260909 \
  --max-pedidos 20 --hasta-horas 48
```

Opciones disponibles en el modo externo:

| Opción | Descripción |
|---|---|
| `--inputs` o `--ventas` | Archivo individual o carpeta de ventas |
| `--bloqueos` | Archivo individual o carpeta de bloqueos |
| `--config` | Configuración JSON opcional para flota, almacenes y ALNS |
| `--max-pedidos` | Límite de pedidos para pruebas rápidas |
| `--max-iteraciones` | Límite de iteraciones del ALNS |
| `--hasta-horas` | Fin del horizonte de simulación en horas |

El modo externo informa los archivos y registros leídos, dimensiona
`MapaCuadricula` para cubrir las coordenadas máximas y activa o desactiva las
aristas temporales durante la simulación. La planificación se ejecuta por
ventanas consecutivas de la duración configurada del turno; no se envía todo
el histórico a una única instancia ALNS. En cada ventana se consideran los
pedidos liberados, pendientes y todavía compatibles con su SLA. El modo JSON
anterior permanece disponible sin cambios.

Durante una ejecución se muestran trazas de progreso en consola con el inicio
de cada turno, la construcción de la solución inicial, las iteraciones ALNS
relevantes, el costo y los pedidos asignados/no asignados. Esto permite
distinguir una ejecución activa de una que se haya detenido.

#### Formato de ventas

Cada línea representa un pedido:

```text
01d01h30m:56,30,c4910,02,36
```

El formato es:

```text
DDdHHhMMm:x,y,cliente,cantidad,sla
```

El tiempo se convierte a horas absolutas desde el inicio del día 1. La cantidad
y el SLA deben ser positivos. Los archivos pueden incluir líneas vacías o
comentarios iniciados con `#`.

#### Formato de bloqueos

Cada línea representa un bloqueo temporal:

```text
01d02h22m-01d04h42m:25,45,45,45,45,40
```

El formato es:

```text
inicio-fin:x1,y1,x2,y2,...
```

Cada par consecutivo de coordenadas define una arista horizontal o vertical.
El bloqueo se aplica en el intervalo `[inicio, fin)`, incluyendo el instante de
inicio y excluyendo el instante final. Las líneas inválidas producen un error
explícito indicando archivo y número de línea.

#### Modos de carga

Los lectores aceptan tres formas de operación:

- **Histórico:** se proporciona una carpeta y se procesan sus archivos ordenados.
- **Archivo individual:** se proporciona un archivo de ventas y otro de bloqueos.
- **Incremental:** la API `RegistroEntradas` permite registrar eventos uno a uno:

```java
RegistroEntradas registro = new RegistroEntradas();
registro.registrarPedido(pedido);
registro.registrarBloqueo(bloqueoTemporal);
```

Los datos externos se mantienen separados de la configuración JSON. El JSON
define la flota, almacenes, reglas operativas y parámetros ALNS; las ventas y
bloqueos se incorporan mediante `--inputs` y `--bloqueos`.

Las coordenadas de los inputs pueden superar el mapa de ejemplo de 30x30. En el
modo externo el mapa se dimensiona automáticamente para cubrir pedidos,
bloqueos y almacenes.

### 4. Ejecución de la Suite de Escenarios ALNS (Simulación 5D y Colapso Logístico)
Ejecuta los tres escenarios de experimentación numérica: caso maestro, simulación multidiaria continua de 5 días (15 turnos de 8h) y prueba de estrés progresivo hasta el colapso:
```bash
java -cp bin com.paqrap.Main --escenarios
```

---

## ⚙️ Estructura del Archivo de Configuración (`config/configuracion.json`)

El archivo JSON centraliza todos los parámetros del sistema:

```json
{
  "entorno": {
    "anchoMapa": 30,
    "altoMapa": 30,
    "kmPorCuadra": 1.0,
    "callesDobleSentido": true,
    "penalizacionInalcanzable": 999999.0
  },
  "operacion": {
    "duracionTurnoHoras": 8.0,
    "horaInicioTurno": 7.0,
    "tiempoServicioClienteHoras": 1.0,
    "duracionRefrigerioHoras": 1.0,
    "margenRefrigerioHoras": 1.0,
    "penalizacionPedidoNoAsignado": 10000.0
  },
  "almacenes": {
    "central": { "id": "AlmacenCentral", "x": 15, "y": 15, "capacidad": -1 },
    "intermedios": [
      { "id": "AlmacenInter-1", "x": 6, "y": 8, "capacidad": 1000, "horaRecarga": "23:59:59" },
      { "id": "AlmacenInter-2", "x": 24, "y": 22, "capacidad": 1000, "horaRecarga": "23:59:59" }
    ]
  },
  "tiposVehiculo": [
    { "id": "AUTO", "nombre": "Auto", "capacidad": 24, "velocidadKmH": 40.0, "costoPorKm": 8.0 },
    { "id": "MOTO", "nombre": "Moto", "capacidad": 8, "velocidadKmH": 25.0, "costoPorKm": 6.0 },
    { "id": "BICI", "nombre": "Bicicleta", "capacidad": 4, "velocidadKmH": 12.0, "costoPorKm": 3.0 }
  ],
  "flota": [ ... ],
  "alns": {
    "maxIteraciones": 350,
    "maxSinMejora": 90,
    "intervaloActualizacion": 15,
    "intervaloSPP": 20,
    "factorReaccion": 0.2,
    "minCantidadDestruccion": 1,
    "maxCantidadDestruccion": 5,
    "puntajeMejorGlobal": 10.0,
    "puntajeMejorActual": 5.0,
    "puntajeAceptado": 2.0,
    "temperaturaAceptacionSA": 100.0,
    "tasaEnfriamientoSA": 0.995
  },
  "pedidosIniciales": [ ... ],
  "disrupciones": [ ... ]
}
```

---

## 📊 Archivos de Registro y Auditoría

Cada ejecución genera automáticamente registros detallados en la carpeta `logs/`:
- **`logs/simulacion_movimientos.log`**: Registro cronológico en texto plano con marcas de tiempo, reloj simulado (`07:00:00`), eventos de despacho, movimientos esquina a esquina, refrigerios y alertas viales.
- **`logs/simulacion_movimientos.json`**: Registro estructurado en formato JSON con la totalidad de los eventos y coordenadas para auditoría o procesamiento posterior.

---

## 🔬 Resumen de Desempeño del Algoritmo ALNS

Resultados representativos obtenidos con el algoritmo ALNS:

| Métrica | Desempeño ALNS |
| :--- | :---: |
| **Tiempo de Cómputo Promedio** | **160 – 240 ms** |
| **Cumplimiento de Pedidos** | **100% (35 de 35 pedidos asignados)** |
| **Costo Total de Red** | **S/ 2,496.00 – S/ 2,534.00** |
| **Distancia Recorrida Total** | **370.0 – 390.0 km** |
| **Respuesta ante Replanificación** | **100% de reasignación factible ante bloqueos y averías** |
| **Límite de Colapso Logístico** | **~80–110 pedidos por turno** (saturación por capacidad física y 1h de servicio) |

Para una explicación teórica y matemática detallada del algoritmo ALNS, sus operadores y su formulación, consultar el archivo [`EXPLICACION_ALGORITMO.md`](EXPLICACION_ALGORITMO.md).
