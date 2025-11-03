package com.gestor.datos;

import com.gestor.negocio.HorarioLaboral;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;

/**
 * (CORREGIDO)
 * Implementa la lógica de `obtenerHorario` que faltaba.
 * El error "UnsupportedOperationException" ha sido reemplazado
 * por la lógica SQL correcta.
 */
public class HorarioDAO {

    /**
     * (IMPLEMENTADO)
     * Obtiene el horario laboral (apertura, cierre, turno) para un día de la semana.
     */
    public HorarioLaboral obtenerHorario(DayOfWeek dia) {
        
        // --- INICIO DE LA IMPLEMENTACIÓN ---
        String sql = "SELECT * FROM horario_laboral WHERE dia_semana = ?";
        
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            // dia.name() devuelve "MONDAY", "TUESDAY", etc.
            ps.setString(1, dia.name()); 
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Construye el objeto de negocio HorarioLaboral
                    return new HorarioLaboral(
                        dia,
                        rs.getTime("hora_apertura").toLocalTime(),
                        rs.getTime("hora_cierre").toLocalTime(),
                        rs.getInt("duracion_turnos_minutos")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener horario laboral: " + e.getMessage());
        }
        
        // Si no se encuentra un horario para ese día (o hay un error), devuelve null.
        // ReservaDAO está preparado para manejar esto (imprimirá "No hay horario laboral...").
        return null; 
        // --- FIN DE LA IMPLEMENTACIÓN ---
        
        // La siguiente línea (que estaba en la línea 57) era la causa del error y ha sido eliminada:
        // throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * (NO IMPLEMENTADO - Placeholder)
     * Actualiza un horario en la BD.
     */
    public void actualizarHorario(HorarioLaboral horario) {
        // Esta función no es llamada por la aplicación principal todavía.
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    /**
     * (NO IMPLEMENTADO - Placeholder)
     * Obtiene todos los horarios de la semana.
     */
    public List<HorarioLaboral> obtenerTodos() {
        // Esta función no es llamada por la aplicación principal todavía.
        throw new UnsupportedOperationException("Not supported yet.");
    }

    HorarioLaboral obtenerHorarioPorDia(DayOfWeek dayOfWeek) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}

