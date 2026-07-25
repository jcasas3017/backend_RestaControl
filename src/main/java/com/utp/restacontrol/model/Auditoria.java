package com.utp.restacontrol.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;

    @Column(name = "usuario")
    private String usuario;

    @Column(name = "rol")
    private String rol;

    @Column(name = "modulo", nullable = false)
    private String modulo;

    @Column(name = "accion", nullable = false)
    private String accion;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "metodo_http")
    private String metodoHttp;

    @Column(name = "endpoint")
    private String endpoint;

    @Column(name = "ip")
    private String ip;

    @Column(name = "exitoso", nullable = false)
    private Boolean exitoso;

    @Column(name = "error")
    private String error;

    public Auditoria() {
    }

    public UUID getId() {
        return id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getRol() {
        return rol;
    }

    public String getModulo() {
        return modulo;
    }

    public String getAccion() {
        return accion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getMetodoHttp() {
        return metodoHttp;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getIp() {
        return ip;
    }

    public Boolean getExitoso() {
        return exitoso;
    }

    public String getError() {
        return error;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public void setModulo(String modulo) {
        this.modulo = modulo;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setMetodoHttp(String metodoHttp) {
        this.metodoHttp = metodoHttp;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setExitoso(Boolean exitoso) {
        this.exitoso = exitoso;
    }

    public void setError(String error) {
        this.error = error;
    }
}