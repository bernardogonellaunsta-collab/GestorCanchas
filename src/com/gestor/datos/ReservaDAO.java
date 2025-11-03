package com.gestor.datos;

import com.gestor.negocio.Cliente;
import com.gestor.negocio.Cancha;
import com.gestor.negocio.Reserva;
import com.gestor.negocio.ReservaFija;
import com.gestor.negocio.ReservaSimple;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * (ACTUALIZADO)
 * Clase DAO para manejar toda la lógica de persistencia de Reservas en MySQL.
 * Incluye lógica de transacciones y manejo de grupos para Reservas Fijas.
 */
public class ReservaDAO {

    /**
     * Método principal para registrar una reserva.
     * Delega a métodos específicos si es Simple o Fija.
     *
     * @param reserva El objeto de reserva (Simple o Fija)
     * @return El ID (si es Simple) o la cantidad de reservas (si es Fija). -1 si hay error.
     */
    public int registrarReserva(Reserva reserva) {
        if (reserva instanceof ReservaSimple) {
            return registrarReservaSimple((ReservaSimple) reserva, null, null); // Sin transacción, sin grupo
        } else if (reserva instanceof ReservaFija) {
            return registrarReservaFija((ReservaFija) reserva);
        }
        return -1;
    }

    /**
     * Registra una única ReservaSimple.
     * Acepta un idGrupoFija para enlazar reservas fijas.
     *
     * @param reserva La reserva simple a guardar
     * @param cn La conexión existente (si es parte de una transacción) o null
     * @param idGrupoFija El ID del grupo al que pertenece, o null si es simple
     * @return El ID generado, o -1 si falla
     */
    private int registrarReservaSimple(ReservaSimple reserva, Connection cn, Integer idGrupoFija) {
        // SQL MODIFICADO: Agrega id_grupo_fija
        String sql = "INSERT INTO reserva (id_cancha, id_cliente, fecha_hora_inicio, duracion_minutos, tipo, costo_total, id_grupo_fija) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int idGenerado = -1;
        
        Connection connLocal = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            if (cn == null) {
                // No estamos en una transacción, creamos una conexión nueva
                connLocal = ConexionDB.conectar();
            } else {
                // Estamos en una transacción, usamos la conexión provista
                connLocal = cn;
            }

            ps = connLocal.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, reserva.getCancha().getIdCancha());
            ps.setInt(2, reserva.getCliente().getIdCliente());
            ps.setTimestamp(3, Timestamp.valueOf(reserva.getFechaHoraInicio()));
            ps.setInt(4, reserva.getDuracionMinutos());
            ps.setString(5, "Simple"); // Todas las reservas en la BD son 'simples'
            ps.setDouble(6, reserva.calcularCostoTotal()); 

            // Asignar el ID de grupo
            if (idGrupoFija == null) {
                ps.setNull(7, java.sql.Types.INTEGER);
            } else {
                ps.setInt(7, idGrupoFija);
            }

            int filas = ps.executeUpdate();
            if (filas > 0) {
                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    idGenerado = rs.getInt(1);
                    reserva.setIdReserva(idGenerado); // Actualiza el objeto
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al registrar reserva simple: " + e.getMessage());
            return -1; // Indica error
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                // SOLO cerramos la conexión si la creamos localmente
                if (cn == null && connLocal != null) {
                    connLocal.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar recursos: " + e.getMessage());
            }
        }
        return idGenerado;
    }

    /**
     * Registra una ReservaFija expandiéndola en múltiples ReservasSimples
     * y enlazándolas con un ID de grupo dentro de una transacción.
     *
     * @param fija La plantilla de ReservaFija
     * @return La cantidad de reservas insertadas, o -1 si hay un conflicto o error.
     */
    private int registrarReservaFija(ReservaFija fija) {
        // 1. Generar todas las fechas (lógica de negocio)
        List<LocalDate> ocurrencias = fija.generarOcurrencias(
                fija.getFechaHoraInicio().toLocalDate(),
                fija.getFechaFin()
        );

        if (ocurrencias.isEmpty()) {
            System.err.println("No se encontraron ocurrencias para la reserva fija.");
            return -1;
        }

        List<ReservaSimple> reservasAGuardar = new ArrayList<>();
        LocalTime hora = fija.getFechaHoraInicio().toLocalTime();
        int duracion = fija.getDuracionMinutos();
        double costoConDescuento = fija.calcularCostoTotal() / ocurrencias.size(); // Costo por turno

        // 2. Crear la lista de reservas individuales
        for (LocalDate fecha : ocurrencias) {
            LocalDateTime fechaHoraOcurrencia = LocalDateTime.of(fecha, hora);
            ReservaSimple individual = new ReservaSimple(0, fechaHoraOcurrencia, fija.getCancha(), fija.getCliente(), duracion, null);
            individual.setCostoTotal(costoConDescuento); 
            reservasAGuardar.add(individual);
        }

        // 3. Validar conflictos ANTES de intentar guardar
        List<LocalDateTime> conflictos = consultarConflictos(reservasAGuardar);
        if (!conflictos.isEmpty()) {
            System.err.println("Conflicto de disponibilidad detectado. Horarios: " + conflictos);
            return -1; // Indica conflicto
        }

        // 4. Guardar todas las reservas en una transacción
        Connection cn = null;
        int reservasGuardadas = 0;
        int idGrupoGenerado = -1; // ID de la primera reserva
        
        try {
            cn = ConexionDB.conectar();
            cn.setAutoCommit(false); // Iniciar transacción

            // 1. Guardar la PRIMERA reserva para obtener su ID
            ReservaSimple primeraReserva = reservasAGuardar.get(0);
            idGrupoGenerado = registrarReservaSimple(primeraReserva, cn, null); // ID de grupo nulo al principio
            
            if (idGrupoGenerado == -1) {
                throw new SQLException("Falló al insertar la primera reserva del grupo.");
            }
            reservasGuardadas++;
            
            // 2. Actualizar esa primera reserva para que su id_grupo_fija sea su propio ID
            String sqlUpdate = "UPDATE reserva SET id_grupo_fija = ? WHERE id_reserva = ?";
            try (PreparedStatement psUpdate = cn.prepareStatement(sqlUpdate)) {
                psUpdate.setInt(1, idGrupoGenerado);
                psUpdate.setInt(2, idGrupoGenerado);
                psUpdate.executeUpdate();
            }

            // 3. Guardar el RESTO de las reservas, usando el idGrupoGenerado
            for (int i = 1; i < reservasAGuardar.size(); i++) {
                ReservaSimple res = reservasAGuardar.get(i);
                int id = registrarReservaSimple(res, cn, idGrupoGenerado); // Pasa el ID del grupo
                if (id != -1) {
                    reservasGuardadas++;
                } else {
                    throw new SQLException("Falló al insertar una de las reservas fijas (ocurrencia " + (i+1) + ").");
                }
            }

            cn.commit(); // Todo salió bien, confirmar cambios
            return reservasGuardadas;

        } catch (SQLException e) {
            System.err.println("Error en transacción de reserva fija, haciendo rollback: " + e.getMessage());
            try {
                if (cn != null) cn.rollback(); // Deshacer cambios
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            return -1; // Indica error
        } finally {
            try {
                if (cn != null) {
                    cn.setAutoCommit(true); // Devolver la conexión al estado normal
                    cn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error al cerrar conexión post-transacción: " + e.getMessage());
            }
        }
    }


    /**
     * Cancela una reserva ÚNICA de la BD.
     *
     * @param idReserva El ID de la reserva a cancelar
     * @return true si tuvo éxito, false si no
     */
    public boolean cancelarReservaUnica(int idReserva) {
        String sql = "DELETE FROM reserva WHERE id_reserva = ?";
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idReserva);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            System.err.println("Error al cancelar reserva: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cancela una serie COMPLETA de reservas fijas (todas las que tengan el mismo id_grupo_fija).
     *
     * @param idReservaDeGrupo El ID de CUALQUIER reserva que pertenezca al grupo
     * @return El número de reservas eliminadas, o -1 si hay error.
     */
    public int cancelarReservaGrupo(int idReservaDeGrupo) {
        // 1. Encontrar el ID del grupo
        Integer idGrupo = null;
        String sqlFindGroup = "SELECT id_grupo_fija FROM reserva WHERE id_reserva = ?";
        
        try (Connection cn = ConexionDB.conectar()) {
             
            try (PreparedStatement psFind = cn.prepareStatement(sqlFindGroup)) {
                psFind.setInt(1, idReservaDeGrupo);
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        idGrupo = rs.getInt("id_grupo_fija");
                        if (rs.wasNull()) {
                            idGrupo = null;
                        }
                    }
                }
            }

            // Si no tiene grupo, es una reserva simple, solo borramos esa
            if (idGrupo == null) {
                return cancelarReservaUnica(idReservaDeGrupo) ? 1 : -1;
            }
            
            // 2. Borrar todas las reservas con ese ID de grupo
            String sqlDeleteGroup = "DELETE FROM reserva WHERE id_grupo_fija = ?";
            try (PreparedStatement psDelete = cn.prepareStatement(sqlDeleteGroup)) {
                psDelete.setInt(1, idGrupo);
                int filasAfectadas = psDelete.executeUpdate();
                return filasAfectadas; // Devuelve cuántas se borraron
            }
            
        } catch (SQLException e) {
            System.err.println("Error al cancelar grupo de reserva: " + e.getMessage());
            return -1;
        }
    }


    /**
     * Obtiene todas las reservas para una fecha específica.
     * Ahora también obtiene el id_grupo_fija.
     *
     * @param fecha La fecha a consultar
     * @return Una lista de objetos Reserva (simples)
     */
    public List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        List<Reserva> reservas = new ArrayList<>();
        // SQL MODIFICADO: Añade 'r.id_grupo_fija'
        String sql = "SELECT r.*, c.nombre as cancha_nombre, c.deporte, c.precio_por_hora, cl.nombre as cliente_nombre, cl.telefono "
                   + "FROM reserva r "
                   + "JOIN cancha c ON r.id_cancha = c.id_cancha "
                   + "JOIN cliente cl ON r.id_cliente = cl.id "
                   + "WHERE DATE(r.fecha_hora_inicio) = ? "
                   + "ORDER BY r.fecha_hora_inicio";

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            
            ps.setDate(1, java.sql.Date.valueOf(fecha));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cancha c = new Cancha(
                        rs.getInt("id_cancha"), 
                        rs.getString("cancha_nombre"), 
                        rs.getString("deporte"), 
                        rs.getDouble("precio_por_hora")
                    );
                    Cliente cl = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("cliente_nombre"),
                        rs.getString("telefono")
                    );

                    // Leer id_grupo_fija
                    Integer idGrupo = rs.getInt("id_grupo_fija");
                    if (rs.wasNull()) {
                        idGrupo = null;
                    }

                    // Crear ReservaSimple usando el constructor que acepta el ID de grupo
                    ReservaSimple r = new ReservaSimple(
                        rs.getInt("id_reserva"),
                        rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(),
                        c,
                        cl,
                        rs.getInt("duracion_minutos"),
                        idGrupo // <-- Pasa el ID de grupo
                    );
                    
                    reservas.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por fecha: " + e.getMessage());
        }
        return reservas;
    }

    /**
     * Consulta los horarios disponibles (en intervalos de 60 min) para una cancha y fecha.
     *
     * @param idCancha El ID de la cancha
     * @param fecha La fecha a consultar
     * @return Una lista de LocalTime con las horas de inicio libres
     */
    public List<LocalTime> consultarDisponibilidad(int idCancha, LocalDate fecha) {
        List<LocalTime> libres = new ArrayList<>();
        
        // 1. Obtener todas las reservas existentes para esa cancha y día
        String sqlReservas = "SELECT fecha_hora_inicio, duracion_minutos FROM reserva "
                           + "WHERE id_cancha = ? AND DATE(fecha_hora_inicio) = ?";
        
        List<Reserva> reservasDelDia = new ArrayList<>();
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sqlReservas)) {
             
            ps.setInt(1, idCancha);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Solo necesitamos la info de tiempo, creamos un objeto 'dummy'
                    reservasDelDia.add(new ReservaSimple(
                        0, 
                        rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(), 
                        null, null, // No necesitamos Cancha/Cliente aquí
                        rs.getInt("duracion_minutos")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando disponibilidad: " + e.getMessage());
            return libres; // Devuelve lista vacía si hay error
        }

        // 2. Iterar por los horarios del día (ej. 8am a 11pm)
        LocalTime t = LocalTime.of(8, 0);
        LocalTime finDia = LocalTime.of(23, 0);
        int duracionTurno = 60; // Asumimos turnos de 60 min

        while (t.isBefore(finDia)) {
            LocalDateTime inicioTurno = LocalDateTime.of(fecha, t);
            LocalDateTime finTurno = inicioTurno.plusMinutes(duracionTurno);
            
            boolean ocupado = false;
            
            // Crear una reserva "dummy" para el turno que queremos chequear
            ReservaSimple turnoAChequear = new ReservaSimple(0, inicioTurno, null, null, duracionTurno);
            
            // 3. Comprobar si solapa con alguna reserva existente
            for (Reserva r : reservasDelDia) {
                if (turnoAChequear.solapa(r)) { // Usamos la lógica de negocio de Reserva.java
                    ocupado = true;
                    break;
                }
            }
            
            if (!ocupado) {
                libres.add(t);
            }
            
            t = t.plusMinutes(duracionTurno); // Siguiente turno
        }
        
        return libres;
    }

    /**
     * Verifica si una lista de reservas propuestas entra en conflicto
     * con CUALQUIER reserva existente en la base de datos.
     * Usado para validar Reservas Fijas.
     * @param reservasPropuestas La lista de reservas a verificar
     * @return Una lista de LocalDateTime de los horarios en conflicto (vacía si no hay)
     */
    public List<LocalDateTime> consultarConflictos(List<ReservaSimple> reservasPropuestas) {
        List<LocalDateTime> conflictos = new ArrayList<>();
        if (reservasPropuestas == null || reservasPropuestas.isEmpty()) {
            return conflictos;
        }

        int idCancha = reservasPropuestas.get(0).getCancha().getIdCancha();
        LocalDate fechaInicio = reservasPropuestas.get(0).getFechaHoraInicio().toLocalDate();
        LocalDate fechaFin = reservasPropuestas.get(reservasPropuestas.size() - 1).getFechaHoraInicio().toLocalDate();

        // 2. Traer TODAS las reservas existentes en ese rango de fechas para esa cancha
        String sqlExistentes = "SELECT fecha_hora_inicio, duracion_minutos FROM reserva "
                             + "WHERE id_cancha = ? AND DATE(fecha_hora_inicio) BETWEEN ? AND ?";
        
        List<Reserva> reservasExistentes = new ArrayList<>();
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sqlExistentes)) {
            
            ps.setInt(1, idCancha);
            ps.setDate(2, java.sql.Date.valueOf(fechaInicio));
            ps.setDate(3, java.sql.Date.valueOf(fechaFin));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reservasExistentes.add(new ReservaSimple(
                        0, 
                        rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(), 
                        null, null, 
                        rs.getInt("duracion_minutos")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando conflictos: " + e.getMessage());
            // Si falla la comprobación, por seguridad, decimos que hay conflicto
            conflictos.add(LocalDateTime.now()); 
            return conflictos;
        }

        // 3. Comprobar cada reserva propuesta contra las existentes (lógica en memoria)
        for (ReservaSimple propuesta : reservasPropuestas) {
            for (Reserva existente : reservasExistentes) {
                if (propuesta.solapa(existente)) {
                    conflictos.add(propuesta.getFechaHoraInicio());
                    break; // No es necesario seguir comprobando esta propuesta
                }
            }
        }
        
        return conflictos;
    }
}

