package com.gestor;


import java.time.*;

public class ReservaSimple extends Reserva {

    private int duracionMinutos;

    public ReservaSimple() {}

    public ReservaSimple(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente, int duracionMinutos) {
        super(idReserva, fechaHoraInicio, cancha, cliente);
        this.duracionMinutos = duracionMinutos;
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
