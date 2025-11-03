package com.gestor.presentacion; // <-- PAQUETE ACTUALIZADO

// Imports de la GUI (Swing)
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Imports de las OTRAS CAPAS
import com.gestor.datos.CanchaDAO;
import com.gestor.datos.ClienteDAO;
import com.gestor.datos.ReservaDAO;
import com.gestor.negocio.Cancha;
import com.gestor.negocio.Cliente;
import com.gestor.negocio.Reserva;
import com.gestor.negocio.ReservaFija;
import com.gestor.negocio.ReservaSimple;

/**
 * GUI principal (Capa de Presentación).
 * NO contiene lógica de negocio ni SQL.
 * Delega todas las operaciones de datos a las clases DAO (Capa de Datos).
 * Maneja los objetos de la Capa de Negocio (Cliente, Cancha, Reserva).
 */
public class MainFrame extends JFrame {

    // ---- Datos en memoria para la GUI (cacheados de la BD) ----
    private final List<Cancha> canchas = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();

    // ---- Atributos para las capas de datos ----
    private final ClienteDAO clienteDAO;
    private final CanchaDAO canchaDAO;
    private final ReservaDAO reservaDAO;

    // ---- Contenedor principal ----
    private JTabbedPane tabs;

    // ---- Reservas ----
    private JPanel panelReservas;
    //public JTextField txtIdReserva;
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
    //public JTextField txtIdCancha;
    public JTextField txtNombreCancha;
    public JTextField txtDeporte;
    public JFormattedTextField ftfPrecioHora;
    public JButton btnAgregarCancha;
    public JTable tblCanchas;
    public DefaultTableModel modelCanchas;

    // ---- Clientes ----
    private JPanel panelClientes;
    //public JTextField txtIdCliente; // El ID ahora es autogenerado
    public JTextField txtNombreCliente;
    public JTextField txtTelefono;
    public JButton btnAgregarCliente;
    public JTable tblClientes;
    public DefaultTableModel modelClientes;

    // ---- Utiles ----
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter F_FECHA_HORA_MOSTRAR
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final java.text.NumberFormat F_MONEDA_AR
            = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));

    public MainFrame() {
        setTitle("Gestor Deportivo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);
        
        // ---- INICIALIZACIÓN DE CAPAS ----
        // 1. Inicializa los DAO (Capa de Datos)
        this.clienteDAO = new ClienteDAO();
        this.canchaDAO = new CanchaDAO();
        this.reservaDAO = new ReservaDAO();
        // ----------------------------------

        tabs = new JTabbedPane();
        buildPanelReservas();
        buildPanelDisponibilidad();
        buildPanelCanchas();
        buildPanelClientes();

        setContentPane(tabs);
        
        // 2. Carga datos iniciales usando los DAO
        cargarClientesDesdeDB();
        cargarCanchasDesdeDB();
        // Carga las reservas del día actual al iniciar
        cargarReservasDelDia(LocalDate.now());
    }

    /**
     * (REFACTORIZADO)
     * Carga los clientes desde la capa de datos (DAO) y puebla la GUI.
     * Ya no contiene SQL.
     */
    private void cargarClientesDesdeDB() {
        // Limpio primero las estructuras en memoria y la tabla
        clientes.clear();
        cmbCliente.removeAllItems();
        modelClientes.setRowCount(0);

        // 1. Pide los clientes al DAO
        List<Cliente> clientesDesdeDB = clienteDAO.obtenerTodos();

        // 2. La GUI se encarga de mostrar los datos
        for (Cliente cli : clientesDesdeDB) {
            clientes.add(cli);
            cmbCliente.addItem(cli);
            modelClientes.addRow(new Object[]{cli.getIdCliente(), cli.getNombreCliente(), cli.getTelefono()});
        }
    }

    /**
     * (REFACTORIZADO)
     * Carga las canchas desde la capa de datos (DAO) y puebla la GUI.
     * Ya no contiene SQL.
     */
    private void cargarCanchasDesdeDB() {
        // limpiar estructuras y tablas
        canchas.clear();
        cmbCancha.removeAllItems();
        cmbCanchaDisp.removeAllItems();
        modelCanchas.setRowCount(0);

        // 1. Pide las canchas al DAO
        List<Cancha> canchasDesdeDB = canchaDAO.obtenerTodas();

        // 2. La GUI muestra los datos
        for (Cancha c : canchasDesdeDB) {
            canchas.add(c);
            cmbCancha.addItem(c);
            cmbCanchaDisp.addItem(c);
            modelCanchas.addRow(new Object[]{c.getIdCancha(), c.getNombre(), c.getDeporte(), c.getPrecioPorHora()});
        }
    }

    // -----------------------------------------------------------
    // Construcción de pestañas (Sin cambios)
    // -----------------------------------------------------------
    private void buildPanelReservas() {
        panelReservas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        //txtIdReserva = new JTextField(10);
        // El ID de reserva es autogenerado por la BD, lo deshabilitamos
        /*txtIdReserva.setText("(Automático)");
        txtIdReserva.setEnabled(false); */
        
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
        //addRow(form, gc, 0, new JLabel("ID Reserva:"), txtIdReserva);
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

    private void buildPanelCanchas() {
        panelCanchas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        /*txtIdCancha = new JTextField(8);
        txtIdCancha.setText("(Automático)");
        txtIdCancha.setEnabled(false); // ID es autogenerado*/
        
        txtNombreCancha = new JTextField(14);
        txtDeporte = new JTextField(10);
        ftfPrecioHora = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        ftfPrecioHora.setColumns(8);
        btnAgregarCancha = new JButton("Agregar cancha");

        //addRow(form, gc, 0, new JLabel("ID:"), txtIdCancha);
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

    private void buildPanelClientes() {
        panelClientes = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        // El ID es autogenerado, no es necesario un campo
        //txtIdCliente = new JTextField(8);
        txtNombreCliente = new JTextField(18);
        txtTelefono = new JTextField(12);
        btnAgregarCliente = new JButton("Agregar cliente");

        //addRow(form, gc, 0, new JLabel("ID:"), txtIdCliente);
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
    // Handlers de NEGOCIO (REFACTORIZADOS - usan DAO)
    // -----------------------------------------------------------

    /**
     * (REFACTORIZADO)
     * Crea un objeto de negocio (Reserva) y lo pasa al DAO para guardarlo.
     * Ya no contiene SQL.
     */
    private void onRegistrarReserva() {
        // 1. Obtener datos de la GUI
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();

        if (cancha == null || cliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar una cancha y un cliente.");
            return;
        }

        LocalDate fecha = parseFecha(ftfFecha.getText());
        LocalTime hora = parseHora(ftfHora.getText());
        if (fecha == null || hora == null) {
             JOptionPane.showMessageDialog(this, "Fecha u hora inválida.");
             return;
        }
        
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        int duracion = (int) spDuracion.getValue();

        // 2. Crear el objeto de Negocio (aún no tiene ID)
        Reserva nuevaReserva;
        if (rbSimple.isSelected()) {
            nuevaReserva = new ReservaSimple(0, inicio, cancha, cliente, duracion);
        } else {
            LocalDate fechaFin = parseFecha(ftfFechaFin.getText());
            if (fechaFin == null) {
                JOptionPane.showMessageDialog(this, "Debe ingresar una fecha de fin para la reserva fija.");
                return;
            }
            DayOfWeek dia = (DayOfWeek) cmbDiaSemana.getSelectedItem();
            double descuento = ((Number) spDescuento.getValue()).doubleValue();
            
            ReservaFija fija = new ReservaFija(0, inicio, cancha, cliente, dia, fechaFin, descuento);
            fija.setDuracionMinutos(duracion); // Asegurarse que use la duración del spinner
            nuevaReserva = fija;
        }
        
        // TODO: Aquí debería ir la validación de solapamiento (Capa de Negocio)
        // boolean solapada = gestorDeNegocio.verificarSolapamiento(nuevaReserva);
        // if (solapada) { ... }

        // 3. Enviar el objeto a la Capa de Datos (DAO)
        int idGenerado = reservaDAO.registrarReserva(nuevaReserva);

        // 4. Actualizar la GUI si el DAO tuvo éxito
        if (idGenerado != -1) {
            // El DAO actualizó el ID dentro del objeto 'nuevaReserva'
            agregarFila(nuevaReserva, rbSimple.isSelected() ? "Simple" : "Fija");
            JOptionPane.showMessageDialog(this, "Reserva guardada correctamente.");
            // Limpiar campos si se desea
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar la reserva. Verifique la consola.");
        }
    }

    /**
     * (REFACTORIZADO)
     * Carga las reservas del día actual al iniciar la app.
     * Usa el DAO de Reservas.
     */
    private void cargarReservasDelDia(LocalDate fecha) {
        modelReservas.setRowCount(0);
        List<Reserva> lista = reservaDAO.obtenerReservasPorFecha(fecha);
        for (Reserva r : lista) {
            String tipo = (r instanceof ReservaFija) ? "Fija" : "Simple";
            agregarFila(r, tipo);
        }
    }

    /**
     * (REFACTORIZADO)
     * Cancela una reserva usando el DAO.
     */
    private void onCancelarReserva() {
        int row = tblReservas.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una reserva de la tabla para cancelar.");
            return;
        }
        int id = (int) modelReservas.getValueAt(row, 0);
        
        // Llama al DAO
        boolean exito = reservaDAO.cancelarReserva(id);
        
        if (exito) {
            modelReservas.removeRow(row);
            JOptionPane.showMessageDialog(this, "Reserva ID " + id + " cancelada.");
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo cancelar la reserva.");
        }
    }

    /**
     * (REFACTORIZADO)
     * Lista las reservas de un día específico usando el DAO.
     */
    private void onListarReservasDia() {
        LocalDate fecha = parseFecha(ftfFecha.getText());
        if (fecha == null) return;
        
        // Llama al DAO
        List<Reserva> lista = reservaDAO.obtenerReservasPorFecha(fecha);
        
        modelReservas.setRowCount(0);
        for (Reserva r : lista) {
            String tipo = (r instanceof ReservaFija) ? "Fija" : "Simple";
            agregarFila(r, tipo);
        }
    }

    /**
     * (SIN CAMBIOS)
     * Esta es lógica de Presentación/Negocio. No toca la BD,
     * solo usa los objetos del modelo para un cálculo.
     */
    private void onCalcularCosto() {
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        if (cancha == null || cliente == null) return;

        LocalDate fecha = parseFecha(ftfFecha.getText());
        LocalTime hora  = parseHora(ftfHora.getText());
        if (fecha == null || hora == null) return;
        
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);

        double costo;
        if (rbSimple.isSelected()) {
            ReservaSimple tmp = new ReservaSimple(0, inicio, cancha, cliente, (int) spDuracion.getValue());
            costo = tmp.calcularCostoTotal();
        } else {
            LocalDate fechaFin = parseFecha(ftfFechaFin.getText());
            if(fechaFin == null) fechaFin = fecha; // Asumir hoy si está vacío para el cálculo

            ReservaFija tmp = new ReservaFija(0, inicio, cancha, cliente,
                    (DayOfWeek) cmbDiaSemana.getSelectedItem(),
                    fechaFin,
                    ((Number) spDescuento.getValue()).doubleValue());
            tmp.setDuracionMinutos((int) spDuracion.getValue());
            costo = tmp.calcularCostoTotal();
        }
        JOptionPane.showMessageDialog(this, "Costo estimado: " + F_MONEDA_AR.format(costo));
    }

    /**
     * (REFACTORIZADO)
     * Consulta disponibilidad usando el DAO.
     */
    private void onConsultarDisponibilidad() {
        Cancha cancha = (Cancha) cmbCanchaDisp.getSelectedItem();
        if (cancha == null) return;
        
        LocalDate fecha = parseFecha(ftfFechaDisp.getText());
        if (fecha == null) return;
        
        // Llama al DAO
        List<LocalTime> libres = reservaDAO.consultarDisponibilidad(cancha.getIdCancha(), fecha);
        
        modelHoras.clear();
        for (LocalTime t : libres) modelHoras.addElement(t);
    }

    /**
     * (REFACTORIZADO)
     * Agrega una cancha usando el DAO.
     */
    private void onAgregarCancha() {
        String nom = txtNombreCancha.getText();
        String dep = txtDeporte.getText();
        Number numPrecio = (Number) ftfPrecioHora.getValue();
        double precio = (numPrecio != null) ? numPrecio.doubleValue() : 0.0;

        if (nom == null || nom.trim().isEmpty() || dep == null || dep.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nombre y Deporte son obligatorios.");
            return;
        }

        // 1. Crear objeto de negocio
        Cancha c = new Cancha(0, nom, dep, precio); // ID 0, la BD lo asignará

        // 2. Enviar al DAO
        int idGenerado = canchaDAO.agregarCancha(c);

        // 3. Actualizar GUI
        if (idGenerado != -1) {
            // El DAO actualizó el ID en el objeto 'c'
            canchas.add(c);
            cmbCancha.addItem(c);
            cmbCanchaDisp.addItem(c);
            modelCanchas.addRow(new Object[]{c.getIdCancha(), c.getNombre(), c.getDeporte(), c.getPrecioPorHora()});
            
            //txtIdCancha.setText(String.valueOf(idGenerado)); // Mostrar el ID generado
            txtNombreCancha.setText("");
            txtDeporte.setText("");
            ftfPrecioHora.setValue(null);
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar la cancha.");
        }
    }
    
    /**
     * (REFACTORIZADO)
     * Agrega un cliente usando el DAO.
     */
    private void onAgregarCliente() {
        String nom = txtNombreCliente.getText();
        String tel = txtTelefono.getText();

        if (nom == null || nom.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre válido");
            return;
        }

        // 1. Crear objeto de negocio
        Cliente cli = new Cliente(0, nom, tel); // ID 0, la BD lo asignará

        // 2. Enviar al DAO
        int idGenerado = clienteDAO.agregarCliente(cli);

        // 3. Actualizar GUI
        if (idGenerado != -1) {
            // El DAO actualizó el ID en el objeto 'cli'
            clientes.add(cli);
            cmbCliente.addItem(cli);
            modelClientes.addRow(new Object[]{cli.getIdCliente(), cli.getNombreCliente(), cli.getTelefono()});
            
            txtNombreCliente.setText("");
            txtTelefono.setText("");
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar el cliente.");
        }
    }
       

    // -----------------------------------------------------------
    // Utilidades de la GUI (Sin cambios)
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

    private LocalDate parseFecha(String s) { 
        try {
            return LocalDate.parse(s.trim(), F_FECHA); 
        } catch (Exception e) {
            return null;
        }
    }
    private LocalTime parseHora(String s)  { 
        try {
            return LocalTime.parse(s.trim(), F_HORA); 
        } catch (Exception e) {
            return null;
        }
    }
    
    private int generarId() { 
        // Ya no se usa, la BD genera los IDs
        return (int) (System.currentTimeMillis() & 0x7fffffff); 
    }

    /**
     * Helper para agregar una fila a la tabla de reservas formateando los datos.
     */
    private void agregarFila(Reserva r, String tipo) {
        String inicioStr = r.getFechaHoraInicio().format(F_FECHA_HORA_MOSTRAR);
        String finStr = r.getFechaHoraFin().format(F_FECHA_HORA_MOSTRAR);
        String costoStr = F_MONEDA_AR.format(r.calcularCostoTotal());

        modelReservas.addRow(new Object[]{
                r.getIdReserva(),
                r.getCancha() != null ? r.getCancha().getNombre() : "-",
                r.getCliente() != null ? r.getCliente().getNombreCliente() : "-",
                inicioStr,
                finStr,
                tipo,
                costoStr
        });
    }

    // EL MÉTODO cargarDatosDeEjemplo() FUE ELIMINADO
    // EL MÉTODO main() FUE ELIMINADO (el punto de entrada es GestorDeportivoApp)
}
