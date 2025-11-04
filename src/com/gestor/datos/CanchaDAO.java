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
        }
        return canchas;
    }

    /**
     * Agrega una nueva cancha a la base de datos.
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

    /**
     * Modifica una cancha existente en la base de datos.
     * @param cancha El objeto Cancha con los datos actualizados (incluyendo el ID)
     * @return true si la actualización fue exitosa, false si no
     */
    public boolean modificarCancha(Cancha cancha) {
        String sql = "UPDATE cancha SET nombre = ?, deporte = ?, precio_por_hora = ? WHERE id_cancha = ?";
        
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, cancha.getNombre());
            ps.setString(2, cancha.getDeporte());
            ps.setDouble(3, cancha.getPrecioPorHora());
            ps.setInt(4, cancha.getIdCancha());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Devuelve true si se actualizó al menos 1 fila

        } catch (SQLException e) {
            System.err.println("Error al modificar la cancha: " + e.getMessage());
            return false;
        }
    }

    /**
     * Elimina una cancha de la base de datos.
     * Fallará si la cancha tiene reservas asociadas (por restricción de Foreign Key).
     * @param idCancha El ID de la cancha a eliminar
     * @return true si la eliminación fue exitosa, false si no
     */
    public boolean eliminarCancha(int idCancha) {
        String sql = "DELETE FROM cancha WHERE id_cancha = ?";
        
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idCancha);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Devuelve true si se eliminó al menos 1 fila

        } catch (SQLException e) {
            // Esto es importante: si la cancha tiene reservas, la BD lanzará un error
            // de restricción (Foreign Key constraint violation), que será capturado aquí.
            System.err.println("Error al eliminar la cancha (puede tener reservas asociadas): " + e.getMessage());
            return false;
        }
    }
}
