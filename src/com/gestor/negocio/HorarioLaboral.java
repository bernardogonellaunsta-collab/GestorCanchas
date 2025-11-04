package com.gestor.negocio;

import java.time.*;

public class HorarioLaboral {
    private DayOfWeek diaDeLaSemana;
    private LocalTime horaApertura;
    private LocalTime horaCierre;
    private int duracionTurnoMinutos;

    public HorarioLaboral() {}

    public HorarioLaboral(DayOfWeek diaDeLaSemana, LocalTime horaApertura, LocalTime horaCierre, int duracionTurnoMinutos) {
        this.diaDeLaSemana = diaDeLaSemana;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
        this.duracionTurnoMinutos = duracionTurnoMinutos;
    }

    public DayOfWeek getDiaDeLaSemana() { return diaDeLaSemana; }
    public void setDiaDeLaSemana(DayOfWeek diaDeLaSemana) { this.diaDeLaSemana = diaDeLaSemana; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public int getDuracionTurnoMinutos() { return duracionTurnoMinutos; }
    public void setDuracionTurnoMinutos(int duracionTurnoMinutos) { this.duracionTurnoMinutos = duracionTurnoMinutos; }

    @Override
    public String toString() {
        return diaDeLaSemana + " " + horaApertura + "-" + horaCierre + " cada " + duracionTurnoMinutos + " min";
    }
}
