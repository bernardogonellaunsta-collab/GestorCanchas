package com.gestor.negocio;

import java.time.LocalDateTime;

/**
 * (MODIFICADO)
 * Constructor actualizado.
 */
public class ReservaSimple extends Reserva {

    private int duracionMinutos;

    public ReservaSimple() {
        super();
    }

    /**
     * Constructor MODIFICADO para incluir el idGrupoFija (opcional).
     */
    public ReservaSimple(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente, int duracionMinutos, Integer idGrupoFija) {
        super(idReserva, fechaHoraInicio, cancha, cliente);
        this.duracionMinutos = duracionMinutos;
        this.idGrupoFija = idGrupoFija; // Asigna el ID de grupo
    }
    
    // Constructor anterior (lo mantenemos por compatibilidad si es necesario)
    public ReservaSimple(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente, int duracionMinutos) {
        this(idReserva, fechaHoraInicio, cancha, cliente, duracionMinutos, null);
    }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    @Override
    public double calcularCostoTotal() {
        if (cancha == null) return 0.0;
        double horas = duracionMinutos / 60.0;
        return cancha.getPrecioPorHora() * horas;
    }
}
