package com.gestor.presentacion;

import com.gestor.datos.SetUpDB;
import java.time.*;
import java.util.*;

public class GestorDeportivoApp {

    public static void main(String[] args) {
        
        SetUpDB.verificarYCrearBD();
        
        // --- Abrir GUI ---
        /*
        * Se usa invokeLater como una buena práctica para evitar posibles errores
        * Evita que la interfaz se congele y que los botones fallen
        */
        
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
