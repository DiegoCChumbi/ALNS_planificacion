# PaqRap - Sistema de Planificación y Enrutamiento Logístico con Algoritmo ALNS

Sistema integral de optimización de rutas de última milla basado en el algoritmo **ALNS (Adaptive Large Neighborhood Search)** con flota heterogénea, ventanas de tiempo estrictas (SLA), pausas obligatorias de almuerzo, límites de jornada laboral y replanificación dinámica ante contingencias viales en tiempo real.

Implementado en **Java 27 puro** utilizando exclusivamente la biblioteca estándar del lenguaje (sin dependencias externas ni gestores pesados).

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
find src -name "*.java" | xargs javac -d bin
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

### 3. Ejecución de la Suite de Escenarios ALNS (Simulación 5D y Colapso Logístico)
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
