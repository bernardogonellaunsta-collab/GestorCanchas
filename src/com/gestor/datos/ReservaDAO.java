package com.gestor.datos;

import com.gestor.negocio.Cliente;
import com.gestor.negocio.Cancha;
import com.gestor.negocio.HorarioLaboral; // Asegúrate que este import esté
import com.gestor.negocio.Reserva;
import com.gestor.negocio.ReservaFija;
import com.gestor.negocio.ReservaSimple;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase DAO para manejar toda la lógica de persistencia de Reservas en MySQL.
 * Incluye lógica de transacciones y manejo de grupos para Reservas Fijas.
 */
public class ReservaDAO {

    // (NUEVO) Referencia al DAO de Horarios
    private final HorarioDAO horarioDAO;

    public ReservaDAO() {
        this.horarioDAO = new HorarioDAO();
    }
    
    /**
     * Método principal para registrar una reserva.
     * Ahora valida conflictos para AMBOS tipos de reserva.
     * Valida contra el horario laboral.
     *
     * @param reserva El objeto de reserva (Simple o Fija)
     * @return El ID (si es Simple) o la cantidad de reservas (si es Fija). -1 si hay error.
     */
    public int registrarReserva(Reserva reserva) {
        
        // --- VALIDACIÓN DE HORARIO LABORAL ---
        LocalDate fechaReserva = reserva.getFechaHoraInicio().toLocalDate();
        DayOfWeek dia = fechaReserva.getDayOfWeek();
        HorarioLaboral horario = horarioDAO.obtenerHorario(dia);

        if (horario == null) {
            System.err.println("Error: No hay horario laboral definido para " + dia + ". No se puede registrar la reserva.");
            return -1; // Indica error (cerrado ese día)
        }

        LocalDateTime inicioReservaDT = reserva.getFechaHoraInicio();
        LocalDateTime finReservaDT = reserva.getFechaHoraFin();
        
        LocalDateTime aperturaDT = LocalDateTime.of(fechaReserva, horario.getHoraApertura());
        LocalDateTime cierreDT = LocalDateTime.of(fechaReserva, horario.getHoraCierre());

        // Check 1: La hora de INICIO no puede ser antes de la apertura
        if (inicioReservaDT.isBefore(aperturaDT)) {
            System.err.println("Error: La reserva (" + inicioReservaDT.toLocalTime() + ") no puede ser antes de la hora de apertura (" + horario.getHoraApertura() + ").");
            return -1;
        }

        // Check 2: La hora de FIN no puede ser después del cierre
        // (isAfter compara el LocalDateTime completo)
        if (finReservaDT.isAfter(cierreDT)) {
            System.err.println("Error: El fin de la reserva (" + finReservaDT.toLocalTime() + " del " + finReservaDT.toLocalDate() + ") no puede ser después de la hora de cierre (" + horario.getHoraCierre() + " del " + fechaReserva + ").");
            return -1;
        }


        if (reserva instanceof ReservaSimple) {
            
            // 1. Convertir a lista para usar el validador de conflictos
            List<ReservaSimple> aChequear = new ArrayList<>();
            aChequear.add((ReservaSimple) reserva);
            
            // 2. Validar conflictos ANTES de guardar
            List<LocalDateTime> conflictos = consultarConflictos(aChequear);
            if (!conflictos.isEmpty()) {
                System.err.println("Conflicto de disponibilidad detectado para reserva simple en: " + conflictos.get(0));
                return -1; // Indica conflicto
            }
            
            // 3. Si no hay conflictos, registrar
            return registrarReservaSimple((ReservaSimple) reserva, null, null); // Sin transacción, sin grupo
            
        } else if (reserva instanceof ReservaFija) {
            // ReservaFija ya tiene su propia validación de conflictos interna
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
            // ps.setTimestamp(3, Timestamp.valueOf(reserva.getFechaHoraInicio())); // (CORREGIDO) Causa bug de TimeZone
            ps.setObject(3, reserva.getFechaHoraInicio()); // (SOLUCIÓN) Usa el tipo de Java 8+
            ps.setInt(4, reserva.getDuracionMinutos());
            ps.setString(5, "Simple"); // Todas las reservas en la BD son 'simples'
            
            // (CORREGIDO) Asegurarnos de calcular el costo simple
            double costo = reserva.calcularCostoTotal();
            ps.setDouble(6, costo); 
            reserva.setCostoTotal(costo); // Actualiza el costo en el objeto

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
        // (CORREGIDO) El costo total de la ReservaFija es el costo *por turno*
        double costoConDescuento = fija.calcularCostoTotal();

        // 2. Crear la lista de reservas individuales
        for (LocalDate fecha : ocurrencias) {
            LocalDateTime fechaHoraOcurrencia = LocalDateTime.of(fecha, hora);
            ReservaSimple individual = new ReservaSimple(0, fechaHoraOcurrencia, fija.getCancha(), fija.getCliente(), duracion, null);
            individual.setCostoTotal(costoConDescuento); 
            reservasAGuardar.add(individual);
        }

        // --- VALIDACIÓN DE HORARIO LABORAL (PARA RESERVAS FIJAS) ---
        // Se debe chequear cada ocurrencia generada
        for (ReservaSimple res : reservasAGuardar) {
            LocalDate fechaRes = res.getFechaHoraInicio().toLocalDate();
            DayOfWeek diaRes = fechaRes.getDayOfWeek();
            HorarioLaboral horarioRes = horarioDAO.obtenerHorario(diaRes);

            if (horarioRes == null) {
                System.err.println("Error en reserva fija: No hay horario laboral definido para " + diaRes);
                return -1;
            }
            
            LocalDateTime inicioReservaDT = res.getFechaHoraInicio();
            LocalDateTime finReservaDT = res.getFechaHoraFin();

            LocalDateTime aperturaDT = LocalDateTime.of(fechaRes, horarioRes.getHoraApertura());
            LocalDateTime cierreDT = LocalDateTime.of(fechaRes, horarioRes.getHoraCierre());

            if (inicioReservaDT.isBefore(aperturaDT)) {
                System.err.println("Error en reserva fija: Una ocurrencia (" + inicioReservaDT.toLocalTime() + ") es antes de la apertura (" + aperturaDT.toLocalTime() + ").");
                return -1;
            }
            if (finReservaDT.isAfter(cierreDT)) {
                System.err.println("Error en reserva fija: Una ocurrencia (" + finReservaDT.toLocalTime() + " del " + finReservaDT.toLocalDate() + ") termina después del cierre (" + cierreDT.toLocalTime() + " del " + fechaRes + ").");
                return -1;
            }
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
            
            // 2. Borrar todas las reservas con ese ID de grupo (INCLUYENDO LA PRIMERA)
            // (CORREGIDO) El ID de grupo es el ID de la primera reserva.
            String sqlDeleteGroup = "DELETE FROM reserva WHERE id_grupo_fija = ? OR id_reserva = ?";
            try (PreparedStatement psDelete = cn.prepareStatement(sqlDeleteGroup)) {
                psDelete.setInt(1, idGrupo);
                psDelete.setInt(2, idGrupo); // Borra también la reserva "líder" del grupo
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
        // SQL MODIFICADO: Añade 'r.id_grupo_fija' y 'r.costo_total'
        String sql = "SELECT r.*, c.nombre as cancha_nombre, c.deporte, c.precio_por_hora, cl.nombre as cliente_nombre, cl.telefono "
                   + "FROM reserva r "
                   + "JOIN cancha c ON r.id_cancha = c.id_cancha "
                   + "JOIN cliente cl ON r.id_cliente = cl.id "
                   + "WHERE DATE(r.fecha_hora_inicio) >= ? "
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
                        // rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(), // (CORREGIDO) Causa bug de TimeZone
                        rs.getObject("fecha_hora_inicio", LocalDateTime.class), // (SOLUCIÓN) Usa el tipo de Java 8+
                        c,
                        cl,
                        rs.getInt("duracion_minutos"),
                        idGrupo // <-- Pasa el ID de grupo
                    );
                    
                    // (NUEVO) Asigna el costo real guardado en la BD
                    r.setCostoTotal(rs.getDouble("costo_total"));
                    
                    reservas.add(r);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por fecha: " + e.getMessage());
        }
        return reservas;
    }

    /**
     * Consulta los horarios disponibles usando la tabla `horario_laboral`
     *
     * @param idCancha El ID de la cancha
     * @param fecha La fecha a consultar
     * @return Una lista de LocalTime con las horas de inicio libres
     */
    public List<LocalTime> consultarDisponibilidad(int idCancha, LocalDate fecha) {
        List<LocalTime> libres = new ArrayList<>();
        
        // 1. Obtener el horario laboral para ESE día
        HorarioLaboral horario = horarioDAO.obtenerHorario(fecha.getDayOfWeek());
        if (horario == null) {
            System.err.println("No hay horario laboral definido para " + fecha.getDayOfWeek());
            return libres; // Devuelve lista vacía
        }

        // 2. Obtener todas las reservas existentes para esa cancha y día
        String sqlReservas = "SELECT fecha_hora_inicio, duracion_minutos FROM reserva "
                           + "WHERE id_cancha = ? AND DATE(fecha_hora_inicio) = ?";
        
        List<Reserva> reservasDelDia = new ArrayList<>();
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sqlReservas)) {
             
            ps.setInt(1, idCancha);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // (MODIFICADO) Creamos una cancha 'dummy' pero con el ID correcto
                    // para que 'solapa' funcione en la capa de negocio.
                    Cancha canchaDummy = new Cancha();
                    canchaDummy.setIdCancha(idCancha);
                    
                    reservasDelDia.add(new ReservaSimple(
                        0, 
                        // rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(), // (CORREGIDO) Causa bug de TimeZone
                        rs.getObject("fecha_hora_inicio", LocalDateTime.class), // (SOLUCIÓN) Usa el tipo de Java 8+
                        canchaDummy, null, // (MODIFICADO) Pasamos la cancha dummy
                        rs.getInt("duracion_minutos")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error consultando disponibilidad: " + e.getMessage());
            return libres; // Devuelve lista vacía si hay error
        }

        // 3. Iterar por los horarios del día (SEGÚN LA BD)
        LocalTime t = horario.getHoraApertura();
        LocalTime finDia = horario.getHoraCierre();
        int duracionTurno = horario.getDuracionTurnoMinutos();

        while (t.isBefore(finDia)) {
            LocalDateTime inicioTurno = LocalDateTime.of(fecha, t);
            
            boolean ocupado = false;
            
            // (MODIFICADO) Crear una reserva "dummy" con el ID de cancha correcto
            Cancha canchaDummy = new Cancha();
            canchaDummy.setIdCancha(idCancha);
            ReservaSimple turnoAChequear = new ReservaSimple(0, inicioTurno, canchaDummy, null, duracionTurno);
            
            // 4. Comprobar si solapa con alguna reserva existente
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
     * Usado para validar Reservas Fijas (y ahora Simples).
     * @param reservasPropuestas La lista de reservas a verificar
     * @return Una lista de LocalDateTime de los horarios en conflicto (vacía si no hay)
     */
    public List<LocalDateTime> consultarConflictos(List<ReservaSimple> reservasPropuestas) {
        List<LocalDateTime> conflictos = new ArrayList<>();
        if (reservasPropuestas == null || reservasPropuestas.isEmpty()) {
            return conflictos;
        }

        // (MODIFICADO) Obtenemos la cancha del objeto (ya no es null)
        Cancha canchaPropuesta = reservasPropuestas.get(0).getCancha();
        if (canchaPropuesta == null) {
            System.err.println("Error de validación: La reserva propuesta no tiene cancha.");
            return conflictos; // No se puede validar
        }
        int idCancha = canchaPropuesta.getIdCancha();
        
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
                    // (MODIFICADO) Creamos una cancha 'dummy' con el ID correcto
                    Cancha canchaDummy = new Cancha();
                    canchaDummy.setIdCancha(idCancha);
                    
                    reservasExistentes.add(new ReservaSimple(
                        0, 
                        // rs.getTimestamp("fecha_hora_inicio").toLocalDateTime(), // (CORREGIDO) Causa bug de TimeZone
                        rs.getObject("fecha_hora_inicio", LocalDateTime.class), // (SOLUCIÓN) Usa el tipo de Java 8+
                        canchaDummy, null, // Pasamos la cancha dummy
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
                // Ahora 'solapa' comparará IDs de cancha (propuesta.cancha vs existente.cancha)
                if (propuesta.solapa(existente)) {
                    conflictos.add(propuesta.getFechaHoraInicio());
                    break; // No es necesario seguir comprobando esta propuesta
                }
            }
        }
        
        return conflictos;
    }
}


