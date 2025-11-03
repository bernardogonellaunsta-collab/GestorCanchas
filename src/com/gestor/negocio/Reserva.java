package com.gestor.negocio; // <-- Movido al paquete de negocio

import java.io.Serializable;
import java.time.LocalDateTime;

// Importa las otras clases de la capa de negocio
import com.gestor.negocio.Cancha;
import com.gestor.negocio.Cliente;

/**
 * Clase de entidad (Modelo) que representa una Reserva.
 * NO contiene lógica de persistencia (no sabe cómo guardarse en la BD).
 * Contiene la lógica de negocio pura (ej: calcular solapamiento).
 */
public abstract class Reserva implements Serializable {

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

    // Getters y Setters (sin cambios)
    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }
    public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    // Métodos de negocio (sin cambios)
    public abstract double calcularCostoTotal();
    public abstract int getDuracionMinutos();

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraInicio.plusMinutes(getDuracionMinutos());
    }

    /**
     * Lógica de negocio pura: determina si esta reserva se pisa con otra
     * en la misma cancha.
     */
    public boolean solapa(Reserva otra) {
        if (this.cancha == null || otra.cancha == null) return false;
        if (this.cancha.getIdCancha() != otra.cancha.getIdCancha()) return false;
        
        // Comprobación de solapamiento de rangos de tiempo
        return !(getFechaHoraFin().isEqual(otra.fechaHoraInicio) || getFechaHoraFin().isBefore(otra.fechaHoraInicio)
                 || otra.getFechaHoraFin().isEqual(this.fechaHoraInicio) || otra.getFechaHoraFin().isBefore(this.fechaHoraInicio));
    }

    // --- TODO EL CÓDIGO DE PERSISTENCIA (loadAll, saveAll, etc.) FUE ELIMINADO ---
    // --- Esa lógica ahora pertenece a ReservaDAO en la capa de datos. ---
}
