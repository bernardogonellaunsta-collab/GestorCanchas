package com.gestor.negocio;

import java.time.LocalDateTime;

public abstract class Reserva {

    protected int idReserva;
    protected LocalDateTime fechaHoraInicio;
    protected Cancha cancha;
    protected Cliente cliente;
    protected Integer idGrupoFija; 

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

    public boolean solapa(Reserva otra) {
        
        if (this.cancha != null && otra.cancha != null) {
            if (this.cancha.getIdCancha() != otra.cancha.getIdCancha()) {
                return false; // Son canchas diferentes, no pueden solapar.
            }
        }
        
        LocalDateTime thisInicio = this.getFechaHoraInicio();
        LocalDateTime thisFin = this.getFechaHoraFin();
        LocalDateTime otraInicio = otra.getFechaHoraInicio();
        LocalDateTime otraFin = otra.getFechaHoraFin();

        // Lógica de solapamiento (Overlapping intervals)
        // Solapan si (InicioA < FinB) y (InicioB < FinA)
        // Usamos !isBefore (>=) y !isAfter (<=) para manejar los bordes.
        
        // (this.inicio < otra.fin)
        boolean check1 = thisInicio.isBefore(otraFin);
        
        // (otra.inicio < this.fin)
        boolean check2 = otraInicio.isBefore(thisFin);

        return check1 && check2;
        // --- FIN DE CORRECCIÓN ---
    }

    // --- TODO EL CÓDIGO DE PERSISTENCIA (loadAll, saveAll, etc.) FUE ELIMINADO ---
    // --- Esa lógica ahora pertenece a ReservaDAO en la capa de datos. ---
}
