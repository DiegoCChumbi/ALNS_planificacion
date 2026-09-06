package com.paqrap.visor;

import com.paqrap.configuracion.ConfiguracionSistema;
import com.paqrap.modelo.NodoCuadricula;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.List;
import java.util.Map;

/**
 * Panel Swing de renderizado 2D de alta fidelidad para la cuadrícula vial de 30x30 km,
 * almacenes, pedidos, calles clausuradas y vehículos en movimiento.
 */
public class PanelCuadriculaSwing extends JPanel {

    private final ConfiguracionSistema config;
    private EstadoVisor estadoActual;
    private String vehiculoSeleccionadoId = null;
    private String pedidoSeleccionadoId = null;
    private boolean mostrarRutasCompletas = true;
    private boolean mostrarTrayectoriaHistorica = true;

    // Zoom y Desplazamiento (Pan)
    private double zoomFactor = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private Point ultimoPuntoArrastre = null;

    // Dimensiones lógicas de la cuadrícula
    private final int anchoCuadricula;
    private final int altoCuadricula;

    public PanelCuadriculaSwing(ConfiguracionSistema config) {
        this.config = config;
        this.anchoCuadricula = config.getEntorno().getAnchoMapa();
        this.altoCuadricula = config.getEntorno().getAltoMapa();

        setBackground(new Color(22, 27, 34)); // Dark slate background
        setDoubleBuffered(true);

        // Interacción de mouse: arrastre (Pan), zoom con rueda y selección con clic
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e) || SwingUtilities.isMiddleMouseButton(e)) {
                    ultimoPuntoArrastre = e.getPoint();
                } else if (SwingUtilities.isLeftMouseButton(e)) {
                    detectarSeleccion(e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                ultimoPuntoArrastre = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (ultimoPuntoArrastre != null) {
                    offsetX += (e.getX() - ultimoPuntoArrastre.x);
                    offsetY += (e.getY() - ultimoPuntoArrastre.y);
                    ultimoPuntoArrastre = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                double factor = (e.getWheelRotation() < 0) ? 1.15 : 0.85;
                zoomFactor = Math.max(0.4, Math.min(5.0, zoomFactor * factor));
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                actualizarTooltip(e.getX(), e.getY());
            }
        };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    public void setEstadoActual(EstadoVisor estado) {
        this.estadoActual = estado;
        repaint();
    }

    public void setVehiculoSeleccionadoId(String vid) {
        this.vehiculoSeleccionadoId = vid;
        repaint();
    }

    public void setPedidoSeleccionadoId(String pid) {
        this.pedidoSeleccionadoId = pid;
        repaint();
    }

    public void toggleMostrarRutas() {
        this.mostrarRutasCompletas = !this.mostrarRutasCompletas;
        repaint();
    }

    public void toggleMostrarTrayectoria() {
        this.mostrarTrayectoriaHistorica = !this.mostrarTrayectoriaHistorica;
        repaint();
    }

    public void resetVista() {
        this.zoomFactor = 1.0;
        this.offsetX = 0.0;
        this.offsetY = 0.0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int panelW = getWidth();
        int panelH = getHeight();

        // Márgenes para ejes y leyenda
        int marginX = 50;
        int marginY = 40;
        int drawableW = panelW - 2 * marginX;
        int drawableH = panelH - 2 * marginY;

        double cellSize = Math.min((double) drawableW / anchoCuadricula, (double) drawableH / altoCuadricula) * zoomFactor;
        double gridOriginX = marginX + (drawableW - anchoCuadricula * cellSize) / 2.0 + offsetX;
        double gridOriginY = panelH - marginY - (drawableH - altoCuadricula * cellSize) / 2.0 + offsetY;

        // 1. Dibujar líneas de calles de la cuadrícula
        g2.setColor(new Color(40, 50, 68));
        g2.setStroke(new BasicStroke(1.0f));

        for (int i = 0; i <= anchoCuadricula; i++) {
            double sx = gridOriginX + i * cellSize;
            double syTop = gridOriginY - altoCuadricula * cellSize;
            double syBottom = gridOriginY;

            if (i % 5 == 0) {
                g2.setColor(new Color(60, 75, 100));
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(35, 45, 60));
                g2.setStroke(new BasicStroke(0.8f));
            }
            g2.draw(new Line2D.Double(sx, syTop, sx, syBottom));

            // Números eje X
            if (i % 5 == 0 || i == anchoCuadricula) {
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String label = String.valueOf(i);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, (float) (sx - fm.stringWidth(label) / 2.0), (float) (syBottom + 16));
            }
        }

        for (int j = 0; j <= altoCuadricula; j++) {
            double sy = gridOriginY - j * cellSize;
            double sxLeft = gridOriginX;
            double sxRight = gridOriginX + anchoCuadricula * cellSize;

            if (j % 5 == 0) {
                g2.setColor(new Color(60, 75, 100));
                g2.setStroke(new BasicStroke(1.5f));
            } else {
                g2.setColor(new Color(35, 45, 60));
                g2.setStroke(new BasicStroke(0.8f));
            }
            g2.draw(new Line2D.Double(sxLeft, sy, sxRight, sy));

            // Números eje Y
            if (j % 5 == 0 || j == altoCuadricula) {
                g2.setColor(new Color(148, 163, 184));
                g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                String label = String.valueOf(j);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(label, (float) (sxLeft - fm.stringWidth(label) - 8), (float) (sy + fm.getAscent() / 2.0 - 2));
            }
        }

        // Títulos de ejes
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString("X (km)", (float) (gridOriginX + anchoCuadricula * cellSize / 2.0 - 15), (float) (gridOriginY + 34));
        AffineTransform oldAt = g2.getTransform();
        g2.rotate(-Math.PI / 2);
        g2.drawString("Y (km)", (float) (-(gridOriginY - altoCuadricula * cellSize / 2.0 + 15)), (float) (gridOriginX - 30));
        g2.setTransform(oldAt);

        if (estadoActual == null) return;

        // 2. Dibujar Calles Bloqueadas (Incidencias Viales)
        for (TramoBloqueado tb : estadoActual.getBloqueosActivos()) {
            double x1 = gridOriginX + tb.getX1() * cellSize;
            double y1 = gridOriginY - tb.getY1() * cellSize;
            double x2 = gridOriginX + tb.getX2() * cellSize;
            double y2 = gridOriginY - tb.getY2() * cellSize;

            // Halo rojo advertencia
            g2.setColor(new Color(239, 68, 68, 80));
            g2.setStroke(new BasicStroke(8.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));

            // Línea roja discontinua
            float[] dash = {6.0f, 4.0f};
            g2.setColor(new Color(239, 68, 68));
            g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));

            // Icono de bloqueo central
            double cx = (x1 + x2) / 2.0;
            double cy = (y1 + y2) / 2.0;
            g2.setColor(new Color(220, 38, 38));
            g2.fill(new Ellipse2D.Double(cx - 6, cy - 6, 12, 12));
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new Line2D.Double(cx - 3, cy, cx + 3, cy));
        }

        // 3. Dibujar Trayectorias de Vehículos
        if (mostrarTrayectoriaHistorica) {
            for (InfoVehiculoVisor iv : estadoActual.getVehiculos().values()) {
                List<double[]> pts = iv.getTrayectoriaRecorrida();
                if (pts.size() < 2) continue;

                boolean esSeleccionado = iv.getId().equals(vehiculoSeleccionadoId);
                Color colRuta = obtenerColorVehiculo(iv.getTipo());
                int alpha = esSeleccionado ? 200 : 70;
                float strokeW = esSeleccionado ? 2.5f : 1.2f;

                g2.setColor(new Color(colRuta.getRed(), colRuta.getGreen(), colRuta.getBlue(), alpha));
                g2.setStroke(new BasicStroke(strokeW));

                Path2D path = new Path2D.Double();
                double p0x = gridOriginX + pts.get(0)[0] * cellSize;
                double p0y = gridOriginY - pts.get(0)[1] * cellSize;
                path.moveTo(p0x, p0y);

                for (int p = 1; p < pts.size(); p++) {
                    double px = gridOriginX + pts.get(p)[0] * cellSize;
                    double py = gridOriginY - pts.get(p)[1] * cellSize;
                    path.lineTo(px, py);
                }
                g2.draw(path);
            }
        }

        // 4. Dibujar Almacenes
        // 4.1 Almacén Central
        NodoCuadricula ac = config.getAlmacenCentral();
        double acX = gridOriginX + ac.getX() * cellSize;
        double acY = gridOriginY - ac.getY() * cellSize;
        dibujarAlmacen(g2, acX, acY, "Almacén Central", true);

        // 4.2 Almacenes Intermedios
        for (NodoCuadricula ai : config.getAlmacenesIntermedios()) {
            double aiX = gridOriginX + ai.getX() * cellSize;
            double aiY = gridOriginY - ai.getY() * cellSize;
            dibujarAlmacen(g2, aiX, aiY, ai.getId(), false);
        }

        // 5. Dibujar Pedidos (Clientes)
        for (InfoPedidoVisor p : estadoActual.getPedidos().values()) {
            double px = gridOriginX + p.getX() * cellSize;
            double py = gridOriginY - p.getY() * cellSize;
            boolean seleccionado = p.getId().equals(pedidoSeleccionadoId);
            dibujarPedido(g2, px, py, p, seleccionado);
        }

        // 6. Dibujar Vehículos
        for (InfoVehiculoVisor v : estadoActual.getVehiculos().values()) {
            double vx = gridOriginX + v.getX() * cellSize;
            double vy = gridOriginY - v.getY() * cellSize;
            boolean seleccionado = v.getId().equals(vehiculoSeleccionadoId);
            dibujarVehiculo(g2, vx, vy, v, seleccionado);
        }

        // 7. Mini Brújula y Leyenda Rápida en Esquina Superior Derecha
        dibujarMiniLeyenda(g2, panelW - 190, 15);
    }

    private void dibujarAlmacen(Graphics2D g2, double x, double y, String nombre, boolean central) {
        int r = central ? 14 : 10;
        Shape forma;

        if (central) {
            // Estrella o Rombo dorado
            forma = new Rectangle2D.Double(x - r, y - r, 2 * r, 2 * r);
            g2.setColor(new Color(234, 179, 8, 80)); // Aura dorada
            g2.fill(new Ellipse2D.Double(x - r - 4, y - r - 4, (r + 4) * 2, (r + 4) * 2));
            g2.setColor(new Color(234, 179, 8)); // Amarillo oro
            g2.fill(forma);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(forma);

            // Icono central
            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("AC", (float) (x - fm.stringWidth("AC") / 2.0), (float) (y + 4));
        } else {
            // Hexágono o Cuadrado violeta
            forma = new RoundRectangle2D.Double(x - r, y - r, 2 * r, 2 * r, 6, 6);
            g2.setColor(new Color(168, 85, 247, 80));
            g2.fill(new Ellipse2D.Double(x - r - 3, y - r - 3, (r + 3) * 2, (r + 3) * 2));
            g2.setColor(new Color(168, 85, 247)); // Violeta
            g2.fill(forma);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1.5f));
            g2.draw(forma);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("AI", (float) (x - fm.stringWidth("AI") / 2.0), (float) (y + 3));
        }

        // Etiqueta
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(241, 245, 249));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(nombre, (float) (x - fm.stringWidth(nombre) / 2.0), (float) (y - r - 4));
    }

    private void dibujarPedido(Graphics2D g2, double x, double y, InfoPedidoVisor p, boolean seleccionado) {
        int radio = 6;
        Color colFondo;
        Color colBorde = Color.WHITE;

        String est = p.getEstado();
        if ("ENTREGADO".equals(est)) {
            colFondo = new Color(16, 185, 129); // Verde esmeralda
        } else if ("EN_TRANSITO".equals(est) || "EN_ENTREGA".equals(est)) {
            colFondo = new Color(56, 189, 248); // Celeste cielo
        } else if ("TARDE".equals(est)) {
            colFondo = new Color(239, 68, 68); // Rojo
        } else {
            colFondo = new Color(245, 158, 11); // Ámbar / Pendiente
        }

        if (seleccionado) {
            g2.setColor(new Color(255, 255, 255, 150));
            g2.fill(new Ellipse2D.Double(x - radio - 5, y - radio - 5, (radio + 5) * 2, (radio + 5) * 2));
        }

        g2.setColor(colFondo);
        g2.fill(new Ellipse2D.Double(x - radio, y - radio, radio * 2, radio * 2));
        g2.setColor(colBorde);
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(new Ellipse2D.Double(x - radio, y - radio, radio * 2, radio * 2));

        // Etiqueta del cliente
        g2.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g2.setColor(new Color(203, 213, 225));
        g2.drawString(p.getDestinoId(), (float) (x + radio + 2), (float) (y + 3));
    }

    private void dibujarVehiculo(Graphics2D g2, double x, double y, InfoVehiculoVisor v, boolean seleccionado) {
        Color colVeh = obtenerColorVehiculo(v.getTipo());
        boolean averiado = "AVERIADO".equals(v.getEstado());
        boolean refrigerio = "REFRIGERIO".equals(v.getEstado());

        if (averiado) {
            colVeh = new Color(239, 68, 68); // Rojo brillante avería
        }

        int size = seleccionado ? 16 : 12;

        if (seleccionado) {
            g2.setColor(new Color(250, 204, 21, 120));
            g2.fill(new Ellipse2D.Double(x - size - 4, y - size - 4, (size + 4) * 2, (size + 4) * 2));
        }

        // Forma según tipo
        Shape forma;
        if ("Auto".equalsIgnoreCase(v.getTipo())) {
            forma = new RoundRectangle2D.Double(x - size, y - size, size * 2, size * 2, 6, 6);
        } else if ("Moto".equalsIgnoreCase(v.getTipo())) {
            // Triángulo apuntando hacia arriba
            Polygon tri = new Polygon();
            tri.addPoint((int) x, (int) (y - size));
            tri.addPoint((int) (x - size), (int) (y + size));
            tri.addPoint((int) (x + size), (int) (y + size));
            forma = tri;
        } else {
            // Diamante (Bici)
            Polygon dia = new Polygon();
            dia.addPoint((int) x, (int) (y - size));
            dia.addPoint((int) (x + size), (int) y);
            dia.addPoint((int) x, (int) (y + size));
            dia.addPoint((int) (x - size), (int) y);
            forma = dia;
        }

        g2.setColor(colVeh);
        g2.fill(forma);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(forma);

        // Badge de estado adicional
        if (averiado) {
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            g2.drawString("⚠️", (float) (x - 6), (float) (y - size - 2));
        } else if (refrigerio) {
            g2.setColor(Color.ORANGE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 9));
            g2.drawString("☕", (float) (x - 5), (float) (y - size - 2));
        }

        // Nombre del vehículo
        g2.setFont(new Font("SansSerif", Font.BOLD, 8));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(v.getId(), (float) (x - fm.stringWidth(v.getId()) / 2.0), (float) (y + size + 9));
    }

    private Color obtenerColorVehiculo(String tipo) {
        if (tipo == null) return new Color(59, 130, 246);
        switch (tipo.toUpperCase()) {
            case "AUTO": return new Color(59, 130, 246); // Azul
            case "MOTO": return new Color(249, 115, 22); // Naranja
            case "BICI":
            case "BICICLETA": return new Color(16, 185, 129); // Verde
            default: return new Color(99, 102, 241);
        }
    }

    private void dibujarMiniLeyenda(Graphics2D g2, int x, int y) {
        int w = 175;
        int h = 135;

        g2.setColor(new Color(15, 23, 42, 220));
        g2.fillRoundRect(x, y, w, h, 8, 8);
        g2.setColor(new Color(51, 65, 85));
        g2.setStroke(new BasicStroke(1.0f));
        g2.drawRoundRect(x, y, w, h, 8, 8);

        g2.setColor(new Color(248, 250, 252));
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("LEYENDA DE CUADRÍCULA", x + 10, y + 15);

        int ly = y + 30;
        int step = 15;

        dibujarItemLeyenda(g2, x + 10, ly, new Color(234, 179, 8), "Almacén Central (AC)");
        dibujarItemLeyenda(g2, x + 10, ly + step, new Color(168, 85, 247), "Almacén Intermedio (AI)");
        dibujarItemLeyenda(g2, x + 10, ly + 2 * step, new Color(239, 68, 68), "Calle Bloqueada / Avería");
        dibujarItemLeyenda(g2, x + 10, ly + 3 * step, new Color(16, 185, 129), "Pedido Entregado");
        dibujarItemLeyenda(g2, x + 10, ly + 4 * step, new Color(56, 189, 248), "Pedido en Entrega");
        dibujarItemLeyenda(g2, x + 10, ly + 5 * step, new Color(245, 158, 11), "Pedido Pendiente");
        dibujarItemLeyenda(g2, x + 10, ly + 6 * step, new Color(59, 130, 246), "Auto | Moto | Bici");
    }

    private void dibujarItemLeyenda(Graphics2D g2, int x, int y, Color color, String texto) {
        g2.setColor(color);
        g2.fillOval(x, y - 6, 7, 7);
        g2.setColor(new Color(203, 213, 225));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
        g2.drawString(texto, x + 14, y);
    }

    private void detectarSeleccion(int mx, int my) {
        if (estadoActual == null) return;

        int marginX = 50;
        int marginY = 40;
        int drawableW = getWidth() - 2 * marginX;
        int drawableH = getHeight() - 2 * marginY;
        double cellSize = Math.min((double) drawableW / anchoCuadricula, (double) drawableH / altoCuadricula) * zoomFactor;
        double gridOriginX = marginX + (drawableW - anchoCuadricula * cellSize) / 2.0 + offsetX;
        double gridOriginY = getHeight() - marginY - (drawableH - altoCuadricula * cellSize) / 2.0 + offsetY;

        // Verificar clic sobre vehículo
        for (InfoVehiculoVisor v : estadoActual.getVehiculos().values()) {
            double vx = gridOriginX + v.getX() * cellSize;
            double vy = gridOriginY - v.getY() * cellSize;
            if (Point2D.distance(mx, my, vx, vy) <= 15) {
                this.vehiculoSeleccionadoId = v.getId();
                this.pedidoSeleccionadoId = null;
                repaint();
                return;
            }
        }

        // Verificar clic sobre pedido
        for (InfoPedidoVisor p : estadoActual.getPedidos().values()) {
            double px = gridOriginX + p.getX() * cellSize;
            double py = gridOriginY - p.getY() * cellSize;
            if (Point2D.distance(mx, my, px, py) <= 12) {
                this.pedidoSeleccionadoId = p.getId();
                this.vehiculoSeleccionadoId = null;
                repaint();
                return;
            }
        }

        // Clic en vacío deselecciona
        this.vehiculoSeleccionadoId = null;
        this.pedidoSeleccionadoId = null;
        repaint();
    }

    private void actualizarTooltip(int mx, int my) {
        if (estadoActual == null) {
            setToolTipText(null);
            return;
        }

        int marginX = 50;
        int marginY = 40;
        int drawableW = getWidth() - 2 * marginX;
        int drawableH = getHeight() - 2 * marginY;
        double cellSize = Math.min((double) drawableW / anchoCuadricula, (double) drawableH / altoCuadricula) * zoomFactor;
        double gridOriginX = marginX + (drawableW - anchoCuadricula * cellSize) / 2.0 + offsetX;
        double gridOriginY = getHeight() - marginY - (drawableH - altoCuadricula * cellSize) / 2.0 + offsetY;

        for (InfoVehiculoVisor v : estadoActual.getVehiculos().values()) {
            double vx = gridOriginX + v.getX() * cellSize;
            double vy = gridOriginY - v.getY() * cellSize;
            if (Point2D.distance(mx, my, vx, vy) <= 15) {
                setToolTipText(String.format("<html><b>Vehículo: %s (%s)</b><br>Estado: %s<br>Posición: (%.1f, %.1f)<br>Carga: %d/%d paq.<br>Detalle: %s</html>",
                        v.getId(), v.getTipo(), v.getEstado(), v.getX(), v.getY(), v.getCargaActual(), v.getCapacidadTotal(), v.getDetalleEstado()));
                return;
            }
        }

        for (InfoPedidoVisor p : estadoActual.getPedidos().values()) {
            double px = gridOriginX + p.getX() * cellSize;
            double py = gridOriginY - p.getY() * cellSize;
            if (Point2D.distance(mx, my, px, py) <= 12) {
                setToolTipText(String.format("<html><b>Pedido: %s (Destino %s)</b><br>Ubicación: (%d, %d)<br>Cantidad: %d paq.<br>Estado: %s<br>Plazo Límite: %.1fh</html>",
                        p.getId(), p.getDestinoId(), p.getX(), p.getY(), p.getCantidad(), p.getEstado(), p.getTiempoMaximoEntrega()));
                return;
            }
        }

        setToolTipText(null);
    }
}
