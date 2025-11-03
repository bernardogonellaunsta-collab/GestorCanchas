package com.gestor.negocio;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * (MODIFICADO)
 * Clase abstracta que define una reserva.
 * Ahora incluye 'idGrupoFija' para saber si pertenece a una serie.
 */
public abstract class Reserva implements Serializable {

    protected int idReserva;
    protected LocalDateTime fechaHoraInicio;
    protected Cancha cancha;
    protected Cliente cliente;
    protected Integer idGrupoFija; // --- CAMBIO: Añadido este campo ---

    public Reserva() {}

    public Reserva(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente) {
        this.idReserva = idReserva;
        this.fechaHoraInicio = fechaHoraInicio;
        this.cancha = cancha;
        this.cliente = cliente;
        this.idGrupoFija = null; // Por defecto no pertenece a un grupo
    }

    // --- INICIO DE GETTERS/SETTERS (Algunos nuevos) ---
    public int getIdReserva() { return idReserva; }
    public void setIdReserva(int idReserva) { this.idReserva = idReserva; }
    public LocalDateTime getFechaHoraInicio() { return fechaHoraInicio; }
    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) { this.fechaHoraInicio = fechaHoraInicio; }
    public Cancha getCancha() { return cancha; }
    public void setCancha(Cancha cancha) { this.cancha = cancha; }
    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    /**
     * Devuelve el ID del grupo de reserva fija, o null si es una reserva simple.
     */
    public Integer getIdGrupoFija() { return idGrupoFija; }
    public void setIdGrupoFija(Integer idGrupoFija) { this.idGrupoFija = idGrupoFija; }
    
    /**
     * Devuelve true si la reserva es parte de una serie fija.
     */
    public boolean esParteDeGrupo() { return this.idGrupoFija != null; }
    
    public void setCostoTotal(double costoTotal) {
        // Usado por el DAO para setear el costo con descuento
    }
    // --- FIN DE GETTERS/SETTERS ---

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
