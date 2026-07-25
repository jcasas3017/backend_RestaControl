package com.utp.restacontrol.controller;

import com.utp.restacontrol.service.ReporteConsumosMesaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/reportes/consumos-mesa")
public class ReporteConsumosMesaController {

    private final ReporteConsumosMesaService service;

    public ReporteConsumosMesaController(
            ReporteConsumosMesaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaDesde,

            @RequestParam(required = false)
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate fechaHasta,

            @RequestParam(required = false)
            UUID idMesa,

            @RequestParam(required = false)
            String estado,

            @RequestParam(required = false)
            String search,

            @RequestParam(defaultValue = "1")
            int page,

            @RequestParam(defaultValue = "10")
            int size) {

        return ResponseEntity.ok(
                service.listar(
                        fechaDesde,
                        fechaHasta,
                        idMesa,
                        estado,
                        search,
                        page,
                        size
                )
        );
    }

    @GetMapping("/exportar-excel")
        public ResponseEntity<byte[]> exportarExcel(
                @RequestParam(required = false)
                @DateTimeFormat(
                        iso = DateTimeFormat.ISO.DATE
                )
                LocalDate fechaDesde,

                @RequestParam(required = false)
                @DateTimeFormat(
                        iso = DateTimeFormat.ISO.DATE
                )
                LocalDate fechaHasta,

                @RequestParam(required = false)
                UUID idMesa,

                @RequestParam(required = false)
                String estado,

                @RequestParam(required = false)
                String search) {

        byte[] archivo = service.exportarExcel(
                fechaDesde,
                fechaHasta,
                idMesa,
                estado,
                search
        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=consumos_por_mesa.xlsx"
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .contentLength(archivo.length)
                .body(archivo);
        }
    @GetMapping("/{idAtencion}")
    public ResponseEntity<?> detalle(
            @PathVariable UUID idAtencion) {

        return ResponseEntity.ok(
                Map.of(
                        "success", true,
                        "message",
                        "Detalle obtenido correctamente",
                        "data",
                        service.obtenerDetalle(idAtencion)
                )
        );
    }
}