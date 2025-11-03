package com.gestor.datos;

import com.gestor.negocio.Cancha; // Importa el modelo de la capa de negocio
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la entidad Cancha.
 * Encapsula todo el acceso a la base de datos (SQL) para la tabla 'cancha'.
 */
public class CanchaDAO {

    /**
     * Obtiene todas las canchas de la base de datos.
     * (Lógica extraída de cargarCanchasDesdeDB en MainFrame)
     * @return Una lista de objetos Cancha.
     */
    public List<Cancha> obtenerTodas() {
        List<Cancha> canchas = new ArrayList<>();
        String sql = "SELECT id_cancha, nombre, deporte, precio_por_hora FROM cancha ORDER BY nombre";

        try (Connection cn = ConexionDB.conectar();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Cancha c = new Cancha(
                    rs.getInt("id_cancha"),
                    rs.getString("nombre"),
                    rs.getString("deporte"),
                    rs.getDouble("precio_por_hora")
                );
                canchas.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Error al cargar canchas: " + e.getMessage());
            // En una app real, podríamos lanzar una excepción personalizada
        }
        return canchas;
    }

    /**
     * Agrega una nueva cancha a la base de datos.
     * (Lógica extraída de onAgregarCancha en MainFrame)
     * @param cancha El objeto Cancha a guardar (el ID se ignora y se usa el autogenerado)
     * @return El ID autogenerado por la base de datos, o -1 si falló.
     */
    public int agregarCancha(Cancha cancha) {
        String sql = "INSERT INTO cancha (nombre, deporte, precio_por_hora) VALUES (?, ?, ?)";
        int idGenerado = -1;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cancha.getNombre());
            ps.setString(2, cancha.getDeporte());
            ps.setDouble(3, cancha.getPrecioPorHora());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idGenerado = rs.getInt(1);
                    cancha.setIdCancha(idGenerado); // Actualiza el ID en el objeto original
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al guardar la cancha: " + e.getMessage());
        }
        return idGenerado;
    }

    // TODO: Implementar métodos para modificar y eliminar
    /*
    public boolean modificarCancha(Cancha cancha) {
        // ... lógica UPDATE cancha SET ... WHERE id_cancha = ?
        return false;
    }

    public boolean eliminarCancha(int idCancha) {
        // ... lógica DELETE FROM cancha WHERE id_cancha = ?
        return false;
    }
    */
}
