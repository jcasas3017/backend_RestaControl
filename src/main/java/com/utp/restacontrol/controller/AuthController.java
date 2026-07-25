package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.auth.LoginRequest;
import com.utp.restacontrol.model.Usuario;
import com.utp.restacontrol.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.utp.restacontrol.audit.Auditable;
import com.utp.restacontrol.dto.auth.CambiarPasswordRequest;
import org.springframework.web.bind.annotation.PatchMapping;

import com.utp.restacontrol.service.AuditoriaService;
import com.utp.restacontrol.service.UsuarioCrudService;
import org.springframework.security.core.GrantedAuthority;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioCrudService usuarioCrudService;
    private final AuditoriaService auditoriaService;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public AuthController(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            UsuarioRepository usuarioRepository,
            UsuarioCrudService usuarioCrudService,
            AuditoriaService auditoriaService) {

        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.usuarioRepository = usuarioRepository;
        this.usuarioCrudService = usuarioCrudService;
        this.auditoriaService = auditoriaService;
    }

    
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request == null
                || request.getUsername() == null
                || request.getUsername().isBlank()
                || request.getPassword() == null
                || request.getPassword().isBlank()) {

            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Debes enviar usuario y contraseña"
            ));
        }

        try {
            UsernamePasswordAuthenticationToken authenticationRequest =
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.getUsername().trim(),
                            request.getPassword()
                    );

            Authentication authentication =
                    authenticationManager.authenticate(authenticationRequest);

            SecurityContext context =
                    securityContextHolderStrategy.createEmptyContext();

            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);

            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            String roles = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        auditoriaService.registrar(
                authentication.getName(),
                roles,
                "Autenticacion",
                "INICIAR_SESION",
                "El usuario inició sesión correctamente",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                obtenerIp(httpRequest),
                true,
                null
        );
            Usuario usuario = buscarUsuario(authentication.getName());

            Map<String, Object> response = construirRespuestaUsuario(
                    usuario,
                    authentication
            );

            response.put("message", "Inicio de sesión correcto");

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {


                String usernameIntentado =
                request != null
                        && request.getUsername() != null
                        && !request.getUsername().isBlank()
                        ? request.getUsername().trim()
                        : "ANONIMO";

        auditoriaService.registrar(
                usernameIntentado,
                "",
                "Autenticacion",
                "INICIAR_SESION_FALLIDO",
                "Intento fallido de inicio de sesión",
                httpRequest.getMethod(),
                httpRequest.getRequestURI(),
                obtenerIp(httpRequest),
                false,
                "Usuario o contraseña incorrectos"
        );

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "Usuario o contraseña incorrectos"
            ));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> usuarioActual(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "success", false,
                    "message", "No existe una sesión activa"
            ));
        }

        Usuario usuario = buscarUsuario(authentication.getName());

        return ResponseEntity.ok(
                construirRespuestaUsuario(usuario, authentication)
        );
    }

    @Auditable(
                modulo = "Autenticacion",
                accion = "CAMBIAR_PASSWORD_PROPIA",
                descripcion = "El usuario cambió su propia contraseña"
        )
        @PatchMapping("/me/password")
        public ResponseEntity<?> cambiarMiPassword(
                Authentication authentication,
                @RequestBody CambiarPasswordRequest request) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                        "success", false,
                        "message", "No existe una sesión activa"
                ));
        }

        try {
                usuarioCrudService.cambiarPasswordPropia(
                        authentication.getName(),
                        request
                );

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Contraseña actualizada correctamente"
                ));

        } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", ex.getMessage()
                ));
        }
        }


        @Auditable(
                modulo = "Autenticacion",
                accion = "CERRAR_SESION",
                descripcion = "El usuario cerró sesión"
        )
        @PostMapping("/logout")
        public ResponseEntity<Map<String, Object>> logout(
                Authentication authentication,
                HttpServletRequest request) {

        String username = authentication != null
                ? authentication.getName()
                : "ANONIMO";

        HttpSession session = request.getSession(false);

        if (session != null) {
                session.invalidate();
        }

        securityContextHolderStrategy.clearContext();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Sesión cerrada correctamente",
                "username", username
        ));
        }

    private Usuario buscarUsuario(String username) {
        return usuarioRepository
                .findByUsernameIgnoreCaseAndActivoTrue(username)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No se encontró el usuario autenticado"
                        )
                );
    }

    private Map<String, Object> construirRespuestaUsuario(
            Usuario usuario,
            Authentication authentication) {

        List<String> roles = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("username", usuario.getUsername());
        response.put("nombreCompleto", usuario.getName());
        response.put("rol", usuario.getRol());
        response.put("roles", roles);

        return response;
    }


    private String obtenerIp(HttpServletRequest request) {

        String forwarded =
                request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
                return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
        }
}