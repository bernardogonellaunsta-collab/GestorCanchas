package com.gestor.negocio;
    
public class Cancha {
    private int idCancha;
    private String nombre;
    private String deporte;
    private double precioPorHora;

    public Cancha() {}

    public Cancha(int idCancha, String nombre, String deporte, double precioPorHora) {
        this.idCancha = idCancha;
        this.nombre = nombre;
        this.deporte = deporte;
        this.precioPorHora = precioPorHora;
    }

    public int getIdCancha() { return idCancha; }
    public void setIdCancha(int idCancha) { this.idCancha = idCancha; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDeporte() { return deporte; }
    public void setDeporte(String deporte) { this.deporte = deporte; }

    public double getPrecioPorHora() { return precioPorHora; }
    public void setPrecioPorHora(double precioPorHora) { this.precioPorHora = precioPorHora; }


    /*public String toString() {
        return nombre;
    }*/
}
