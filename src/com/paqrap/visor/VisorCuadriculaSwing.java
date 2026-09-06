package com.paqrap.visor;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.simulador.EventoSimulacion;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.Locale;

/**
 * Ventana principal del Visor de Simulación y Monitoreo de Cuadrilla y Rutas.
 * Desarrollada en Swing puro sobre el estándar de Java.
 */
public class VisorCuadriculaSwing extends JFrame {

    private final MotorEstadoVisor motorEstado;
    private final PanelCuadriculaSwing panelCuadricula;

    // Controles de Reproducción
    private JSlider sliderTiempo;
    private JButton btnPlayPause;
    private JLabel lblReloj;
    private JLabel lblKpis;
    private JComboBox<String> comboVelocidad;

    // Tablas de Inspección
    private JTable tablaFlota;
    private DefaultTableModel modeloFlota;
    private JTable tablaPedidos;
    private DefaultTableModel modeloPedidos;
    private DefaultListModel<String> modeloEventos;
    private JList<String> listaEventos;

    // Estado del reproductor
    private Timer animadorTimer;
    private boolean reproduciendo = false;
    private double tiempoActual = 0.0;
    private double multiplicadorVelocidad = 5.0; // 5x por defecto

    public VisorCuadriculaSwing(ConfiguracionSistema config, java.util.List<EventoSimulacion> eventos) {
        super("PaqRap - Visor de Monitoreo de Cuadrilla y Rutas en Tiempo Real");
        this.motorEstado = new MotorEstadoVisor(config, eventos);
        this.panelCuadricula = new PanelCuadriculaSwing(config);

        inicializarInterfaz();
        configurarTemporizador();
        actualizarAInstante(0.0);
    }

    private void inicializarInterfaz() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1350, 850);
        setMinimumSize(new Dimension(1000, 700));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Barra Superior (Header)
        add(crearBarraSuperior(), BorderLayout.NORTH);

        // 2. Centro: Split Pane con Mapa y Panel Lateral de Datos
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(panelCuadricula);
        splitPane.setRightComponent(crearPanelLateral());
        splitPane.setResizeWeight(0.72);
        splitPane.setDividerSize(6);
        add(splitPane, BorderLayout.CENTER);

        // 3. Barra Inferior (Controles de Reproducción)
        add(crearBarraInferior(), BorderLayout.SOUTH);
    }

    private JPanel crearBarraSuperior() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(15, 23, 42));
        header.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        // Título e indicador
        JPanel pnlTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlTitulo.setOpaque(false);

        JLabel lblLogo = new JLabel("🚚 PAQRAP");
        lblLogo.setFont(new Font("SansSerif", Font.BOLD, 18));
        lblLogo.setForeground(new Color(56, 189, 248)); // Cyan

        JLabel lblSub = new JLabel("Monitoreo Dinámico de Cuadrilla (Grid 30x30 km)");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(new Color(148, 163, 184));

        pnlTitulo.add(lblLogo);
        pnlTitulo.add(lblSub);
        header.add(pnlTitulo, BorderLayout.WEST);

        // Reloj central y botones derecha
        JPanel pnlDerecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlDerecha.setOpaque(false);

        lblReloj = new JLabel("🕒 07:00:00  (t = 0.00h)");
        lblReloj.setFont(new Font("Monospaced", Font.BOLD, 14));
        lblReloj.setForeground(new Color(250, 204, 21)); // Gold
        pnlDerecha.add(lblReloj);

        JButton btnResetVista = new JButton("🔍 Ajustar Vista");
        estilizarBoton(btnResetVista, new Color(51, 65, 85));
        btnResetVista.addActionListener(e -> panelCuadricula.resetVista());
        pnlDerecha.add(btnResetVista);

        JButton btnRutas = new JButton("🛤️ Rutas");
        estilizarBoton(btnRutas, new Color(51, 65, 85));
        btnRutas.addActionListener(e -> panelCuadricula.toggleMostrarTrayectoria());
        pnlDerecha.add(btnRutas);

        JButton btnWeb = new JButton("🌐 Visor Web");
        estilizarBoton(btnWeb, new Color(16, 185, 129));
        btnWeb.addActionListener(e -> abrirEnNavegador());
        pnlDerecha.add(btnWeb);

        header.add(pnlDerecha, BorderLayout.EAST);
        return header;
    }

    private JPanel crearPanelLateral() {
        JPanel lateral = new JPanel(new BorderLayout());
        lateral.setBackground(new Color(30, 41, 59));
        lateral.setPreferredSize(new Dimension(380, 600));

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(new Color(30, 41, 59));
        tabs.setForeground(Color.WHITE);

        // Tab 1: Flota de Vehículos
        modeloFlota = new DefaultTableModel(new String[]{"ID", "Tipo", "Pos", "Carga", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaFlota = new JTable(modeloFlota);
        tablaFlota.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaFlota.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablaFlota.getSelectedRow() >= 0) {
                String vid = (String) modeloFlota.getValueAt(tablaFlota.getSelectedRow(), 0);
                panelCuadricula.setVehiculoSeleccionadoId(vid);
            }
        });
        estilizarTabla(tablaFlota);
        tabs.addTab("Flota", new JScrollPane(tablaFlota));

        // Tab 2: Pedidos
        modeloPedidos = new DefaultTableModel(new String[]{"ID", "Dest", "Dem", "Plazo", "Estado"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaPedidos = new JTable(modeloPedidos);
        tablaPedidos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tablaPedidos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tablaPedidos.getSelectedRow() >= 0) {
                String pid = (String) modeloPedidos.getValueAt(tablaPedidos.getSelectedRow(), 0);
                panelCuadricula.setPedidoSeleccionadoId(pid);
            }
        });
        estilizarTabla(tablaPedidos);
        tabs.addTab("Pedidos", new JScrollPane(tablaPedidos));

        // Tab 3: Registro de Eventos
        modeloEventos = new DefaultListModel<>();
        listaEventos = new JList<>(modeloEventos);
        listaEventos.setBackground(new Color(15, 23, 42));
        listaEventos.setForeground(new Color(226, 232, 240));
        listaEventos.setFont(new Font("Monospaced", Font.PLAIN, 10));
        tabs.addTab("Eventos", new JScrollPane(listaEventos));

        lateral.add(tabs, BorderLayout.CENTER);

        // Barra inferior de KPIs
        JPanel pnlKpis = new JPanel(new GridLayout(2, 1, 4, 4));
        pnlKpis.setBackground(new Color(15, 23, 42));
        pnlKpis.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        lblKpis = new JLabel("📦 Entregas: 0 | 🚛 Flota: 16 | ⚠️ Bloqueos: 0");
        lblKpis.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblKpis.setForeground(new Color(56, 189, 248));

        JLabel lblAyuda = new JLabel("Arrastre mapa con clic derecho | Rueda: Zoom | Clic: Seleccionar");
        lblAyuda.setFont(new Font("SansSerif", Font.ITALIC, 9));
        lblAyuda.setForeground(new Color(148, 163, 184));

        pnlKpis.add(lblKpis);
        pnlKpis.add(lblAyuda);
        lateral.add(pnlKpis, BorderLayout.SOUTH);

        return lateral;
    }

    private JPanel crearBarraInferior() {
        JPanel footer = new JPanel(new BorderLayout(8, 4));
        footer.setBackground(new Color(15, 23, 42));
        footer.setBorder(BorderFactory.createEmptyBorder(8, 16, 10, 16));

        // Slider de tiempo
        int maxPasos = (int) (motorEstado.getTiempoMaximo() * 100);
        sliderTiempo = new JSlider(0, Math.max(1, maxPasos), 0);
        sliderTiempo.setBackground(new Color(15, 23, 42));
        sliderTiempo.setForeground(new Color(148, 163, 184));
        sliderTiempo.setMajorTickSpacing(100); // Cada 1 hora
        sliderTiempo.setPaintTicks(true);
        sliderTiempo.addChangeListener(e -> {
            if (sliderTiempo.getValueIsAdjusting()) {
                actualizarAInstante(sliderTiempo.getValue() / 100.0);
            }
        });
        footer.add(sliderTiempo, BorderLayout.NORTH);

        // Botonera de control
        JPanel pnlBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlBotones.setOpaque(false);

        JButton btnReset = new JButton("⏮ 0.0h");
        estilizarBoton(btnReset, new Color(51, 65, 85));
        btnReset.addActionListener(e -> actualizarAInstante(0.0));
        pnlBotones.add(btnReset);

        JButton btnStepBack = new JButton("⏪ -3m");
        estilizarBoton(btnStepBack, new Color(51, 65, 85));
        btnStepBack.addActionListener(e -> actualizarAInstante(Math.max(0.0, tiempoActual - 0.05)));
        pnlBotones.add(btnStepBack);

        btnPlayPause = new JButton("▶ Reproducir");
        estilizarBoton(btnPlayPause, new Color(37, 99, 235));
        btnPlayPause.addActionListener(e -> togglePlayPause());
        pnlBotones.add(btnPlayPause);

        JButton btnStepFwd = new JButton("⏩ +3m");
        estilizarBoton(btnStepFwd, new Color(51, 65, 85));
        btnStepFwd.addActionListener(e -> actualizarAInstante(Math.min(motorEstado.getTiempoMaximo(), tiempoActual + 0.05)));
        pnlBotones.add(btnStepFwd);

        // Accesos directos a hitos
        JButton btnHitoDisrupcion = new JButton("⚡ Disrupciones (2.5h)");
        estilizarBoton(btnHitoDisrupcion, new Color(220, 38, 38));
        btnHitoDisrupcion.addActionListener(e -> actualizarAInstante(2.5));
        pnlBotones.add(btnHitoDisrupcion);

        JButton btnHitoRefrig = new JButton("☕ Refrigerio (4.0h)");
        estilizarBoton(btnHitoRefrig, new Color(217, 119, 6));
        btnHitoRefrig.addActionListener(e -> actualizarAInstante(4.0));
        pnlBotones.add(btnHitoRefrig);

        JButton btnHitoFin = new JButton("🏁 Fin Turno (8.0h)");
        estilizarBoton(btnHitoFin, new Color(13, 148, 136));
        btnHitoFin.addActionListener(e -> actualizarAInstante(8.0));
        pnlBotones.add(btnHitoFin);

        footer.add(pnlBotones, BorderLayout.WEST);

        // Selector de velocidad a la derecha
        JPanel pnlVelocidad = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        pnlVelocidad.setOpaque(false);

        JLabel lblVel = new JLabel("Velocidad:");
        lblVel.setForeground(new Color(203, 213, 225));
        lblVel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        pnlVelocidad.add(lblVel);

        comboVelocidad = new JComboBox<>(new String[]{"1x (Tiempo Real)", "5x (Rápido)", "10x", "25x (Super)", "50x (Ultra)"});
        comboVelocidad.setSelectedIndex(1); // 5x
        comboVelocidad.setBackground(new Color(30, 41, 59));
        comboVelocidad.setForeground(Color.WHITE);
        comboVelocidad.addActionListener(e -> {
            switch (comboVelocidad.getSelectedIndex()) {
                case 0: multiplicadorVelocidad = 1.0; break;
                case 1: multiplicadorVelocidad = 5.0; break;
                case 2: multiplicadorVelocidad = 10.0; break;
                case 3: multiplicadorVelocidad = 25.0; break;
                case 4: multiplicadorVelocidad = 50.0; break;
            }
        });
        pnlVelocidad.add(comboVelocidad);

        footer.add(pnlVelocidad, BorderLayout.EAST);
        return footer;
    }

    private void configurarTemporizador() {
        // Tasa de refresco: 40 ms por frame (25 FPS)
        int intervaloMs = 40;
        animadorTimer = new Timer(intervaloMs, e -> {
            if (!reproduciendo) return;
            // 1 segundo real = (multiplicadorVelocidad) segundos simulados
            // dt en horas = (intervaloMs / 1000.0) * multiplicadorVelocidad / 3600.0
            double dtHoras = (intervaloMs / 1000.0) * multiplicadorVelocidad / 60.0; // En minutos rápidos
            double nuevoT = tiempoActual + dtHoras;
            if (nuevoT >= motorEstado.getTiempoMaximo()) {
                nuevoT = motorEstado.getTiempoMaximo();
                togglePlayPause();
            }
            actualizarAInstante(nuevoT);
        });
    }

    private void togglePlayPause() {
        reproduciendo = !reproduciendo;
        if (reproduciendo) {
            btnPlayPause.setText("⏸ Pausar");
            btnPlayPause.setBackground(new Color(220, 38, 38));
            animadorTimer.start();
        } else {
            btnPlayPause.setText("▶ Reproducir");
            btnPlayPause.setBackground(new Color(37, 99, 235));
            animadorTimer.stop();
        }
    }

    public void actualizarAInstante(double t) {
        this.tiempoActual = t;

        // Actualizar slider sin reentrar listener
        int valSlider = (int) Math.round(t * 100);
        if (sliderTiempo.getValue() != valSlider) {
            sliderTiempo.setValue(valSlider);
        }

        // Calcular estado en t
        EstadoVisor estado = motorEstado.calcularEstado(t);
        panelCuadricula.setEstadoActual(estado);

        // Actualizar reloj
        lblReloj.setText(String.format(Locale.US, "🕒 %s  (t = %5.2fh)", estado.getReloj(), t));

        // Actualizar KPIs
        lblKpis.setText(String.format(Locale.US, "📦 Entregas: %d/%d | 🚛 Activos: %d | ⚠️ Bloqueos: %d",
                estado.getPedidosEntregados(), estado.getPedidos().size(),
                estado.getVehiculos().size() - estado.getVehiculosAveriados().size(),
                estado.getBloqueosActivos().size()));

        // Actualizar tabla de flota
        actualizarTablaFlota(estado);

        // Actualizar tabla de pedidos
        actualizarTablaPedidos(estado);

        // Actualizar lista de eventos
        modeloEventos.clear();
        for (int i = estado.getEventosRecientes().size() - 1; i >= 0; i--) {
            EventoSimulacion ev = estado.getEventosRecientes().get(i);
            modeloEventos.addElement(String.format("[%s] %s %s", ev.getReloj(), ev.getTipo(), ev.getDescripcion()));
        }
    }

    private void actualizarTablaFlota(EstadoVisor estado) {
        int filaSel = tablaFlota.getSelectedRow();
        String selVid = (filaSel >= 0 && filaSel < modeloFlota.getRowCount()) ? (String) modeloFlota.getValueAt(filaSel, 0) : null;

        modeloFlota.setRowCount(0);
        int nuevaFilaSel = -1;
        int idx = 0;
        for (InfoVehiculoVisor v : estado.getVehiculos().values()) {
            String pos = String.format(Locale.US, "(%.1f, %.1f)", v.getX(), v.getY());
            String carga = v.getCargaActual() + "/" + v.getCapacidadTotal();
            modeloFlota.addRow(new Object[]{v.getId(), v.getTipo(), pos, carga, v.getEstado()});
            if (v.getId().equals(selVid)) {
                nuevaFilaSel = idx;
            }
            idx++;
        }
        if (nuevaFilaSel >= 0) {
            tablaFlota.setRowSelectionInterval(nuevaFilaSel, nuevaFilaSel);
        }
    }

    private void actualizarTablaPedidos(EstadoVisor estado) {
        int filaSel = tablaPedidos.getSelectedRow();
        String selPid = (filaSel >= 0 && filaSel < modeloPedidos.getRowCount()) ? (String) modeloPedidos.getValueAt(filaSel, 0) : null;

        modeloPedidos.setRowCount(0);
        int nuevaFilaSel = -1;
        int idx = 0;
        for (InfoPedidoVisor p : estado.getPedidos().values()) {
            String dest = String.format("%s(%d,%d)", p.getDestinoId(), p.getX(), p.getY());
            String plazo = String.format(Locale.US, "%.0fh", p.getPlazoHoras());
            modeloPedidos.addRow(new Object[]{p.getId(), dest, p.getCantidad(), plazo, p.getEstado()});
            if (p.getId().equals(selPid)) {
                nuevaFilaSel = idx;
            }
            idx++;
        }
        if (nuevaFilaSel >= 0) {
            tablaPedidos.setRowSelectionInterval(nuevaFilaSel, nuevaFilaSel);
        }
    }

    private void abrirEnNavegador() {
        try {
            // Iniciar servidor web si no está activo y abrir browser
            ServidorVisorWeb.iniciarSiNoActivo(motorEstado);
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI("http://localhost:8080"));
            } else {
                JOptionPane.showMessageDialog(this,
                        "Servidor Web activo en:\nhttp://localhost:8080\nAbra esta URL en su navegador.",
                        "Visor Web Activo", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Abra su navegador en:\nhttp://localhost:8080\n(Error al abrir automático: " + ex.getMessage() + ")",
                    "Servidor Web Activo", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void estilizarBoton(JButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void estilizarTabla(JTable tabla) {
        tabla.setBackground(new Color(15, 23, 42));
        tabla.setForeground(new Color(241, 245, 249));
        tabla.setGridColor(new Color(51, 65, 85));
        tabla.setRowHeight(22);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 10));
        tabla.getTableHeader().setBackground(new Color(30, 41, 59));
        tabla.getTableHeader().setForeground(new Color(203, 213, 225));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
    }
}
