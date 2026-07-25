package com.utp.restacontrol.audit;

import com.utp.restacontrol.service.AuditoriaService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.http.ResponseEntity;

import java.util.stream.Collectors;

@Aspect
@Component
public class AuditoriaAspect {

    private final AuditoriaService auditoriaService;

    public AuditoriaAspect(
            AuditoriaService auditoriaService) {

        this.auditoriaService = auditoriaService;
    }

    @Around("@annotation(auditable)")
    public Object auditar(
            ProceedingJoinPoint joinPoint,
            Auditable auditable)
            throws Throwable {

        long inicio = System.currentTimeMillis();

        String usuario = obtenerUsuario();
        String rol = obtenerRoles();
        String metodoHttp = null;
        String endpoint = null;
        String ip = null;

        HttpServletRequest request = obtenerRequest();

        if (request != null) {
            metodoHttp = request.getMethod();
            endpoint = request.getRequestURI();
            ip = obtenerIp(request);
        }

        boolean exitoso = false;
        String error = null;

        try {
            Object resultado = joinPoint.proceed();

            if (resultado instanceof ResponseEntity<?> response) {
                exitoso = response.getStatusCode().is2xxSuccessful();

                if (!exitoso) {
                    error = "La operación respondió con estado HTTP "
                            + response.getStatusCode().value();
                }
            } else {
                exitoso = true;
            }

            return resultado;

        } catch (Throwable exception) {
            exitoso = false;

            error = limitarTexto(
                    exception.getMessage(),
                    2000
            );

            throw exception;
        } finally {
            long duracion = System.currentTimeMillis() - inicio;

            String descripcion =
                    auditable.descripcion().isBlank()
                            ? construirDescripcion(
                                    joinPoint,
                                    duracion
                            )
                            : auditable.descripcion()
                                    + " | "
                                    + duracion
                                    + " ms";

            auditoriaService.registrar(
                    usuario,
                    rol,
                    auditable.modulo(),
                    auditable.accion(),
                    descripcion,
                    metodoHttp,
                    endpoint,
                    ip,
                    exitoso,
                    error
            );
        }
    }

    private String obtenerUsuario() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
            authentication == null ||
            !authentication.isAuthenticated()
        ) {
            return "ANONIMO";
        }

        return authentication.getName();
    }

    private String obtenerRoles() {
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null) {
            return "";
        }

        return authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    private HttpServletRequest obtenerRequest() {
        var attributes =
                RequestContextHolder
                        .getRequestAttributes();

        if (
            attributes instanceof
            ServletRequestAttributes servletAttributes
        ) {
            return servletAttributes.getRequest();
        }

        return null;
    }

    private String obtenerIp(
            HttpServletRequest request) {

        String forwarded =
                request.getHeader("X-Forwarded-For");

        if (
            forwarded != null &&
            !forwarded.isBlank()
        ) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    private String construirDescripcion(
            ProceedingJoinPoint joinPoint,
            long duracion) {

        return joinPoint
                .getSignature()
                .toShortString()
                + " | "
                + duracion
                + " ms";
    }

    private String limitarTexto(
            String value,
            int maxLength) {

        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}