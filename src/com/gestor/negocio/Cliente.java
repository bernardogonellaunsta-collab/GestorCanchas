package com.gestor.negocio;

public class Cliente {
    private int idCliente;
    private String nombreCliente;
    private String telefono;

    public Cliente() {}

    public Cliente(int idCliente, String nombreCliente, String telefono) {
        this.idCliente = idCliente;
        this.nombreCliente = nombreCliente;
        this.telefono = telefono;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String nombreCliente) { this.nombreCliente = nombreCliente; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    @Override
    public String toString() {
        return nombreCliente;
    }
}
