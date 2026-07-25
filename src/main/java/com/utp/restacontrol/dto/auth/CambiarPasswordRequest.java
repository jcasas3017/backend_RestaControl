package com.utp.restacontrol.dto.auth;

public class CambiarPasswordRequest {

    private String passwordActual;
    private String nuevaPassword;

    public CambiarPasswordRequest() {
    }

    public CambiarPasswordRequest(String passwordActual, String nuevaPassword) {
        this.passwordActual = passwordActual;
        this.nuevaPassword = nuevaPassword;
    }

    public String getPasswordActual() {
        return passwordActual;
    }

    public void setPasswordActual(String passwordActual) {
        this.passwordActual = passwordActual;
    }

    public String getNuevaPassword() {
        return nuevaPassword;
    }

    public void setNuevaPassword(String nuevaPassword) {
        this.nuevaPassword = nuevaPassword;
    }
}