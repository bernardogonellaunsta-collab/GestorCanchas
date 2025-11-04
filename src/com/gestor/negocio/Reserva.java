package com.gestor.negocio;

import java.time.LocalDateTime;

public abstract class Reserva {

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
     * (CORREGIDO OTRA VEZ)
     * Lógica de negocio pura: determina si esta reserva se pisa con otra.
     * La lógica de "no solapamiento" es:
     * (this.fin <= otra.inicio) O (otra.fin <= this.inicio)
     * Por lo tanto, el solapamiento es la negación de eso.
     *
     * (CORRECCIÓN FINAL)
     * La lógica anterior `otra.getFechaHoraFin().isEqual(this.fechaHoraInicio)` [20:00 == 20:00]
     * causaba que dos reservas "tocándose" (19-20 y 20-21) no se solapen.
     * La nueva lógica `!A.isBefore(B)` es lo mismo que `A >= B`.
     * Un solapamiento real ocurre si:
     * this.inicio < otra.fin Y this.fin > otra.inicio
     */
    public boolean solapa(Reserva otra) {
        
        // --- INICIO DE CORRECCIÓN ---
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

