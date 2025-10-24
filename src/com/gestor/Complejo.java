package com.gestor;


import java.io.Serializable;
import java.util.*;
import java.time.*;

public class Complejo implements Serializable {
    private int idComplejo;
    private String nombre;
    private List<Cancha> canchas = new ArrayList<>();
    private List<HorarioLaboral> horariosLaborales = new ArrayList<>();

    public Complejo() {}

    public Complejo(int idComplejo, String nombre) {
        this.idComplejo = idComplejo;
        this.nombre = nombre;
    }

    public int getIdComplejo() { return idComplejo; }
    public void setIdComplejo(int idComplejo) { this.idComplejo = idComplejo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public List<Cancha> getCanchas() { return canchas; }
    public List<HorarioLaboral> getHorariosLaborales() { return horariosLaborales; }

    public void agregarCancha(Cancha nuevaCancha) {
        canchas.add(nuevaCancha);
    }

    public void editarCancha(Cancha cancha) {
        for (int i = 0; i < canchas.size(); i++) {
            if (canchas.get(i).getIdCancha() == cancha.getIdCancha()) {
                canchas.set(i, cancha);
                return;
            }
        }
    }

    public void removerCancha(Cancha cancha) {
        canchas.removeIf(c -> c.getIdCancha() == cancha.getIdCancha());
    }

    public void agregarHorarioLaboral(HorarioLaboral horario) {
        horariosLaborales.add(horario);
    }

    public Optional<HorarioLaboral> horarioPara(LocalDate fecha) {
        DayOfWeek dw = fecha.getDayOfWeek();
        return horariosLaborales.stream().filter(h -> h.getDiaDeLaSemana()==dw).findFirst();
    }
}
