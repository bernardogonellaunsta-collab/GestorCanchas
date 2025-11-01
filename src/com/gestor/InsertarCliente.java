/*package com.gestor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertarCliente {
    public static void main(String[] args) {
        try (Connection cn = ConexionDB.conectar()) {

            String sql = "INSERT INTO cliente (nombre, telefono) VALUES (?, ?)";
            PreparedStatement ps = cn.prepareStatement(sql);

            int filas = ps.executeUpdate();
            if (filas > 0) {
                System.out.println("✅ Cliente insertado correctamente.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

*/