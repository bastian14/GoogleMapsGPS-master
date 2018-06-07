package cl.ubiobio.mapa;

import java.util.Date;

public class Farmacia {

    private String fecha;
    private String local_id;
    private String region_id;
    private String comuna_id;
    private String localidad_id;
    private String nombre_farmacia;
    private String nombre_comuna;
    private String direccion_farmacia;
    private String horario_apertura;
    private String horario_cierre;
    private double latitud;
    private double longitud;
    private String telefono;

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public Farmacia(){}

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getLocal_id() {
        return local_id;
    }

    public void setLocal_id(String local_id) {
        this.local_id = local_id;
    }

    public String getRegion_id() {
        return region_id;
    }

    public void setRegion_id(String region_id) {
        this.region_id = region_id;
    }

    public String getComuna_id() {
        return comuna_id;
    }

    public void setComuna_id(String comuna_id) {
        this.comuna_id = comuna_id;
    }

    public String getLocalidad_id() {
        return localidad_id;
    }

    public void setLocalidad_id(String localidad_id) {
        this.localidad_id = localidad_id;
    }

    public String getNombre_farmacia() {
        return nombre_farmacia;
    }

    public void setNombre_farmacia(String nombre_farmacia) {
        this.nombre_farmacia = nombre_farmacia;
    }

    public String getNombre_comuna() {
        return nombre_comuna;
    }

    public void setNombre_comuna(String nombre_comuna) {
        this.nombre_comuna = nombre_comuna;
    }

    public String getDireccion_farmacia() {
        return direccion_farmacia;
    }

    public void setDireccion_farmacia(String direccion_farmacia) {
        this.direccion_farmacia = direccion_farmacia;
    }

    public String getHorario_apertura() {
        return horario_apertura;
    }

    public void setHorario_apertura(String horario_apertura) {
        this.horario_apertura = horario_apertura;
    }

    public String getHorario_cierre() {
        return horario_cierre;
    }

    public void setHorario_cierre(String horario_cierre) {
        this.horario_cierre = horario_cierre;
    }

    public double getLatitud() {
        return latitud;
    }

    public void setLatitud(double latitud) {
        this.latitud = latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public void setLongitud(double longitud) {
        this.longitud = longitud;
    }

}
