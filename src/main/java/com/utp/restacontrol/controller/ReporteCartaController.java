package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.ReporteCartaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.utp.restacontrol.audit.Auditable;


import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/reportes/carta")
public class ReporteCartaController {

    private final ReporteCartaService service;

    public ReporteCartaController(
            ReporteCartaService service) {

        this.service = service;
    }

    @Auditable(
        modulo = "Carta",
        accion = "CONSULTAR",
        descripcion = "Consultó el reporte de carta"
        )

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false)
            String tipo,

            @RequestParam(required = false)
            UUID idCategoria,

            @RequestParam(required = false)
            String estado,

            @RequestParam(required = false)
            String disponibilidad,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {


        return ResponseEntity.ok(
                service.listar(
                        tipo,
                        idCategoria,
                        estado,
                        disponibilidad,
                        search,
                        page,
                        size
                )
        );
    }

    @Auditable(
                modulo = "Carta",
                accion = "EXPORTAR",
                descripcion = "Exportó el reporte de carta a Excel"
        )
        @GetMapping("/exportar-excel")
        public ResponseEntity<byte[]> exportarExcel(
                @RequestParam(required = false)
                String tipo,

                @RequestParam(required = false)
                UUID idCategoria,

                @RequestParam(required = false)
                String estado,

                @RequestParam(required = false)
                String disponibilidad,

                @RequestParam(required = false)
                String search) {

        byte[] archivo = service.exportarExcel(
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=reporte_carta.xlsx"
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .contentLength(archivo.length)
                .body(archivo);
        }


    @Auditable(
        modulo = "Carta",
        accion = "VER_DETALLE",
        descripcion = "Consultó el detalle de un elemento de carta"
        )
    @GetMapping("/{tipo}/{id}")
    public ResponseEntity<?> detalle(
            @PathVariable String tipo,
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Detalle obtenido correctamente",
                        "data",
                        service.obtenerDetalle(tipo, id)
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<?> handleIllegalArgument(
                IllegalArgumentException exception) {

            return ResponseEntity.badRequest().body(
                    Map.of(
                            "success", false,
                            "message", exception.getMessage(),
                            "error", "VALIDACION"
                    )
            );
        }
}