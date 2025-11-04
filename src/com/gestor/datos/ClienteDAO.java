package com.gestor.datos;

import com.gestor.negocio.Cliente; // Importa el modelo de la capa de negocio
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    /**
     * Obtiene todos los clientes de la BD.
     * (Esta es la lógica que estaba en cargarClientesDesdeDB de MainFrame)
     */
    public List<Cliente> obtenerTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT id, nombre, telefono FROM cliente ORDER BY nombre";

        // Usa la conexión de ConexionDB
        try (Connection cn = ConexionDB.conectar();
             Statement st = cn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Cliente cli = new Cliente(
                    rs.getInt("id"),
                    rs.getString("nombre"),
                    rs.getString("telefono")
                );
                clientes.add(cli);
            }
        } catch (Exception e) {
            System.err.println("Error al cargar clientes: " + e.getMessage());
        }
        return clientes;
    }

    /**
     * Agrega un nuevo cliente a la BD.
     * (Esta es la lógica que estaba en onAgregarCliente de MainFrame)
     */
    public int agregarCliente(Cliente cliente) {
        String sql = "INSERT INTO cliente (nombre, telefono) VALUES (?, ?)";
        int idGenerado = -1;

        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombreCliente());
            ps.setString(2, cliente.getTelefono());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    idGenerado = rs.getInt(1);
                    cliente.setIdCliente(idGenerado); // Importante: actualiza el ID en el objeto
                }
            }
        } catch (Exception e) {
            System.err.println("Error al guardar cliente: " + e.getMessage());
        }
        return idGenerado;
    }
    
    /**
     * (NUEVO - IMPLEMENTADO)
     * Modifica un cliente existente en la base de datos.
     * @param cliente El objeto Cliente con los datos actualizados (incluyendo el ID)
     * @return true si la actualización fue exitosa, false si no
     */
    public boolean modificarCliente(Cliente cliente) {
        String sql = "UPDATE cliente SET nombre = ?, telefono = ? WHERE id = ?";
        
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setString(1, cliente.getNombreCliente());
            ps.setString(2, cliente.getTelefono());
            ps.setInt(3, cliente.getIdCliente());

            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Devuelve true si se actualizó al menos 1 fila

        } catch (Exception e) {
            System.err.println("Error al modificar el cliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * (NUEVO - IMPLEMENTADO)
     * Elimina un cliente de la base de datos.
     * Fallará si el cliente tiene reservas asociadas (por restricción de Foreign Key).
     * @param idCliente El ID del cliente a eliminar
     * @return true si la eliminación fue exitosa, false si no
     */
    public boolean eliminarCliente(int idCliente) {
        String sql = "DELETE FROM cliente WHERE id = ?";
        
        try (Connection cn = ConexionDB.conectar();
             PreparedStatement ps = cn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);
            int filasAfectadas = ps.executeUpdate();
            return filasAfectadas > 0; // Devuelve true si se eliminó al menos 1 fila

        } catch (Exception e) {
            // Esto es importante: si el cliente tiene reservas, la BD lanzará un error
            // de restricción (Foreign Key constraint violation), que será capturado aquí.
            System.err.println("Error al eliminar el cliente (puede tener reservas asociadas): " + e.getMessage());
            return false;
        }
    }
}