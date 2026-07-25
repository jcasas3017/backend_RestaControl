package com.utp.restacontrol.service;

import com.utp.restacontrol.model.Usuario;
import com.utp.restacontrol.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UsuarioUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        String usernameNormalizado = username == null
                ? ""
                : username.trim();

        Usuario usuario = usuarioRepository
                .findByUsernameIgnoreCaseAndActivoTrue(usernameNormalizado)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Usuario o contraseña incorrectos"
                        )
                );

        String rol = usuario.getRol() == null
                ? "USUARIO"
                : usuario.getRol()
                    .trim()
                    .replace(" ", "_")
                    .toUpperCase(Locale.ROOT);

        return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(rol)
                .disabled(!Boolean.TRUE.equals(usuario.getActivo()))
                .build();
    }
}