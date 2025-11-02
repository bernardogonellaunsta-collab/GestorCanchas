package com.gestor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clase de utilidad para inicializar la base de datos.
 * Se encarga de conectarse al servidor MySQL y ejecutar el script
 * 'schema.sql' para crear la base de datos y las tablas si no existen.
 */
public class SetUpDB {

    // URL de conexión al SERVIDOR (nota que NO especificamos la base de datos)
    private static final String URL_SERVER = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";     // Usuario root de MySQL
    private static final String PASSWORD = "";      // Contraseña de root de MySQL
    private static final String SCRIPT_PATH = "schema.sql"; // Ruta al script en la raíz del proyecto

    /**
     * Método público que inicia la verificación y creación de la BD.
     */
    public static void verificarYCrearBD() {
        try (Connection cn = DriverManager.getConnection(URL_SERVER, USER, PASSWORD);
             Statement st = cn.createStatement()) {

            System.out.println("Conectado al servidor MySQL. Verificando base de datos...");
            String sqlScript = new String(Files.readAllBytes(Paths.get(SCRIPT_PATH)));

            // Eliminar comentarios SQL (opcional, pero más limpio)
            //sqlScript = sqlScript.replaceAll("--.*\\n", "");

            // Separar los comandos por punto y coma
            String[] commands = sqlScript.split(";");

            for (String command : commands) {
                String trimmedCommand = command.trim();
                if (!trimmedCommand.isEmpty()) {
                    // System.out.println("Ejecutando: " + trimmedCommand.substring(0, Math.min(trimmedCommand.length(), 60)) + "...");
                    st.execute(trimmedCommand);
                }
            }
            System.out.println("✅ Base de datos y tablas verificadas/creadas.");

        } catch (SQLException e) {
            System.err.println("❌ Error de SQL durante el setup: " + e.getMessage());
            // Opcional: mostrar un popup de error
            // javax.swing.JOptionPane.showMessageDialog(null, "Error al configurar la BD: " + e.getMessage(), "Error de Base de Datos", javax.swing.JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            System.err.println("❌ Error: No se pudo leer el archivo 'schema.sql'. " + e.getMessage());
            System.err.println("Asegúrate de que 'schema.sql' esté en la raíz del proyecto.");
            // javax.swing.JOptionPane.showMessageDialog(null, "Error: No se encontró 'schema.sql'", "Error de Configuración", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }
}