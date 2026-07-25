package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Auditoria;
import com.utp.restacontrol.repository.AuditoriaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(
            AuditoriaRepository repository) {

        this.repository = repository;
    }

    @Async
    public void registrar(
            String usuario,
            String rol,
            String modulo,
            String accion,
            String descripcion,
            String metodoHttp,
            String endpoint,
            String ip,
            boolean exitoso,
            String error) {

        try {
            Auditoria auditoria = new Auditoria();

            auditoria.setFecha(LocalDateTime.now());
            auditoria.setUsuario(usuario);
            auditoria.setRol(rol);
            auditoria.setModulo(modulo);
            auditoria.setAccion(accion);
            auditoria.setDescripcion(descripcion);
            auditoria.setMetodoHttp(metodoHttp);
            auditoria.setEndpoint(endpoint);
            auditoria.setIp(ip);
            auditoria.setExitoso(exitoso);
            auditoria.setError(error);

            repository.save(auditoria);

        } catch (Exception exception) {
            System.err.println(
                    "No se pudo registrar la auditoría: "
                    + exception.getMessage()
            );
        }
    }
}