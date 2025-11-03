package com.gestor.datos;

import com.gestor.negocio.*; // Importa todos los modelos de negocio
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la entidad Reserva.
 * Encapsula todo el acceso a la base de datos (SQL) para la tabla 'reserva'.
 * Reemplaza la lógica de archivos (.dat) y la lógica SQL de MainFrame.
 */
public class ReservaDAO {

    /**
     * Registra una nueva reserva (simple o fija) en la base de datos.
     * (Lógica extraída de onRegistrarReserva en MainFrame)
     * @param reserva El objeto Reserva (puede ser ReservaSimple o ReservaFija)
     * @return El ID autogenerado por la BD, o -1 si falló.
     */
    public int registrarReserva(Reserva reserva) {
        String sql = "INSERT INTO reserva (id_cancha, id_cliente, fecha_hora_inicio, duracion_minutos, tipo, fecha_fin, dia_semana, descuento_aplicado, costo_total) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int idGenerado = -1;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, reserva.getCancha().getIdCancha());
            ps.setInt(2, reserva.getCliente().getIdCliente());
            ps.setTimestamp(3, Timestamp.valueOf(reserva.getFechaHoraInicio()));
            ps.setInt(4, reserva.getDuracionMinutos());
            ps.setDouble(9, reserva.calcularCostoTotal());

            // Diferenciar entre ReservaSimple y ReservaFija
            if (reserva instanceof ReservaFija) {
                ReservaFija fija = (ReservaFija) reserva;
                ps.setString(5, "Fija");
                ps.setDate(6, Date.valueOf(fija.getFechaFin()));
                ps.setString(7, fija.getDiaDeLaSemana().name());
                ps.setDouble(8, fija.getDescuentoAplicado());
            } else {
                ps.setString(5, "Simple");
                ps.setNull(6, Types.DATE);
                ps.setNull(7, Types.VARCHAR);
                ps.setDouble(8, 0); // Sin descuento para simple
            }

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idGenerado = rs.getInt(1);
                    reserva.setIdReserva(idGenerado); // Actualiza el ID en el objeto
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al guardar la reserva: " + e.getMessage());
            // Podríamos lanzar una excepción aquí
        }
        return idGenerado;
    }

    /**
     * Cancela (elimina) una reserva de la base de datos por su ID.
     * (Lógica reimplementada de cancelarReserva en Reserva.java)
     * @param idReserva El ID de la reserva a eliminar.
     * @return true si se eliminó, false si no.
     */
    public boolean cancelarReserva(int idReserva) {
        String sql = "DELETE FROM reserva WHERE id_reserva = ?";
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setInt(1, idReserva);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error al cancelar la reserva: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene una lista de TODAS las reservas de una fecha específica.
     * Esta consulta es compleja porque "re-hidrata" los objetos Cancha y Cliente.
     * (Lógica adaptada de cargarReservasDesdeDB en MainFrame y obtenerReservasPorFecha en Reserva)
     */
    public List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        List<Reserva> reservas = new ArrayList<>();
        // Un JOIN complejo para traer todos los datos necesarios de una vez
        String sql = "SELECT r.*, " +
                     "c.nombre AS cancha_nombre, c.deporte, c.precio_por_hora, " +
                     "cl.nombre AS cliente_nombre, cl.telefono " +
                     "FROM reserva r " +
                     "JOIN cancha c ON r.id_cancha = c.id_cancha " +
                     "JOIN cliente cl ON r.id_cliente = cl.id " +
                     "WHERE DATE(r.fecha_hora_inicio) = ? " +
                     "ORDER BY r.fecha_hora_inicio";

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(fecha));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // 1. Re-hidratar la Cancha
                    Cancha cancha = new Cancha(
                        rs.getInt("id_cancha"),
                        rs.getString("cancha_nombre"),
                        rs.getString("deporte"),
                        rs.getDouble("precio_por_hora")
                    );
                    // 2. Re-hidratar el Cliente
                    Cliente cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("cliente_nombre"),
                        rs.getString("telefono")
                    );
                    // 3. Re-hidratar la Reserva (Simple o Fija)
                    reservas.add(hidratarReserva(rs, cancha, cliente));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por fecha: " + e.getMessage());
        }
        return reservas;
    }

    /**
     * Consulta la disponibilidad de una cancha en una fecha específica.
     * (Lógica reimplementada de consultarDisponibilidad en Reserva.java)
     */
    public List<LocalTime> consultarDisponibilidad(int idCancha, LocalDate fecha) {
        // 1. Obtener las reservas existentes para esa cancha y fecha
        List<Reserva> reservasDelDia = obtenerReservasPorFechaYCancha(fecha, idCancha);

        // 2. Definir la ventana de horarios (igual que antes)
        List<LocalTime> libres = new ArrayList<>();
        LocalTime t = LocalTime.of(8, 0); // TODO: Esto debería venir de HorarioLaboral
        LocalTime fin = LocalTime.of(23, 0); // TODO: Esto debería venir de HorarioLaboral
        int duracionTurno = 60; // TODO: Esto debería venir de HorarioLaboral

        while (!t.isAfter(fin.minusMinutes(duracionTurno))) {
            boolean ocupado = false;
            LocalDateTime inicioSlot = LocalDateTime.of(fecha, t);
            LocalDateTime finSlot = inicioSlot.plusMinutes(duracionTurno);
            
            // 3. Crear una "reserva fantasma" para usar la lógica de negocio .solapa()
            Reserva slotFantasma = new ReservaSimple(0, inicioSlot, null, null, duracionTurno);
            // (Nota: le pasamos null a cancha/cliente porque .solapa() solo necesita los tiempos)
            
            for (Reserva r : reservasDelDia) {
                // Asignamos la misma cancha para que la lógica de solapamiento funcione
                slotFantasma.setCancha(r.getCancha()); 
                
                if (r.solapa(slotFantasma)) { 
                    ocupado = true;
                    break; 
                }
            }
            
            if (!ocupado) {
                libres.add(t);
            }
            t = t.plusMinutes(duracionTurno);
        }
        return libres;
    }

    /**
     * Método auxiliar para obtener reservas de una cancha y fecha específicas.
     */
    private List<Reserva> obtenerReservasPorFechaYCancha(LocalDate fecha, int idCancha) {
        List<Reserva> reservas = new ArrayList<>();
        String sql = "SELECT r.*, " +
                     "c.nombre AS cancha_nombre, c.deporte, c.precio_por_hora, " +
                     "cl.nombre AS cliente_nombre, cl.telefono " +
                     "FROM reserva r " +
                     "JOIN cancha c ON r.id_cancha = c.id_cancha " +
                     "JOIN cliente cl ON r.id_cliente = cl.id " +
                     "WHERE DATE(r.fecha_hora_inicio) = ? AND r.id_cancha = ?";

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setDate(1, Date.valueOf(fecha));
            ps.setInt(2, idCancha);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cancha cancha = new Cancha(rs.getInt("id_cancha"), rs.getString("cancha_nombre"), rs.getString("deporte"), rs.getDouble("precio_por_hora"));
                    Cliente cliente = new Cliente(rs.getInt("id_cliente"), rs.getString("cliente_nombre"), rs.getString("telefono"));
                    reservas.add(hidratarReserva(rs, cancha, cliente));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por fecha/cancha: " + e.getMessage());
        }
        return reservas;
    }

    /**
     * Helper para construir un objeto Reserva (Simple o Fija) desde un ResultSet.
     * @param rs El ResultSet posicionado en la fila actual.
     * @param cancha El objeto Cancha ya construido.
     * @param cliente El objeto Cliente ya construido.
     * @return Un objeto ReservaSimple o ReservaFija.
     * @throws SQLException
     */
    private Reserva hidratarReserva(ResultSet rs, Cancha cancha, Cliente cliente) throws SQLException {
        String tipo = rs.getString("tipo");
        int idReserva = rs.getInt("id_reserva");
        LocalDateTime inicio = rs.getTimestamp("fecha_hora_inicio").toLocalDateTime();
        int duracion = rs.getInt("duracion_minutos");

        if ("Fija".equalsIgnoreCase(tipo)) {
            LocalDate fechaFin = rs.getDate("fecha_fin").toLocalDate();
            DayOfWeek dia = DayOfWeek.valueOf(rs.getString("dia_semana"));
            double descuento = rs.getDouble("descuento_aplicado");
            
            ReservaFija fija = new ReservaFija(idReserva, inicio, cancha, cliente, dia, fechaFin, descuento);
            fija.setDuracionMinutos(duracion); // Importante
            return fija;
        } else {
            return new ReservaSimple(idReserva, inicio, cancha, cliente, duracion);
        }
    }
}
