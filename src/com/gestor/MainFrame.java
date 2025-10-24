package com.gestor;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI simple para conectar con las clases del proyecto entregado (sin agregar nuevas clases de dominio).
 *
 * Pestañas:
 *  - Reservas (crear/cancelar/listar y calcular costo)
 *  - Disponibilidad (consultar horas libres por fecha/cancha)
 *  - Canchas (alta en memoria para poblar combos)
 *  - Clientes (alta en memoria para poblar combos)
 */
public class MainFrame extends JFrame {

    // ---- Datos en memoria para la GUI ----
    private final List<Cancha> canchas = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();

    // ---- Contenedor principal ----
    private JTabbedPane tabs;

    // ---- Reservas ----
    private JPanel panelReservas;
    public JTextField txtIdReserva;
    public JComboBox<Cancha> cmbCancha;
    public JComboBox<Cliente> cmbCliente;
    public JFormattedTextField ftfFecha; // dd/MM/yyyy
    public JFormattedTextField ftfHora;  // HH:mm
    public JSpinner spDuracion;          // minutos
    public JRadioButton rbSimple;
    public JRadioButton rbFija;
    public ButtonGroup bgTipo;
    public JComboBox<DayOfWeek> cmbDiaSemana;
    public JFormattedTextField ftfFechaFin; // dd/MM/yyyy
    public JSpinner spDescuento;            // 0..1
    public JButton btnCalcularCosto;
    public JButton btnRegistrarReserva;
    public JButton btnCancelarReserva;
    public JButton btnListarReservasDia;
    public JTable tblReservas;
    public DefaultTableModel modelReservas;

    // ---- Disponibilidad ----
    private JPanel panelDisponibilidad;
    public JComboBox<Cancha> cmbCanchaDisp;
    public JFormattedTextField ftfFechaDisp;
    public JButton btnConsultarDisponibilidad;
    public JList<LocalTime> lstHorasLibres;
    public DefaultListModel<LocalTime> modelHoras;

    // ---- Canchas ----
    private JPanel panelCanchas;
    public JTextField txtIdCancha;
    public JTextField txtNombreCancha;
    public JTextField txtDeporte;
    public JFormattedTextField ftfPrecioHora;
    public JButton btnAgregarCancha;
    public JTable tblCanchas;
    public DefaultTableModel modelCanchas;

    // ---- Clientes ----
    private JPanel panelClientes;
    public JTextField txtIdCliente;
    public JTextField txtNombreCliente;
    public JTextField txtTelefono;
    public JButton btnAgregarCliente;
    public JTable tblClientes;
    public DefaultTableModel modelClientes;

    // ---- Utiles ----
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA  = DateTimeFormatter.ofPattern("HH:mm");

    public MainFrame() {
        setTitle("Gestor Deportivo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        tabs = new JTabbedPane();
        buildPanelReservas();
        buildPanelDisponibilidad();
        buildPanelCanchas();
        buildPanelClientes();

        setContentPane(tabs);
        cargarDatosDeEjemplo();
    }

    // -----------------------------------------------------------
    // Construcción de pestaña: RESERVAS
    // -----------------------------------------------------------
    private void buildPanelReservas() {
        panelReservas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        txtIdReserva = new JTextField(10);
        txtIdReserva.setText( String.valueOf(generarId()) );
        cmbCancha = new JComboBox<>();
        cmbCliente = new JComboBox<>();
        ftfFecha = new JFormattedTextField(F_FECHA.toFormat());
        ftfFecha.setColumns(8);
        ftfFecha.setText(LocalDate.now().format(F_FECHA));
        ftfHora = new JFormattedTextField(F_HORA.toFormat());
        ftfHora.setColumns(5);
        ftfHora.setText("19:00");
        spDuracion = new JSpinner(new SpinnerNumberModel(60, 30, 240, 30));

        rbSimple = new JRadioButton("Simple", true);
        rbFija   = new JRadioButton("Fija");
        bgTipo   = new ButtonGroup();
        bgTipo.add(rbSimple); bgTipo.add(rbFija);

        cmbDiaSemana = new JComboBox<>(DayOfWeek.values());
        ftfFechaFin  = new JFormattedTextField(F_FECHA.toFormat());
        ftfFechaFin.setColumns(8);
        spDescuento  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 0.9, 0.05));

        // Fila 0..N
        addRow(form, gc, 0, new JLabel("ID Reserva:"), txtIdReserva);
        addRow(form, gc, 1, new JLabel("Cancha:"), cmbCancha);
        addRow(form, gc, 2, new JLabel("Cliente:"), cmbCliente);
        addRow(form, gc, 3, new JLabel("Fecha (dd/MM/yyyy):"), ftfFecha);
        addRow(form, gc, 4, new JLabel("Hora (HH:mm):"), ftfHora);
        addRow(form, gc, 5, new JLabel("Duración (min):"), spDuracion);

        JPanel tipoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tipoPanel.add(rbSimple); tipoPanel.add(rbFija);
        addRow(form, gc, 6, new JLabel("Tipo:"), tipoPanel);

        addRow(form, gc, 7, new JLabel("Día semana (fija):"), cmbDiaSemana);
        addRow(form, gc, 8, new JLabel("Fecha fin (fija):"), ftfFechaFin);
        addRow(form, gc, 9, new JLabel("Descuento 0..1 (fija):"), spDescuento);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnCalcularCosto = new JButton("Calcular costo");
        btnRegistrarReserva = new JButton("Registrar");
        btnCancelarReserva = new JButton("Cancelar selección");
        btnListarReservasDia = new JButton("Listar del día");
        acciones.add(btnCalcularCosto);
        acciones.add(btnRegistrarReserva);
        acciones.add(btnCancelarReserva);
        acciones.add(btnListarReservasDia);
        addRow(form, gc, 10, new JLabel("Acciones:"), acciones);

        panelReservas.add(form, BorderLayout.NORTH);

        // Tabla
        String[] cols = {"ID", "Cancha", "Cliente", "Inicio", "Fin", "Tipo", "Costo"};
        modelReservas = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblReservas = new JTable(modelReservas);
        panelReservas.add(new JScrollPane(tblReservas), BorderLayout.CENTER);

        // Listeners
        btnRegistrarReserva.addActionListener(e -> onRegistrarReserva());
        btnCancelarReserva.addActionListener(e -> onCancelarReserva());
        btnListarReservasDia.addActionListener(e -> onListarReservasDia());
        btnCalcularCosto.addActionListener(e -> onCalcularCosto());

        tabs.addTab("Reservas", panelReservas);
    }

    // -----------------------------------------------------------
    // Construcción de pestaña: DISPONIBILIDAD
    // -----------------------------------------------------------
    private void buildPanelDisponibilidad() {
        panelDisponibilidad = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        cmbCanchaDisp = new JComboBox<>();
        ftfFechaDisp  = new JFormattedTextField(F_FECHA.toFormat());
        ftfFechaDisp.setColumns(8);
        ftfFechaDisp.setText(LocalDate.now().format(F_FECHA));
        btnConsultarDisponibilidad = new JButton("Consultar");

        addRow(form, gc, 0, new JLabel("Cancha:"), cmbCanchaDisp);
        addRow(form, gc, 1, new JLabel("Fecha (dd/MM/yyyy):"), ftfFechaDisp);
        addRow(form, gc, 2, new JLabel("Acción:"), btnConsultarDisponibilidad);

        panelDisponibilidad.add(form, BorderLayout.NORTH);

        modelHoras = new DefaultListModel<>();
        lstHorasLibres = new JList<>(modelHoras);
        panelDisponibilidad.add(new JScrollPane(lstHorasLibres), BorderLayout.CENTER);

        btnConsultarDisponibilidad.addActionListener(e -> onConsultarDisponibilidad());

        tabs.addTab("Disponibilidad", panelDisponibilidad);
    }

    // -----------------------------------------------------------
    // Construcción de pestaña: CANCHAS (en memoria)
    // -----------------------------------------------------------
    private void buildPanelCanchas() {
        panelCanchas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        txtIdCancha = new JTextField(8);
        txtNombreCancha = new JTextField(14);
        txtDeporte = new JTextField(10);
        ftfPrecioHora = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        ftfPrecioHora.setColumns(8);
        btnAgregarCancha = new JButton("Agregar cancha");

        addRow(form, gc, 0, new JLabel("ID:"), txtIdCancha);
        addRow(form, gc, 1, new JLabel("Nombre:"), txtNombreCancha);
        addRow(form, gc, 2, new JLabel("Deporte:"), txtDeporte);
        addRow(form, gc, 3, new JLabel("Precio/hora:"), ftfPrecioHora);
        addRow(form, gc, 4, new JLabel("Acción:"), btnAgregarCancha);

        panelCanchas.add(form, BorderLayout.NORTH);

        modelCanchas = new DefaultTableModel(new String[]{"ID","Nombre","Deporte","Precio"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCanchas = new JTable(modelCanchas);
        panelCanchas.add(new JScrollPane(tblCanchas), BorderLayout.CENTER);

        btnAgregarCancha.addActionListener(e -> onAgregarCancha());

        tabs.addTab("Canchas", panelCanchas);
    }

    // -----------------------------------------------------------
    // Construcción de pestaña: CLIENTES (en memoria)
    // -----------------------------------------------------------
    private void buildPanelClientes() {
        panelClientes = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        txtIdCliente = new JTextField(8);
        txtNombreCliente = new JTextField(18);
        txtTelefono = new JTextField(12);
        btnAgregarCliente = new JButton("Agregar cliente");

        addRow(form, gc, 0, new JLabel("ID:"), txtIdCliente);
        addRow(form, gc, 1, new JLabel("Nombre:"), txtNombreCliente);
        addRow(form, gc, 2, new JLabel("Teléfono:"), txtTelefono);
        addRow(form, gc, 3, new JLabel("Acción:"), btnAgregarCliente);

        panelClientes.add(form, BorderLayout.NORTH);

        modelClientes = new DefaultTableModel(new String[]{"ID","Nombre","Teléfono"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblClientes = new JTable(modelClientes);
        panelClientes.add(new JScrollPane(tblClientes), BorderLayout.CENTER);

        btnAgregarCliente.addActionListener(e -> onAgregarCliente());

        tabs.addTab("Clientes", panelClientes);
    }

    // -----------------------------------------------------------
    // Handlers de NEGOCIO (conexión a clases existentes)
    // -----------------------------------------------------------

    private void onRegistrarReserva() {
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        if (cancha == null || cliente == null) return;

        LocalDate fecha = parseFecha(ftfFecha.getText());
        LocalTime hora  = parseHora(ftfHora.getText());
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        int id = parseIntOr(txtIdReserva.getText(), generarId());

        if (rbSimple.isSelected()) {
            int dur = (int) spDuracion.getValue();
            ReservaSimple rs = new ReservaSimple(id, inicio, cancha, cliente, dur);
            Reserva.registrarReserva(rs);
            agregarFila(rs, "Simple");
        } else {
            DayOfWeek dow = (DayOfWeek) cmbDiaSemana.getSelectedItem();
            LocalDate fin = parseFecha(ftfFechaFin.getText());
            double d = ((Number) spDescuento.getValue()).doubleValue();
            ReservaFija rf = new ReservaFija(id, inicio, cancha, cliente, dow, fin, d);
            rf.setDuracionMinutos((int) spDuracion.getValue());
            Reserva.registrarReserva(rf);
            agregarFila(rf, "Fija");
        }
        txtIdReserva.setText(String.valueOf(generarId()));
    }

    private void onCancelarReserva() {
        int row = tblReservas.getSelectedRow();
        if (row < 0) return;
        int id = (int) modelReservas.getValueAt(row, 0);
        Reserva.cancelarReserva(id);
        modelReservas.removeRow(row);
    }

    private void onListarReservasDia() {
        LocalDate fecha = parseFecha(ftfFecha.getText());
        java.util.List<Reserva> lista = Reserva.obtenerReservasPorFecha(fecha);
        modelReservas.setRowCount(0);
        for (Reserva r : lista) {
            String tipo = (r instanceof ReservaFija) ? "Fija" : "Simple";
            agregarFila(r, tipo);
        }
    }

    private void onCalcularCosto() {
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        if (cancha == null || cliente == null) return;

        LocalDate fecha = parseFecha(ftfFecha.getText());
        LocalTime hora  = parseHora(ftfHora.getText());
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);

        double costo;
        if (rbSimple.isSelected()) {
            ReservaSimple tmp = new ReservaSimple(0, inicio, cancha, cliente, (int) spDuracion.getValue());
            costo = tmp.calcularCostoTotal();
        } else {
            ReservaFija tmp = new ReservaFija(0, inicio, cancha, cliente,
                    (DayOfWeek) cmbDiaSemana.getSelectedItem(),
                    parseFecha(ftfFechaFin.getText()),
                    ((Number) spDescuento.getValue()).doubleValue());
            tmp.setDuracionMinutos((int) spDuracion.getValue());
            costo = tmp.calcularCostoTotal();
        }
        JOptionPane.showMessageDialog(this, "Costo estimado: $" + String.format("%.2f", costo));
    }

    private void onConsultarDisponibilidad() {
        Cancha cancha = (Cancha) cmbCanchaDisp.getSelectedItem();
        if (cancha == null) return;
        LocalDate fecha = parseFecha(ftfFechaDisp.getText());
        List<LocalTime> libres = Reserva.consultarDisponibilidad(cancha.getIdCancha(), fecha);
        modelHoras.clear();
        for (LocalTime t : libres) modelHoras.addElement(t);
    }

    private void onAgregarCancha() {
        int id = parseIntOr(txtIdCancha.getText(), generarId());
        String nom = txtNombreCancha.getText();
        String dep = txtDeporte.getText();
        double precio = ((Number) ftfPrecioHora.getValue()).doubleValue();
        Cancha c = new Cancha(id, nom, dep, precio);
        canchas.add(c);
        cmbCancha.addItem(c);
        cmbCanchaDisp.addItem(c);
        modelCanchas.addRow(new Object[]{id, nom, dep, precio});
    }

    private void onAgregarCliente() {
        int id = parseIntOr(txtIdCliente.getText(), generarId());
        String nom = txtNombreCliente.getText();
        String tel = txtTelefono.getText();
        Cliente cli = new Cliente(id, nom, tel);
        clientes.add(cli);
        cmbCliente.addItem(cli);
        modelClientes.addRow(new Object[]{id, nom, tel});
    }

    // -----------------------------------------------------------
    // Utilidades
    // -----------------------------------------------------------
    private GridBagConstraints baseGC() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.insets = new Insets(4, 6, 4, 6);
        gc.anchor = GridBagConstraints.LINE_START;
        gc.fill = GridBagConstraints.NONE;
        return gc;
    }

    private void addRow(JPanel panel, GridBagConstraints gc, int fila, JComponent label, JComponent field) {
        GridBagConstraints l = (GridBagConstraints) gc.clone();
        l.gridx = 0; l.gridy = fila; l.weightx = 0; l.fill = GridBagConstraints.NONE;
        panel.add(label, l);
        GridBagConstraints f = (GridBagConstraints) gc.clone();
        f.gridx = 1; f.gridy = fila; f.weightx = 1; f.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, f);
    }

    private LocalDate parseFecha(String s) { return LocalDate.parse(s.trim(), F_FECHA); }
    private LocalTime parseHora(String s)  { return LocalTime.parse(s.trim(), F_HORA); }
    private int parseIntOr(String s, int def) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; } }
    private double parseDoubleOr(String s, double def) { try { return Double.parseDouble(s.trim().replace(',', '.')); } catch (Exception e) { return def; } }
    private int generarId() { return (int) (System.currentTimeMillis() & 0x7fffffff); }

    private void agregarFila(Reserva r, String tipo) {
        Object[] row = {
                r.getIdReserva(),
                r.getCancha()!=null? r.getCancha().getNombre(): "-",
                r.getCliente()!=null? r.getCliente().getNombreCliente(): "-",
                r.getFechaHoraInicio(),
                r.getFechaHoraFin(),
                tipo,
                String.format("%.2f", r.calcularCostoTotal())
        };
        modelReservas.addRow(row);
    }

    private void cargarDatosDeEjemplo() {
        // Canchas
        Cancha c1 = new Cancha(1, "Cancha 1", "Fútbol 5", 12000.0);
        Cancha c2 = new Cancha(2, "Cancha 2", "Pádel", 10000.0);
        canchas.add(c1); canchas.add(c2);
        cmbCancha.addItem(c1); cmbCancha.addItem(c2);
        cmbCanchaDisp.addItem(c1); cmbCanchaDisp.addItem(c2);

        // Clientes
        Cliente a = new Cliente(1, "Juan Pérez", "381-555-000");
        Cliente b = new Cliente(2, "Ana Díaz", "381-555-111");
        clientes.add(a); clientes.add(b);
        cmbCliente.addItem(a); cmbCliente.addItem(b);
    }

    // Punto de entrada opcional
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
