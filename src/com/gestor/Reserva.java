package com.gestor;


import java.io.*;
import java.time.*;
import java.util.*;

/**
 * Centraliza la lógica de negocio mínima y la persistencia simple en archivo.
 * Para mantener el modelo acotado, no se emplean DAOs ni excepciones personalizadas.
 */
public abstract class Reserva implements Serializable {

    private static final String DATA_DIR = "data";
    private static final String FILE_RESERVAS = DATA_DIR + File.separator + "reservas.dat";

    protected int idReserva;
    protected LocalDateTime fechaHoraInicio;
    protected Cancha cancha;
    protected Cliente cliente;

    public Reserva() {}

    public Reserva(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente) {
        this.idReserva = idReserva;
        this.fechaHoraInicio = fechaHoraInicio;
        this.cancha = cancha;
        this.cliente = cliente;
    }

    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }
    public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public abstract double calcularCostoTotal();
    public abstract int getDuracionMinutos();

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraInicio.plusMinutes(getDuracionMinutos());
    }

    public boolean solapa(Reserva otra) {
        if (this.cancha == null || otra.cancha == null) return false;
        if (this.cancha.getIdCancha() != otra.cancha.getIdCancha()) return false;
        return !(getFechaHoraFin().isEqual(otra.fechaHoraInicio) || getFechaHoraFin().isBefore(otra.fechaHoraInicio)
                 || otra.getFechaHoraFin().isEqual(this.fechaHoraInicio) || otra.getFechaHoraFin().isBefore(this.fechaHoraInicio));
    }

    /* ---------------- Persistencia y "servicios" estáticos ---------------- */

    private static void ensureDataDir() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    @SuppressWarnings("unchecked")
    private static List<Reserva> loadAll() {
        ensureDataDir();
        File f = new File(FILE_RESERVAS);
        if (!f.exists()) return new ArrayList<>();
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                return (List<Reserva>) obj;
            }
        } catch (Exception e) {
            // Si hay error, devolvemos lista vacía sin propagar.
        }
        return new ArrayList<>();
    }

    private static void saveAll(List<Reserva> reservas) {
        ensureDataDir();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_RESERVAS))) {
            oos.writeObject(reservas);
        } catch (IOException e) {
            // Silencioso por simplicidad. En un sistema real se informaría.
        }
    }

    public static void registrarReserva(Reserva nuevaReserva) {
        List<Reserva> reservas = loadAll();
        // Validar no solapamiento
        for (Reserva r : reservas) {
            if (r.solapa(nuevaReserva)) {
                return; // No registrar si solapa (simple y sin excepciones)
            }
        }
        reservas.add(nuevaReserva);
        saveAll(reservas);
    }

    public static void cancelarReserva(int idReserva) {
        List<Reserva> reservas = loadAll();
        reservas.removeIf(r -> r.getIdReserva() == idReserva);
        saveAll(reservas);
    }

    public static List<LocalTime> consultarDisponibilidad(int idCancha, LocalDate fecha) {
        // Ventana por defecto: 08:00 a 23:00, intervalos de 60 min
        List<Reserva> reservas = obtenerReservasPorFecha(fecha);
        List<LocalTime> libres = new ArrayList<>();
        LocalTime t = LocalTime.of(8, 0);
        LocalTime fin = LocalTime.of(23, 0);
        while (!t.isAfter(fin.minusMinutes(60))) {
            boolean ocupado = false;
            LocalDateTime ini = LocalDateTime.of(fecha, t);
            LocalDateTime finSlot = ini.plusMinutes(60);
            for (Reserva r : reservas) {
                if (r.getCancha()!=null && r.getCancha().getIdCancha()==idCancha) {
                    boolean overlap = !(r.getFechaHoraFin().isEqual(ini) || r.getFechaHoraFin().isBefore(ini)
                                        || finSlot.isEqual(r.getFechaHoraInicio()) || finSlot.isBefore(r.getFechaHoraInicio()));
                    if (overlap) { ocupado = true; break; }
                }
            }
            if (!ocupado) libres.add(t);
            t = t.plusMinutes(60);
        }
        return libres;
    }

    public static List<Reserva> obtenerReservasPorFecha(LocalDate fecha) {
        List<Reserva> reservas = loadAll();
        List<Reserva> result = new ArrayList<>();
        for (Reserva r : reservas) {
            if (r.getFechaHoraInicio().toLocalDate().equals(fecha)) {
                result.add(r);
            }
        }
        return result;
    }
}
