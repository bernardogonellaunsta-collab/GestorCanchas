package com.gestor;

import java.time.*;
import java.util.*;

public class GestorDeportivoApp {

    public static void main(String[] args) {
        // --- Lógica de ejemplo existente ---
        Cancha c1 = new Cancha(1, "Cancha 1", "Fútbol 5", 12000.0);
        Cancha c2 = new Cancha(2, "Cancha 2", "Pádel", 10000.0);
        Cliente cli = new Cliente(1, "Juan Pérez", "381-555-000");

        LocalDate hoy = LocalDate.now();
        ReservaSimple r1 = new ReservaSimple(1, LocalDateTime.of(hoy, LocalTime.of(19,0)), c1, cli, 60);
        Reserva.registrarReserva(r1);

        List<java.time.LocalTime> libres = Reserva.consultarDisponibilidad(1, hoy);
        System.out.println("Disponibilidad de Cancha 1 para " + hoy + ": " + libres);

        List<Reserva> hoyR = Reserva.obtenerReservasPorFecha(hoy);
        System.out.println("Reservas de hoy: " + hoyR.size());

        // --- Abrir GUI ---
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new MainFrame().setVisible(true);
        });
    }
}
