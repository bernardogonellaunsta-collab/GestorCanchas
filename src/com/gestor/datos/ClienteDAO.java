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
    
    // Aquí deberíamos crear los otros métodos:
    // public void modificarCliente(Cliente cliente) { ... }
    // public void eliminarCliente(int idCliente) { ... }
}