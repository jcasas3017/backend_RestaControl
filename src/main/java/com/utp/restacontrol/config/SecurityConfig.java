package com.utp.restacontrol.config;

import com.utp.restacontrol.service.UsuarioUserDetailsService;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
            .cors(cors ->
                cors.configurationSource(corsConfigurationSource())
            )

            // Temporalmente desactivado porque el frontend es una API
            // separada. Más adelante puede implementarse CSRF para SPA.
            .csrf(csrf -> csrf.disable())

            .securityContext(context ->
                context.securityContextRepository(
                    securityContextRepository
                )
            )

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.IF_REQUIRED
                )
            )

            .authenticationProvider(authenticationProvider)

            .authorizeHttpRequests(auth -> auth

                /*
                 * ENDPOINTS PÚBLICOS
                 */
                .requestMatchers(
                    "/api/auth/login",
                    "/error"
                ).permitAll()

                /*
                 * ENDPOINTS DE SESIÓN
                 */
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).authenticated()

                /*
                 * DASHBOARD
                 * Disponible para cualquier usuario autenticado.
                 */
                .requestMatchers(
                    "/api/dashboard"
                ).authenticated()

                /*
                 * MÓDULOS ADMINISTRATIVOS
                 * Solo Administrador.
                 */
                .requestMatchers(
                    "/api/usuarios/**",
                    "/api/categorias/**",
                    "/api/platos/**",
                    "/api/productos/**"
                ).hasRole("ADMINISTRADOR")

                /*
                 * RECEPCIÓN
                 * Administrador y Recepción.
                 */
                .requestMatchers(
                    "/api/clientes/**",
                    "/api/mesas/**",
                    "/api/reservas/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "RECEPCION"
                )

                /*
                 * OPERACIÓN DE MESAS Y ATENCIONES
                 * Administrador y Mozo.
                 */
                .requestMatchers(
                    "/api/lista-mesas/**",
                    "/api/operacion/**",
                    "/api/atenciones/**",
                    "/api/pedidos/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "MOZO"
                )

                /*
                 * COCINA
                 */
                .requestMatchers(
                    "/api/cocina/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "COCINERO"
                )

                /*
                 * REPORTES DE VENTAS Y CAJA
                 */
                .requestMatchers(
                    "/api/reportes/ventas",
                    "/api/reportes/ventas/**",
                    "/api/reportes/caja",
                    "/api/reportes/caja/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "CAJERO"
                )

                /*
                 * REPORTE DE RESERVAS
                 */
                .requestMatchers(
                    "/api/reportes/reservas",
                    "/api/reportes/reservas/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "RECEPCION"
                )

                /*
                 * REPORTE DE PLATOS CONSUMIDOS
                 */
                .requestMatchers(
                    "/api/reportes/platos-consumidos",
                    "/api/reportes/platos-consumidos/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "CAJERO",
                    "COCINERO"
                )

               

                .requestMatchers(
                        HttpMethod.GET,
                        "/api/reportes/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "CAJERO"
                )
                .requestMatchers(
                    "/api/reportes/consumos-mesa",
                    "/api/reportes/consumos-mesa/**"
                ).hasAnyRole(
                    "ADMINISTRADOR",
                    "CAJERO"
                )

                .requestMatchers(
                        "/api/reportes/carta",
                        "/api/reportes/carta/**"
                ).hasAnyRole(
                        "ADMINISTRADOR",
                        "CAJERO"
                )
                 /*
                 * Cualquier otro endpoint API requiere al menos
                 * una sesión autenticada.
                 */
                .requestMatchers("/api/**").authenticated()

                /*
                 * El frontend HTML está ejecutándose en el puerto 5500,
                 * por eso las demás rutas del backend quedan libres.
                 */
                .anyRequest().permitAll()
            );

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration =
                new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
            "http://localhost:5500"
        ));

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
            "/**",
            configuration
        );

        return source;
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UsuarioUserDetailsService usuarioUserDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                    usuarioUserDetailsService
                );

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }
}