import java.sql.*;

public class TestConexion {
    public static void main(String[] args) {
        try (Connection cn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/gestor_deportivo?useSSL=false&serverTimezone=UTC",
                "root", "")) {
            System.out.println("✅ Conexión exitosa");
        } catch (SQLException e) {
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
}

