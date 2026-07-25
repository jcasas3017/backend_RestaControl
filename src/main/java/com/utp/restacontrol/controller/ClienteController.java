package com.utp.restacontrol.controller;

import com.utp.restacontrol.dto.cliente.ClienteCreateRequest;
import com.utp.restacontrol.dto.cliente.ClienteEstadoRequest;
import com.utp.restacontrol.dto.cliente.ClienteUpdateRequest;
import com.utp.restacontrol.service.ClienteCrudService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.utp.restacontrol.audit.Auditable;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteCrudService clienteCrudService;

    public ClienteController(ClienteCrudService clienteCrudService) {
        this.clienteCrudService = clienteCrudService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<?> result = clienteCrudService.listar(search, activo, page, size);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Clientes obtenidos",
                "data", result.getContent(),
                "total", result.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/buscar-por-documento")
    public ResponseEntity<?> buscarPorDocumento(@RequestParam String documento) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cliente obtenido",
                    "data", clienteCrudService.buscarPorDocumento(documento)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Cliente no encontrado".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cliente obtenido",
                    "data", clienteCrudService.obtener(id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @Auditable(
            modulo = "Clientes",
            accion = "CREAR",
            descripcion = "Registró un nuevo cliente"
    )
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ClienteCreateRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "success", true,
                    "message", "Cliente creado",
                    "data", clienteCrudService.crear(request)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @Auditable(
            modulo = "Clientes",
            accion = "ACTUALIZAR",
            descripcion = "Actualizó un cliente"
    )
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable UUID id, @RequestBody ClienteUpdateRequest request) {
        try {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cliente actualizado",
                    "data", clienteCrudService.actualizar(id, request)
            ));
        } catch (IllegalArgumentException ex) {
            if ("Cliente no encontrado".equals(ex.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
            }
            return ResponseEntity.badRequest().body(error(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error(ex.getMessage()));
        }
    }

    @Auditable(
            modulo = "Clientes",
            accion = "CAMBIAR_ESTADO",
            descripcion = "Cambió el estado de un cliente"
    )
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable UUID id, @RequestBody ClienteEstadoRequest request) {
        try {
            if (request == null || request.getActivo() == null) {
                return ResponseEntity.badRequest().body(error("El campo activo es obligatorio"));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Estado actualizado",
                    "data", clienteCrudService.cambiarEstado(id, request.getActivo())
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    @Auditable(
            modulo = "Clientes",
            accion = "ELIMINAR",
            descripcion = "Inactivó un cliente"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable UUID id) {
        try {
            clienteCrudService.eliminarLogico(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Cliente inactivado",
                    "data", Map.of("id", id)
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(ex.getMessage()));
        }
    }

    private Map<String, Object> error(String message) {
        return Map.of(
                "success", false,
                "message", message
        );
    }
}
