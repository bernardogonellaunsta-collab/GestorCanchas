package com.gestor.datos;

import com.gestor.negocio.HorarioLaboral;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.util.List;

public class HorarioDAO {

    // Obtiene el horario laboral (apertura, cierre, turno) para un día de la semana.
     
    public HorarioLaboral obtenerHorario(DayOfWeek dia) {
        
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
                        rs.getInt("duracion_turno_min")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener horario laboral: " + e.getMessage());
        }
        
        // Si no se encuentra un horario para ese día (o hay un error), devuelve null.
        // ReservaDAO está preparado para manejar esto (imprimirá "No hay horario laboral...").
        return null; 
    }

    public void actualizarHorario(HorarioLaboral horario) {
        // Esta función todavía no funciona.
    }

    //public List<HorarioLaboral> obtenerTodos() {};

    // HorarioLaboral obtenerHorarioPorDia(DayOfWeek dayOfWeek) {}
}

