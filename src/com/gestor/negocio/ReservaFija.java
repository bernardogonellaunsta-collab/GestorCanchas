package com.gestor.negocio;


import com.gestor.negocio.Cancha;
import com.gestor.negocio.Cliente;
import java.time.*;
import java.util.*;

public class ReservaFija extends Reserva {

    private DayOfWeek diaDeLaSemana;
    private LocalDate fechaFin;
    private double descuentoAplicado; // 0..1
    private int duracionMinutos = 60; // por simplicidad

    public ReservaFija() {}

    public ReservaFija(int idReserva, LocalDateTime fechaHoraInicio, Cancha cancha, Cliente cliente,
                       DayOfWeek diaDeLaSemana, LocalDate fechaFin, double descuentoAplicado) {
        super(idReserva, fechaHoraInicio, cancha, cliente);
        this.diaDeLaSemana = diaDeLaSemana;
        this.fechaFin = fechaFin;
        this.descuentoAplicado = descuentoAplicado;
    }

    public DayOfWeek getDiaDeLaSemana() { return diaDeLaSemana; }
    public void setDiaDeLaSemana(DayOfWeek diaDeLaSemana) { this.diaDeLaSemana = diaDeLaSemana; }
    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }
    public double getDescuentoAplicado() { return descuentoAplicado; }
    public void setDescuentoAplicado(double descuentoAplicado) { this.descuentoAplicado = descuentoAplicado; }

    @Override
    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int m) { this.duracionMinutos = m; }

    public List<LocalDate> generarOcurrencias(LocalDate desde, LocalDate hasta) {
        List<LocalDate> fechas = new ArrayList<>();
        LocalDate f = desde;
        while (!f.isAfter(hasta)) {
            if (f.getDayOfWeek() == diaDeLaSemana) {
                fechas.add(f);
            }
            f = f.plusDays(1);
        }
        return fechas;
    }

    @Override
    public double calcularCostoTotal() {
        if (cancha == null) return 0.0;
        long semanas = 0;
        if (fechaFin != null) {
            semanas = java.time.temporal.ChronoUnit.WEEKS.between(fechaHoraInicio.toLocalDate(), fechaFin) + 1;
            if (semanas < 1) semanas = 1;
        } else {
            semanas = 1;
        }
        double horas = duracionMinutos / 60.0;
        double total = cancha.getPrecioPorHora() * horas ;
        return total * (1.0 - descuentoAplicado);
    }
}
