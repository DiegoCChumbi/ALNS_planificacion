# PaqRap - Sistema de Planificación y Monitoreo Logístico (ALNS & IPSO)

Sistema integral de optimización de rutas dinámicas de última milla con metaheurísticas ALNS (*Adaptive Large Neighborhood Search*) e IPSO (*Discrete Particle Swarm Optimization*), simulación de contingencias en tiempo real y visualizador gráfico interactivo (Swing Desktop y Web HTML5).

---

## 🚀 Compilación y Ejecución

El proyecto está implementado en **Java 27 puro** utilizando únicamente la biblioteca estándar (sin dependencias externas ni gestores pesados).

### 1. Compilar el proyecto
```bash
mkdir -p bin
find src -name "*.java" | xargs javac -d bin
```

### 2. Ejecutar Simulación Dinámica con Replanificación
Ejecuta el ciclo de 1 día operativo con disrupciones a las $t = 2.5\text{h}$ (bloqueos de calles, avería vehicular y pedidos exprés):
```bash
java -cp bin com.paqrap.Main
```
O especificando un archivo de configuración JSON personalizado:
```bash
java -cp bin com.paqrap.Main ruta/al/archivo_config.json
```

---

## 🖥️ Visor Gráfico Interactivo de Cuadrilla y Rutas

El sistema cuenta con un visor simple y completo para observar la cuadrícula de $30 \times 30\text{ km}$, los almacenes, pedidos, calles clausuradas y el movimiento en tiempo real de la flota.

### Modos de Visualización:

#### 1. Visor Integrado (Ventana Desktop Swing + Servidor Web en paralelo):
```bash
java -cp bin com.paqrap.Main --visor
```
- Abre una ventana gráfica nativa con renderizado 2D, controles de reproducción (Play/Pausa, pasos de $\pm 3$ min, saltos a hitos, slider de tiempo), tablas de inspección de flota y pedidos, y consola de eventos.
- Si se detecta un entorno sin pantalla (*headless*), levanta automáticamente el visor web.

#### 2. Visor Web (HTML5 Canvas / Acceso desde navegador local o remoto):
```bash
java -cp bin com.paqrap.Main --visor-web
```
- Inicia el servidor HTTP embebido en el puerto `8080` (`http://localhost:8080`).
- Compatible con cualquier navegador en PC, laptops, tablets o smartphones conectados a la red.
- Renderizado interactivo sobre Canvas con Zoom & Pan, selector de velocidad ($1\times, 5\times, 10\times, 25\times$), tarjetas de KPIs, inspección de cuadrilla y log en vivo.

#### 3. Visor Exclusivo Swing:
```bash
java -cp bin com.paqrap.Main --visor-swing
```

---

## 🗺️ Elementos Representados en la Cuadrícula

| Elemento | Representación Visual | Descripción |
| :--- | :--- | :--- |
| **Cuadrícula** | Malla de calles numeradas $0 \dots 30$ | Red vial ortogonal de $30 \times 30\text{ km}$ ($1\text{ km}$ por cuadra). |
| **Almacén Central** | Cuadrado / Rombo Oro `AC (15,15)` | Centro logístico principal con capacidad ilimitada. |
| **Almacenes Intermedios** | Cuadrados Violeta `AI-1 (6,8)`, `AI-2 (24,22)` | Hubs intermedios de recarga diaria (capacidad 1,000 paq.). |
| **Calles Bloqueadas** | Líneas rojas punteadas con halo y ⛔ | Tramos viales inhabilitados que obligan a desvíos automáticos. |
| **Vehículos (Cuadrilla)** | Marcadores con ID y badge de carga | **Auto** (azul), **Moto** (naranja), **Bicicleta** (verde). Muestran alertas de **Avería** (⚠️ rojo) y **Refrigerio** (☕ naranja). |
| **Pedidos (Clientes)** | Círculos con código de cliente | **Ámbar**: Pendiente \| **Celeste**: En entrega \| **Verde**: Entregado a tiempo \| **Rojo**: Fuera de plazo. |

---

## 📊 Suite de Pruebas y Comparativa (RNF-a y RNF-b)

Para evaluar el desempeño comparativo de las metaheurísticas (**ALNS** vs **IPSO**), simular el horizonte de 5 días continuos y analizar las curvas de colapso logístico:

```bash
java -cp bin com.paqrap.Main --comparativa
```
