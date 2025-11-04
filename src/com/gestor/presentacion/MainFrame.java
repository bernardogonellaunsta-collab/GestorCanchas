package com.gestor.presentacion;

import com.gestor.datos.CanchaDAO;
import com.gestor.datos.ClienteDAO;
import com.gestor.datos.ReservaDAO;
import com.gestor.negocio.Cancha;
import com.gestor.negocio.Cliente;
import com.gestor.negocio.Reserva;
import com.gestor.negocio.ReservaFija;
import com.gestor.negocio.ReservaSimple;
// Importar JDateChooser (si no está ya)
import com.toedter.calendar.JDateChooser;
import java.util.Date;
import java.time.ZoneId;
import java.util.Date; // Para interactuar con JDateChooser
// --- FIN DE IMPORTACIONES MODIFICADAS ---

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * (ACTUALIZADO CON JDateChooser)
 * GUI principal. Refactorizada para usar la Capa de Datos (DAO).
 * Delega todas las operaciones de datos a las clases DAO (Capa de Datos).
 * Maneja los objetos de la Capa de Negocio (Cliente, Cancha, Reserva).
 * Incluye lógica para ocultar/mostrar campos y filtrar combos.
 * Incluye lógica para cancelación de reservas simples vs. fijas (en grupo).
 * (CORREGIDO) Disponibilidad usa JTable.
 * (CORREGIDO) RegistrarReserva ahora recarga la lista.
 * (NUEVO) Agregada funcionalidad para Modificar y Eliminar Canchas.
 * (NUEVO) Agregada funcionalidad para Modificar y Eliminar Clientes.
 */
public class MainFrame extends JFrame {

    // ---- Datos en memoria para la GUI (cacheados de la BD) ----
    private final List<Cancha> canchas = new ArrayList<>();
    private final List<Cliente> clientes = new ArrayList<>();
    // Lista para las reservas mostradas en la tabla
    private final List<Reserva> reservasMostradasEnTabla = new ArrayList<>();
    
    // ---- Atributos para las capas de datos ----
    private final ClienteDAO clienteDAO;
    private final CanchaDAO canchaDAO;
    private final ReservaDAO reservaDAO;

    // ---- Contenedor principal ----
    private JTabbedPane tabs;

    // ---- Reservas ----
    private JPanel panelReservas;
    public JTextField txtIdReserva;
    public JComboBox<String> cmbDeporteReserva; 
    public JComboBox<Cancha> cmbCancha;
    public JComboBox<Cliente> cmbCliente;
    
    // --- INICIO DE CAMPOS MODIFICADOS ---
    // public JFormattedTextField ftfFecha; // Reemplazado
    public JDateChooser jdcFecha; // Nuevo componente de calendario
    public JFormattedTextField ftfHora;  // HH:mm (se mantiene)
    // --- FIN DE CAMPOS MODIFICADOS ---

    public JSpinner spDuracion;          // minutos
    public JRadioButton rbSimple;
    public JRadioButton rbFija;
    public ButtonGroup bgTipo;
    
    // --- INICIO DE CAMPOS MODIFICADOS ---
    public JComboBox<DayOfWeek> cmbDiaSemana;
    // public JFormattedTextField ftfFechaFin; // Reemplazado
    public JDateChooser jdcFechaFin; // Nuevo componente de calendario
    public JSpinner spDescuento;            // 0..1
    // --- FIN DE CAMPOS MODIFICADOS ---


    // Atributos para ocultar/mostrar labels de reserva fija
    private JLabel lblDiaSemana;
    private JLabel lblFechaFin;
    private JLabel lblDescuento;
    
    public JButton btnCalcularCosto;
    public JButton btnRegistrarReserva;
    public JButton btnCancelarReserva;
    public JButton btnListarReservasDia;
    public JTable tblReservas;
    public DefaultTableModel modelReservas;

    // ---- Disponibilidad ----
    private JPanel panelDisponibilidad;
    public JComboBox<Cancha> cmbCanchaDisp;
    // --- INICIO DE CAMPOS MODIFICADOS ---
    // public JFormattedTextField ftfFechaDisp; // Reemplazado
    public JDateChooser jdcFechaDisp; // Nuevo componente de calendario
    // --- FIN DE CAMPOS MODIFICADOS ---
    public JButton btnConsultarDisponibilidad;
    public JTable tblHorasLibres;
    public DefaultTableModel modelHoras;

    // ---- Canchas ----
    private JPanel panelCanchas;
    public JTextField txtIdCancha;
    public JTextField txtNombreCancha;
    public JComboBox<String> cmbDeporteCancha; 
    public JFormattedTextField ftfPrecioHora;
    public JButton btnAgregarCancha;
    // --- NUEVOS BOTONES ---
    public JButton btnModificarCancha;
    public JButton btnEliminarCancha;
    public JButton btnLimpiarCancha;
    // --- FIN NUEVOS BOTONES ---
    public JTable tblCanchas;
    public DefaultTableModel modelCanchas;

    // ---- Clientes ----
    private JPanel panelClientes;
    // --- NUEVOS CAMPOS ---
    public JTextField txtIdCliente;
    public JTextField txtNombreCliente;
    public JTextField txtTelefono;
    public JButton btnAgregarCliente;
    public JButton btnModificarCliente;
    public JButton btnEliminarCliente;
    public JButton btnLimpiarCliente;
    // --- FIN NUEVOS CAMPOS ---
    public JTable tblClientes;
    public DefaultTableModel modelClientes;

    // ---- Utiles ----
    // F_FECHA ya no es necesario para parsear, pero se mantiene para F_FECHA_HORA_MOSTRAR
    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter F_FECHA_HORA_MOSTRAR
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final java.text.NumberFormat F_MONEDA_AR
            = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("es", "AR"));
            
    // Listas de deportes (centralizadas)
    private static final String[] DEPORTES = {"Fútbol", "Pádel", "Tenis", "Básquet"};
    private static final String[] DEPORTES_PARA_FILTRO = {"Todos", "Fútbol", "Pádel", "Tenis", "Básquet"};

    public MainFrame() {
        setTitle("Gestor Deportivo");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 680);
        setLocationRelativeTo(null);

        // 1. Inicializa las capas de datos
        this.clienteDAO = new ClienteDAO();
        this.canchaDAO = new CanchaDAO();
        this.reservaDAO = new ReservaDAO();
        
        // 2. Construye la GUI
        tabs = new JTabbedPane();
        buildPanelReservas();
        buildPanelDisponibilidad();
        buildPanelCanchas();
        buildPanelClientes();
        setContentPane(tabs);
        
        // 3. Carga datos iniciales usando los DAO
        cargarClientesDesdeDB();
        cargarCanchasDesdeDB();
        // Carga las reservas del día actual al iniciar
        cargarReservasDelDia(LocalDate.now());
    }

    /**
     * (REFACTORIZADO)
     * Carga clientes desde el DAO y los pone en la GUI.
     */
    private void cargarClientesDesdeDB() {
        clientes.clear();
        cmbCliente.removeAllItems();
        modelClientes.setRowCount(0);

        List<Cliente> clientesDesdeDB = clienteDAO.obtenerTodos();

        for (Cliente cli : clientesDesdeDB) {
            clientes.add(cli);
            cmbCliente.addItem(cli);
            modelClientes.addRow(new Object[]{cli.getIdCliente(), cli.getNombreCliente(), cli.getTelefono()});
        }
    }

    /**
     * (REFACTORIZADO)
     * Carga canchas desde el DAO y las pone en la GUI.
     */
    private void cargarCanchasDesdeDB() {
        canchas.clear();
        cmbCancha.removeAllItems();
        cmbCanchaDisp.removeAllItems();
        modelCanchas.setRowCount(0);

        List<Cancha> canchasDesdeDB = canchaDAO.obtenerTodas();

        for (Cancha c : canchasDesdeDB) {
            canchas.add(c);
            cmbCancha.addItem(c);
            cmbCanchaDisp.addItem(c);
            modelCanchas.addRow(new Object[]{c.getIdCancha(), c.getNombre(), c.getDeporte(), c.getPrecioPorHora()});
        }
        
        // Aplica el filtro inicial
        if (cmbDeporteReserva != null) {
            filtrarCanchasPorDeporte();
        }
    }

    // -----------------------------------------------------------
    // Construcción de pestaña: RESERVAS
    // -----------------------------------------------------------
    
    /**
     * (MODIFICADO)
     * Construye el panel de Reservas, añadiendo lógica para
     * mostrar/ocultar campos y filtrar canchas.
     * Reemplaza JFormattedTextField por JDateChooser.
     */
    private void buildPanelReservas() {
        panelReservas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        txtIdReserva = new JTextField(10);
        txtIdReserva.setText("(autogenerado)");
        txtIdReserva.setEnabled(false); 
        
        // Combo para filtrar deporte en Reservas
        cmbDeporteReserva = new JComboBox<>(DEPORTES_PARA_FILTRO);
        cmbCancha = new JComboBox<>();
        
        // Agregar el listener para que el combo de deporte filtre el de cancha
        cmbDeporteReserva.addActionListener(e -> filtrarCanchasPorDeporte());
        
        cmbCliente = new JComboBox<>();
        
        // --- INICIO DE MODIFICACIÓN ---
        // ftfFecha = new JFormattedTextField(F_FECHA.toFormat()); // Reemplazado
        // ftfFecha.setColumns(8);
        // ftfFecha.setText(LocalDate.now().format(F_FECHA));
        jdcFecha = new JDateChooser();
        jdcFecha.setDate(new Date()); // Valor por defecto: hoy
        jdcFecha.setPreferredSize(new Dimension(120, jdcFecha.getPreferredSize().height));
        // --- FIN DE MODIFICACIÓN ---

        ftfHora = new JFormattedTextField(F_HORA.toFormat());
        ftfHora.setColumns(5);
        ftfHora.setText("19:00");
        spDuracion = new JSpinner(new SpinnerNumberModel(60, 30, 240, 30));

        rbSimple = new JRadioButton("Simple", true);
        rbFija   = new JRadioButton("Fija");
        bgTipo   = new ButtonGroup();
        bgTipo.add(rbSimple); bgTipo.add(rbFija);

        // Crear el listener para los radio buttons
        ActionListener radioListener = e -> actualizarVisibilidadCamposFijos();
        rbSimple.addActionListener(radioListener);
        rbFija.addActionListener(radioListener);

        // Instanciar los componentes (inputs y labels)
        cmbDiaSemana = new JComboBox<>(DayOfWeek.values());
        
        // --- INICIO DE CÓDIGO NUEVO (REVERTIDO) ---
        // Se eliminó el "Renderer" para mostrar días en español
        // --- FIN DE CÓDIGO NUEVO (REVERTIDO) ---
        
        // --- INICIO DE MODIFICACIÓN ---
        // ftfFechaFin  = new JFormattedTextField(F_FECHA.toFormat()); // Reemplazado
        // ftfFechaFin.setColumns(8);
        jdcFechaFin = new JDateChooser();
        jdcFechaFin.setPreferredSize(new Dimension(120, jdcFechaFin.getPreferredSize().height));
        // --- FIN DE MODIFICACIÓN ---

        spDescuento  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 0.9, 0.05));
        
        // Instanciar los JLabels (usando los atributos de clase)
        lblDiaSemana = new JLabel("Día semana (fija):");
        lblFechaFin = new JLabel("Fecha fin (fija):");
        lblDescuento = new JLabel("Descuento 0..1 (fija):");
        
        // Fila 0..N
        addRow(form, gc, 0, new JLabel("ID Reserva:"), txtIdReserva);
        addRow(form, gc, 1, new JLabel("Deporte:"), cmbDeporteReserva);
        addRow(form, gc, 2, new JLabel("Cancha:"), cmbCancha);
        addRow(form, gc, 3, new JLabel("Cliente:"), cmbCliente);
        
        // --- INICIO DE MODIFICACIÓN ---
        // addRow(form, gc, 4, new JLabel("Fecha (dd/MM/yyyy):"), ftfFecha); // Reemplazado
        addRow(form, gc, 4, new JLabel("Fecha:"), jdcFecha); // Etiqueta simplificada
        // --- FIN DE MODIFICACIÓN ---
        
        addRow(form, gc, 5, new JLabel("Hora (HH:mm):"), ftfHora);
        addRow(form, gc, 6, new JLabel("Duración (min):"), spDuracion);

        JPanel tipoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tipoPanel.add(rbSimple); tipoPanel.add(rbFija);
        addRow(form, gc, 7, new JLabel("Tipo:"), tipoPanel);

        // Usar los atributos de JLabel en addRow
        addRow(form, gc, 8, lblDiaSemana, cmbDiaSemana);
        
        // --- INICIO DE MODIFICACIÓN ---
        // addRow(form, gc, 9, lblFechaFin, ftfFechaFin); // Reemplazado
        addRow(form, gc, 9, lblFechaFin, jdcFechaFin);
        // --- FIN DE MODIFICACIÓN ---
        
        addRow(form, gc, 10, lblDescuento, spDescuento);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnCalcularCosto = new JButton("Calcular costo");
        btnRegistrarReserva = new JButton("Registrar");
        btnCancelarReserva = new JButton("Cancelar selección");
        btnListarReservasDia = new JButton("Listar del día");
        acciones.add(btnCalcularCosto);
        acciones.add(btnRegistrarReserva);
        acciones.add(btnCancelarReserva);
        acciones.add(btnListarReservasDia);
        addRow(form, gc, 11, new JLabel("Acciones:"), acciones);

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

        // Llamada inicial para ocultar los campos al arrancar
        actualizarVisibilidadCamposFijos();
        
        tabs.addTab("Reservas", panelReservas);
    }

    /**
     * (MODIFICADO)
     * Reemplaza la JList por una JTable para mostrar la disponibilidad.
     * Reemplaza JFormattedTextField por JDateChooser.
     */
    private void buildPanelDisponibilidad() {
        panelDisponibilidad = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        cmbCanchaDisp = new JComboBox<>();
        
        // --- INICIO DE MODIFICACIÓN ---
        // ftfFechaDisp  = new JFormattedTextField(F_FECHA.toFormat()); // Reemplazado
        // ftfFechaDisp.setColumns(8);
        // ftfFechaDisp.setText(LocalDate.now().format(F_FECHA));
        jdcFechaDisp = new JDateChooser();
        jdcFechaDisp.setDate(new Date()); // Valor por defecto: hoy
        jdcFechaDisp.setPreferredSize(new Dimension(120, jdcFechaDisp.getPreferredSize().height));
        // --- FIN DE MODIFICACIÓN ---

        btnConsultarDisponibilidad = new JButton("Consultar");

        addRow(form, gc, 0, new JLabel("Cancha:"), cmbCanchaDisp);
        
        // --- INICIO DE MODIFICACIÓN ---
        // addRow(form, gc, 1, new JLabel("Fecha (dd/MM/yyyy):"), ftfFechaDisp); // Reemplazado
        addRow(form, gc, 1, new JLabel("Fecha:"), jdcFechaDisp); // Etiqueta simplificada
        // --- FIN DE MODIFICACIÓN ---
        
        addRow(form, gc, 2, new JLabel("Acción:"), btnConsultarDisponibilidad);

        panelDisponibilidad.add(form, BorderLayout.NORTH);

        // Crear JTable
        String[] cols = {"Horario de Inicio"};
        modelHoras = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblHorasLibres = new JTable(modelHoras);
        
        panelDisponibilidad.add(new JScrollPane(tblHorasLibres), BorderLayout.CENTER);

        btnConsultarDisponibilidad.addActionListener(e -> onConsultarDisponibilidad());

        tabs.addTab("Disponibilidad", panelDisponibilidad);
    }

    /**
     * (MODIFICADO)
     * Construye el panel de Canchas, usando JComboBox para Deporte.
     */
    private void buildPanelCanchas() {
        panelCanchas = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        txtIdCancha = new JTextField(8);
        txtIdCancha.setText("(autogenerado)");
        txtIdCancha.setEnabled(false); // ID es autogenerado
        
        txtNombreCancha = new JTextField(14);
        
        // Usar JComboBox en lugar de JTextField
        cmbDeporteCancha = new JComboBox<>(DEPORTES);
        
        ftfPrecioHora = new JFormattedTextField(java.text.NumberFormat.getNumberInstance());
        ftfPrecioHora.setColumns(8);
        btnAgregarCancha = new JButton("Agregar cancha");
        
        // --- INICIO DE MODIFICACIÓN: Nuevos botones ---
        btnModificarCancha = new JButton("Modificar cancha");
        btnEliminarCancha = new JButton("Eliminar cancha");
        btnLimpiarCancha = new JButton("Limpiar");
        
        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.add(btnAgregarCancha);
        acciones.add(btnModificarCancha);
        acciones.add(btnEliminarCancha);
        acciones.add(btnLimpiarCancha);
        // --- FIN DE MODIFICACIÓN ---

        addRow(form, gc, 0, new JLabel("ID:"), txtIdCancha);
        addRow(form, gc, 1, new JLabel("Nombre:"), txtNombreCancha);
        addRow(form, gc, 2, new JLabel("Deporte:"), cmbDeporteCancha);
        addRow(form, gc, 3, new JLabel("Precio/hora:"), ftfPrecioHora);
        // addRow(form, gc, 4, new JLabel("Acción:"), btnAgregarCancha); // Reemplazado por el panel de acciones
        addRow(form, gc, 4, new JLabel("Acciones:"), acciones); // Panel con todos los botones

        panelCanchas.add(form, BorderLayout.NORTH);

        modelCanchas = new DefaultTableModel(new String[]{"ID","Nombre","Deporte","Precio"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblCanchas = new JTable(modelCanchas);
        panelCanchas.add(new JScrollPane(tblCanchas), BorderLayout.CENTER);

        // --- INICIO DE MODIFICACIÓN: Listeners ---
        btnAgregarCancha.addActionListener(e -> onAgregarCancha());
        btnModificarCancha.addActionListener(e -> onModificarCancha());
        btnEliminarCancha.addActionListener(e -> onEliminarCancha());
        btnLimpiarCancha.addActionListener(e -> onLimpiarCancha());

        // Listener para la tabla (cargar datos al formulario)
        tblCanchas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onTablaCanchaSeleccionada();
            }
        });
        
        // Estado inicial de los botones
        onLimpiarCancha();
        // --- FIN DE MODIFICACIÓN ---

        tabs.addTab("Canchas", panelCanchas);
    }

    private void buildPanelClientes() {
        panelClientes = new JPanel(new BorderLayout(10,10));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = baseGC();

        // --- INICIO DE MODIFICACIÓN ---
        txtIdCliente = new JTextField(8);
        txtIdCliente.setText("(autogenerado)");
        txtIdCliente.setEnabled(false);
        
        txtNombreCliente = new JTextField(18);
        txtTelefono = new JTextField(12);
        btnAgregarCliente = new JButton("Agregar cliente");
        btnModificarCliente = new JButton("Modificar cliente");
        btnEliminarCliente = new JButton("Eliminar cliente");
        btnLimpiarCliente = new JButton("Limpiar");

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        acciones.add(btnAgregarCliente);
        acciones.add(btnModificarCliente);
        acciones.add(btnEliminarCliente);
        acciones.add(btnLimpiarCliente);

        addRow(form, gc, 0, new JLabel("ID:"), txtIdCliente); // Fila 0: ID
        addRow(form, gc, 1, new JLabel("Nombre:"), txtNombreCliente); // Fila 1: Nombre
        addRow(form, gc, 2, new JLabel("Teléfono:"), txtTelefono); // Fila 2: Teléfono
        addRow(form, gc, 3, new JLabel("Acción:"), acciones); // Fila 3: Panel de botones
        // --- FIN DE MODIFICACIÓN ---

        panelClientes.add(form, BorderLayout.NORTH);

        modelClientes = new DefaultTableModel(new String[]{"ID","Nombre","Teléfono"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblClientes = new JTable(modelClientes);
        panelClientes.add(new JScrollPane(tblClientes), BorderLayout.CENTER);

        // --- INICIO DE MODIFICACIÓN: Listeners ---
        btnAgregarCliente.addActionListener(e -> onAgregarCliente());
        btnModificarCliente.addActionListener(e -> onModificarCliente());
        btnEliminarCliente.addActionListener(e -> onEliminarCliente());
        btnLimpiarCliente.addActionListener(e -> onLimpiarCliente());

        // Listener para la tabla (cargar datos al formulario)
        tblClientes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onTablaClienteSeleccionada();
            }
        });
        
        // Estado inicial de los botones
        onLimpiarCliente();
        // --- FIN DE MODIFICACIÓN ---

        tabs.addTab("Clientes", panelClientes);
    }

    // -----------------------------------------------------------
    // Handlers de NEGOCIO (REFACTORIZADOS - usan DAO)
    // -----------------------------------------------------------

    /**
     * (NUEVO MÉTODO)
     * Muestra u oculta los campos específicos de Reserva Fija
     * basado en la selección del radio button.
     */
    private void actualizarVisibilidadCamposFijos() {
        boolean esFija = rbFija.isSelected();
        
        // Muestra u oculta los 3 labels y los 3 campos
        lblDiaSemana.setVisible(esFija);
        cmbDiaSemana.setVisible(esFija);
        
        lblFechaFin.setVisible(esFija);
        // --- INICIO DE MODIFICACIÓN ---
        // ftfFechaFin.setVisible(esFija); // Reemplazado
        jdcFechaFin.setVisible(esFija);
        // --- FIN DE MODIFICACIÓN ---
        
        lblDescuento.setVisible(esFija);
        spDescuento.setVisible(esFija);
    }

    /**
     * (NUEVO MÉTODO)
     * Filtra la lista de canchas en el combo 'cmbCancha'
     * basado en el deporte seleccionado en 'cmbDeporteReserva'.
     */
    private void filtrarCanchasPorDeporte() {
        String deporteSeleccionado = (String) cmbDeporteReserva.getSelectedItem();
        
        // Guarda la cancha seleccionada actualmente, si hay una
        Cancha canchaPreviamenteSeleccionada = (Cancha) cmbCancha.getSelectedItem();
        
        // Limpia el combo de canchas
        cmbCancha.removeAllItems();
        
        Cancha canchaParaSeleccionar = null;
        
        // Itera sobre la lista MAESTRA de canchas (que está en memoria)
        for (Cancha c : canchas) {
            boolean coincide = "Todos".equals(deporteSeleccionado) || 
                               c.getDeporte().equalsIgnoreCase(deporteSeleccionado);
            
            if (coincide) {
                cmbCancha.addItem(c);
                
                // Si esta cancha era la que estaba seleccionada, la marca para re-seleccionarla
                if (canchaPreviamenteSeleccionada != null && c.getIdCancha() == canchaPreviamenteSeleccionada.getIdCancha()) {
                    canchaParaSeleccionar = c;
                }
            }
        }
        
        // Si había una cancha seleccionada y todavía está en la lista filtrada,
        // la vuelve a seleccionar.
        if (canchaParaSeleccionar != null) {
            cmbCancha.setSelectedItem(canchaParaSeleccionar);
        }
    }


    /**
     * (MODIFICADO)
     * Crea un objeto de negocio (Reserva) y lo pasa al DAO para guardarlo.
     * Ya no contiene SQL.
     * (CORRECCIÓN) Llama a onListarReservasDia() después de una
     * inserción simple para resincronizar la tabla.
     * (MODIFICADO) Lee la fecha desde JDateChooser.
     */
    private void onRegistrarReserva() {
        // 1. Obtener datos de la GUI
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();

        if (cancha == null || cliente == null) {
            JOptionPane.showMessageDialog(this, "Debe seleccionar un deporte, una cancha y un cliente.");
            return;
        }

        // --- INICIO DE MODIFICACIÓN ---
        // LocalDate fecha = parseFecha(ftfFecha.getText()); // Reemplazado
        LocalDate fecha = parseDateChooser(jdcFecha); // Lee del JDateChooser
        // --- FIN DE MODIFICACIÓN ---
        
        LocalTime hora = parseHora(ftfHora.getText());
        
        if (fecha == null) {
             JOptionPane.showMessageDialog(this, "La fecha ingresada no es válida.");
             return;
        }
        if (hora == null) {
             JOptionPane.showMessageDialog(this, "La hora ingresada no es válida (HH:mm).");
             return;
        }
        
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);
        int duracion = (int) spDuracion.getValue();

        // 2. Crear el objeto de Negocio (aún no tiene ID)
        Reserva nuevaReserva;
        if (rbSimple.isSelected()) {
            nuevaReserva = new ReservaSimple(0, inicio, cancha, cliente, duracion);
        } else {
            // --- INICIO DE MODIFICACIÓN ---
            // LocalDate fechaFin = parseFecha(ftfFechaFin.getText()); // Reemplazado
            LocalDate fechaFin = parseDateChooser(jdcFechaFin); // Lee del JDateChooser
            // --- FIN DE MODIFICACIÓN ---

            if (fechaFin == null) {
                 JOptionPane.showMessageDialog(this, "La fecha de fin para la reserva fija no es válida.");
                return;
            }
            DayOfWeek dia = (DayOfWeek) cmbDiaSemana.getSelectedItem();
            double descuento = ((Number) spDescuento.getValue()).doubleValue();
            
            ReservaFija fija = new ReservaFija(0, inicio, cancha, cliente, dia, fechaFin, descuento);
            fija.setDuracionMinutos(duracion); // Asegurarse que use la duración del spinner
            nuevaReserva = fija;
        }
        
        // 3. Enviar el objeto a la Capa de Datos (DAO)
        // El DAO ahora se encarga de la lógica de expansión o de transacción
        int resultado = reservaDAO.registrarReserva(nuevaReserva);

        // 4. Actualizar la GUI si el DAO tuvo éxito
        if (resultado != -1) {
            
            if (nuevaReserva instanceof ReservaSimple) {
                // Éxito de Reserva Simple (resultado es el idGenerado)
                JOptionPane.showMessageDialog(this, "Reserva guardada correctamente.");
                onListarReservasDia(); 
                
            } else {
                // Éxito de Reserva Fija (resultado es la cantidad de filas)
                JOptionPane.showMessageDialog(this, "Se guardaron " + resultado + " reservas fijas correctamente.");
                // Actualizamos la tabla para ver las nuevas reservas (solo las de hoy)
                onListarReservasDia();
            }
            
        } else {
            // El DAO retornó -1 (error o conflicto)
            JOptionPane.showMessageDialog(this, 
                "Error al guardar la reserva. Verifique la consola por conflictos de horario.",
                "Error de Reserva", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * (NUEVO)
     * Carga las reservas de una fecha, las guarda en la lista interna
     * y puebla la tabla.
     */
    private void cargarReservasDelDia(LocalDate fecha) {
        // Limpia la tabla y la lista interna
        modelReservas.setRowCount(0);
        reservasMostradasEnTabla.clear();
        
        // Obtiene las reservas del DAO
        reservasMostradasEnTabla.addAll(reservaDAO.obtenerReservasPorFecha(fecha));
        
        // Puebla la tabla
        for (Reserva r : reservasMostradasEnTabla) {
            // El tipo "Fija" lo determinamos si tiene un ID de grupo
            String tipo = (r.esParteDeGrupo()) ? "Fija" : "Simple";
            agregarFila(r, tipo, r.calcularCostoTotal()); // (CORREGIDO) Pasa el costo
        }
    }

    /**
     * (ACTUALIZADO)
     * Cancela una reserva usando el DAO.
     * Si la reserva es parte de un grupo, pregunta al usuario qué desea hacer.
     */
    private void onCancelarReserva() {
        int filaSeleccionada = tblReservas.getSelectedRow();
        if (filaSeleccionada < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione una reserva de la tabla para cancelar.");
            return;
        }
        
        // 1. Obtener el objeto Reserva COMPLETO (gracias a la lista interna)
        Reserva reservaSeleccionada = reservasMostradasEnTabla.get(filaSeleccionada);
        int idReserva = reservaSeleccionada.getIdReserva();
        
        boolean exito = false;
        int reservasCanceladas = 0;

        if (reservaSeleccionada.esParteDeGrupo()) {
            // 2. Si es parte de un grupo, PREGUNTAR al usuario
            String[] opciones = {"Cancelar solo este día", "Cancelar TODA la serie", "No hacer nada"};
            int eleccion = JOptionPane.showOptionDialog(
                    this,
                    "Esta reserva es parte de una serie fija.\n¿Qué desea cancelar?",
                    "Cancelar Reserva Fija",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    opciones[0]);

            if (eleccion == 0) { // "Cancelar solo este día"
                exito = reservaDAO.cancelarReservaUnica(idReserva);
                if (exito) reservasCanceladas = 1;
            } else if (eleccion == 1) { // "Cancelar TODA la serie"
                int resultado = reservaDAO.cancelarReservaGrupo(idReserva);
                if (resultado != -1) {
                    exito = true;
                    reservasCanceladas = resultado;
                }
            } else { // "No hacer nada" o cerró el diálogo
                return; 
            }
            
        } else {
            // 3. Si es una reserva simple, solo confirmar
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "¿Está seguro de que desea cancelar esta reserva?",
                    "Cancelar Reserva",
                    JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                exito = reservaDAO.cancelarReservaUnica(idReserva);
                 if (exito) reservasCanceladas = 1;
            }
        }

        // 4. Actualizar la GUI si el DAO tuvo éxito
        if (exito) {
            if (reservasCanceladas > 1) {
                JOptionPane.showMessageDialog(this, "Se cancelaron " + reservasCanceladas + " reservas de la serie.");
                // Recargamos la lista completa
                onListarReservasDia(); 
            } else {
                JOptionPane.showMessageDialog(this, "Reserva ID " + idReserva + " cancelada.");
                // Solo removemos la fila seleccionada
                reservasMostradasEnTabla.remove(filaSeleccionada);
                modelReservas.removeRow(filaSeleccionada);
            }
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo cancelar la reserva.");
        }
    }


    /**
     * (MODIFICADO)
     * Lista las reservas de un día específico usando el DAO.
     * Lee la fecha desde JDateChooser.
     */
    private void onListarReservasDia() {
        // --- INICIO DE MODIFICACIÓN ---
        // LocalDate fecha = parseFecha(ftfFecha.getText()); // Reemplazado
        LocalDate fecha = parseDateChooser(jdcFecha);
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una fecha válida para listar.");
            return;
        }
        // --- FIN DE MODIFICACIÓN ---
        
        // Llama al método que puebla la lista interna y la tabla
        cargarReservasDelDia(fecha);
    }

    /**
     * (MODIFICADO)
     * Calcula el costo (sin guardar) usando el objeto de negocio.
     * Lee la fecha desde JDateChooser.
     */
    private void onCalcularCosto() {
        Cancha cancha = (Cancha) cmbCancha.getSelectedItem();
        Cliente cliente = (Cliente) cmbCliente.getSelectedItem();
        if (cancha == null || cliente == null) return;
        
        // --- INICIO DE MODIFICACIÓN ---
        // LocalDate fecha = parseFecha(ftfFecha.getText()); // Reemplazado
        LocalDate fecha = parseDateChooser(jdcFecha); // Lee del JDateChooser
        // --- FIN DE MODIFICACIÓN ---
        
        LocalTime hora = parseHora(ftfHora.getText());
        if (fecha == null || hora == null) {
            JOptionPane.showMessageDialog(this, "Fecha u hora no válida para calcular.");
            return;
        }
        
        LocalDateTime inicio = LocalDateTime.of(fecha, hora);

        double costo;
        if (rbSimple.isSelected()) {
            ReservaSimple tmp = new ReservaSimple(0, inicio, cancha, cliente, (int) spDuracion.getValue());
            costo = tmp.calcularCostoTotal();
        } else {
            // --- INICIO DE MODIFICACIÓN ---
            // LocalDate fechaFin = parseFecha(ftfFechaFin.getText()); // Reemplazado
            LocalDate fechaFin = parseDateChooser(jdcFechaFin); // Lee del JDateChooser
            // --- FIN DE MODIFICACIÓN ---

            if (fechaFin == null) {
                JOptionPane.showMessageDialog(this, "La fecha de fin no es válida para calcular el costo.");
                return;
            }
            
            // (CORREGIDO) Instancia de Fija ahora usa la duración y descuento correctos
            ReservaFija tmp = new ReservaFija(0, inicio, cancha, cliente,
                    (DayOfWeek) cmbDiaSemana.getSelectedItem(),
                    fechaFin,
                    ((Number) spDescuento.getValue()).doubleValue());
            tmp.setDuracionMinutos((int) spDuracion.getValue());
            
            // El costo total de una fija se calcula diferente
            // (El DAO se encarga de calcular el costo total de la *serie*)
            // Aquí mostramos el costo *por turno*
            costo = tmp.calcularCostoTotal(); 
            
            List<LocalDate> ocurrencias = tmp.generarOcurrencias(inicio.toLocalDate(), fechaFin);
            int numOcurrencias = ocurrencias.isEmpty() ? 1 : ocurrencias.size();
            
            JOptionPane.showMessageDialog(this, "Costo por turno: " + F_MONEDA_AR.format(costo) + "\n"
                    + "Número de ocurrencias: " + numOcurrencias + "\n"
                    + "Costo TOTAL (serie): " + F_MONEDA_AR.format(costo * numOcurrencias));
            return;
        }
        JOptionPane.showMessageDialog(this, "Costo estimado: " + F_MONEDA_AR.format(costo));
    }

    /**
     * (MODIFICADO)
     * Consulta disponibilidad usando el DAO y puebla la JTable.
     * Lee la fecha desde JDateChooser.
     */
    private void onConsultarDisponibilidad() {
        Cancha cancha = (Cancha) cmbCanchaDisp.getSelectedItem();
        if (cancha == null) return;
        
        // --- INICIO DE MODIFICACIÓN ---
        // LocalDate fecha = parseFecha(ftfFechaDisp.getText()); // Reemplazado
        LocalDate fecha = parseDateChooser(jdcFechaDisp); // Lee del JDateChooser
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una fecha válida para consultar.");
            return;
        }
        // --- FIN DE MODIFICACIÓN ---
        
        // Llama al DAO
        List<LocalTime> libres = reservaDAO.consultarDisponibilidad(cancha.getIdCancha(), fecha);
        
        modelHoras.setRowCount(0); // Limpia la tabla
        
        for (LocalTime t : libres) {
            modelHoras.addRow(new Object[]{ t.format(F_HORA) }); // Añade fila a la tabla
        }
    }

    /**
     * (MODIFICADO)
     * Agrega una cancha usando el DAO.
     */
    private void onAgregarCancha() {
        String nom = txtNombreCancha.getText();
        String dep = (String) cmbDeporteCancha.getSelectedItem();
        
        Number numPrecio = (Number) ftfPrecioHora.getValue();
        double precio = (numPrecio != null) ? numPrecio.doubleValue() : 0.0;

        if (nom == null || nom.trim().isEmpty() || dep == null) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre y seleccionar un deporte.");
            return;
        }

        // 1. Crear objeto de negocio
        Cancha c = new Cancha(0, nom, dep, precio); // ID 0, la BD lo genera

        // 2. Enviar al DAO
        int idGenerado = canchaDAO.agregarCancha(c);

        // 3. Actualizar GUI
        if (idGenerado != -1) {
            // El DAO actualizó el ID en el objeto 'c'
            canchas.add(c);
            cmbCancha.addItem(c);
            cmbCanchaDisp.addItem(c);
            modelCanchas.addRow(new Object[]{c.getIdCancha(), c.getNombre(), c.getDeporte(), c.getPrecioPorHora()});
            
            // Refresca el combo de filtro en reservas
            filtrarCanchasPorDeporte();
            
            // txtIdCancha.setText(String.valueOf(idGenerado)); // No es necesario, onLimpiar lo hace
            // txtNombreCancha.setText("");
            // cmbDeporteCancha.setSelectedIndex(0);
            // ftfPrecioHora.setValue(null);
            onLimpiarCancha(); // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar la cancha.");
        }
    }
    
    /**
     * (NUEVO)
     * Se activa al hacer clic en una fila de la tabla de canchas.
     * Carga los datos de esa fila en el formulario.
     */
    private void onTablaCanchaSeleccionada() {
        int fila = tblCanchas.getSelectedRow();
        if (fila == -1) {
            // Si no hay fila seleccionada (ej. después de limpiar), no hace nada
            return;
        }

        // Obtener datos del modelo de la tabla
        String id = modelCanchas.getValueAt(fila, 0).toString();
        String nombre = modelCanchas.getValueAt(fila, 1).toString();
        String deporte = modelCanchas.getValueAt(fila, 2).toString();
        Object precio = modelCanchas.getValueAt(fila, 3); // Obtener como Object

        // Cargar datos en los campos del formulario
        txtIdCancha.setText(id);
        txtNombreCancha.setText(nombre);
        cmbDeporteCancha.setSelectedItem(deporte);
        ftfPrecioHora.setValue(precio); // Asignar el Object directamente

        // Actualizar estado de botones
        btnAgregarCancha.setEnabled(false);
        btnModificarCancha.setEnabled(true);
        btnEliminarCancha.setEnabled(true);
    }
    
    /**
     * (NUEVO)
     * Limpia el formulario de canchas y restaura el estado de los botones.
     */
    private void onLimpiarCancha() {
        txtIdCancha.setText("(autogenerado)");
        txtNombreCancha.setText("");
        cmbDeporteCancha.setSelectedIndex(0);
        ftfPrecioHora.setValue(null);
        
        tblCanchas.clearSelection(); // Deselecciona cualquier fila de la tabla
        
        // Estado inicial de botones
        btnAgregarCancha.setEnabled(true);
        btnModificarCancha.setEnabled(false);
        btnEliminarCancha.setEnabled(false);
    }

    /**
     * (NUEVO)
     * Llama al DAO para modificar la cancha seleccionada.
     */
    private void onModificarCancha() {
        String idStr = txtIdCancha.getText();
        int idCancha;
        
        try {
            idCancha = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Seleccione una cancha válida de la tabla para modificar.");
            return;
        }

        String nom = txtNombreCancha.getText();
        String dep = (String) cmbDeporteCancha.getSelectedItem();
        Number numPrecio = (Number) ftfPrecioHora.getValue();
        double precio = (numPrecio != null) ? numPrecio.doubleValue() : 0.0;

        if (nom == null || nom.trim().isEmpty() || dep == null) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre y seleccionar un deporte.");
            return;
        }

        // 1. Crear objeto de negocio
        Cancha canchaModificada = new Cancha(idCancha, nom, dep, precio);

        // 2. Enviar al DAO
        boolean exito = canchaDAO.modificarCancha(canchaModificada);

        // 3. Actualizar GUI
        if (exito) {
            JOptionPane.showMessageDialog(this, "Cancha modificada correctamente.");
            cargarCanchasDesdeDB(); // Recarga toda la tabla y los combos
            onLimpiarCancha();     // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, "Error al modificar la cancha.");
        }
    }
    
    /**
     * (NUEVO)
     * Llama al DAO para eliminar la cancha seleccionada.
     */
    private void onEliminarCancha() {
        String idStr = txtIdCancha.getText();
        int idCancha;
        
        try {
            idCancha = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Seleccione una cancha válida de la tabla para eliminar.");
            return;
        }

        // Confirmación
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea eliminar la cancha '" + txtNombreCancha.getText() + "' (ID: " + idCancha + ")?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 1. Enviar al DAO
        boolean exito = canchaDAO.eliminarCancha(idCancha);

        // 2. Actualizar GUI
        if (exito) {
            JOptionPane.showMessageDialog(this, "Cancha eliminada correctamente.");
            cargarCanchasDesdeDB(); // Recarga toda la tabla y los combos
            onLimpiarCancha();     // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Error al eliminar la cancha.\nEs posible que tenga reservas asociadas.",
                    "Error de Eliminación",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * (REFACTORIZADO)
     * Agrega un cliente usando el DAO.
     * (MODIFICADO) Llama a onLimpiarCliente al finalizar.
     */
    private void onAgregarCliente() {
        String nom = txtNombreCliente.getText();
        String tel = txtTelefono.getText();

        if (nom == null || nom.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre válido");
            return;
        }

        // 1. Crear objeto de negocio
        Cliente cli = new Cliente(0, nom, tel);

        // 2. Enviar al DAO
        int idGenerado = clienteDAO.agregarCliente(cli);

        // 3. Actualizar GUI
        if (idGenerado != -1) {
            // El DAO actualizó el ID en el objeto 'cli'
            clientes.add(cli);
            cmbCliente.addItem(cli);
            modelClientes.addRow(new Object[]{cli.getIdCliente(), cli.getNombreCliente(), cli.getTelefono()});
            
            // txtNombreCliente.setText("");
            // txtTelefono.setText("");
            onLimpiarCliente(); // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, "Error al guardar el cliente.");
        }
    }
       
    // --- INICIO DE MÉTODOS NUEVOS PARA CLIENTES ---
    
    /**
     * (NUEVO)
     * Se activa al hacer clic en una fila de la tabla de clientes.
     * Carga los datos de esa fila en el formulario.
     */
    private void onTablaClienteSeleccionada() {
        int fila = tblClientes.getSelectedRow();
        if (fila == -1) {
            return;
        }

        // Obtener datos del modelo de la tabla
        String id = modelClientes.getValueAt(fila, 0).toString();
        String nombre = modelClientes.getValueAt(fila, 1).toString();
        String telefono = modelClientes.getValueAt(fila, 2).toString();

        // Cargar datos en los campos del formulario
        txtIdCliente.setText(id);
        txtNombreCliente.setText(nombre);
        txtTelefono.setText(telefono);

        // Actualizar estado de botones
        btnAgregarCliente.setEnabled(false);
        btnModificarCliente.setEnabled(true);
        btnEliminarCliente.setEnabled(true);
    }
    
    /**
     * (NUEVO)
     * Limpia el formulario de clientes y restaura el estado de los botones.
     */
    private void onLimpiarCliente() {
        txtIdCliente.setText("(autogenerado)");
        txtNombreCliente.setText("");
        txtTelefono.setText("");
        
        tblClientes.clearSelection(); // Deselecciona cualquier fila de la tabla
        
        // Estado inicial de botones
        btnAgregarCliente.setEnabled(true);
        btnModificarCliente.setEnabled(false);
        btnEliminarCliente.setEnabled(false);
    }

    /**
     * (NUEVO)
     * Llama al DAO para modificar el cliente seleccionado.
     */
    private void onModificarCliente() {
        String idStr = txtIdCliente.getText();
        int idCliente;
        
        try {
            idCliente = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente válido de la tabla para modificar.");
            return;
        }

        String nom = txtNombreCliente.getText();
        String tel = txtTelefono.getText();

        if (nom == null || nom.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe ingresar un nombre.");
            return;
        }

        // 1. Crear objeto de negocio
        Cliente clienteModificado = new Cliente(idCliente, nom, tel);

        // 2. Enviar al DAO
        boolean exito = clienteDAO.modificarCliente(clienteModificado);

        // 3. Actualizar GUI
        if (exito) {
            JOptionPane.showMessageDialog(this, "Cliente modificado correctamente.");
            cargarClientesDesdeDB(); // Recarga toda la tabla y los combos
            onLimpiarCliente();     // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, "Error al modificar el cliente.");
        }
    }
    
    /**
     * (NUEVO)
     * Llama al DAO para eliminar el cliente seleccionado.
     */
    private void onEliminarCliente() {
        String idStr = txtIdCliente.getText();
        int idCliente;
        
        try {
            idCliente = Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente válido de la tabla para eliminar.");
            return;
        }

        // Confirmación
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de que desea eliminar al cliente '" + txtNombreCliente.getText() + "' (ID: " + idCliente + ")?",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // 1. Enviar al DAO
        boolean exito = clienteDAO.eliminarCliente(idCliente);

        // 2. Actualizar GUI
        if (exito) {
            JOptionPane.showMessageDialog(this, "Cliente eliminado correctamente.");
            cargarClientesDesdeDB(); // Recarga toda la tabla y los combos
            onLimpiarCliente();     // Limpia el formulario
        } else {
            JOptionPane.showMessageDialog(this, 
                    "Error al eliminar el cliente.\nEs posible que tenga reservas asociadas.",
                    "Error de Eliminación",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    // --- FIN DE MÉTODOS NUEVOS PARA CLIENTES ---
    
    // -----------------------------------------------------------
    // Utilidades de la GUI
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

    // --- MÉTODO parseFecha(String s) ELIMINADO ---

    /**
     * (NUEVO MÉTODO)
     * Convierte la fecha de un JDateChooser a LocalDate.
     * @param chooser El componente JDateChooser
     * @return El LocalDate, o null si no se seleccionó fecha.
     */
    private LocalDate parseDateChooser(JDateChooser chooser) {
        Date date = chooser.getDate();
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
    
    private LocalTime parseHora(String s)  { 
        try {
            return LocalTime.parse(s.trim(), F_HORA); 
        } catch (DateTimeParseException e) {
            return null; // Devuelve null si el formato es incorrecto
        }
    }
    
    /**
     * Agrega una fila a la tabla de reservas formateando las fechas y costos.
     * (CORREGIDO) Acepta el costo como parámetro.
     */
    private void agregarFila(Reserva r, String tipo, double costoGuardado) {
        String inicioStr = r.getFechaHoraInicio().format(F_FECHA_HORA_MOSTRAR);
        String finStr = r.getFechaHoraFin().format(F_FECHA_HORA_MOSTRAR);
        String costoStr = F_MONEDA_AR.format(costoGuardado);

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

}



