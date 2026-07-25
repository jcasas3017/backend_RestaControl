package com.utp.restacontrol.dto.reporte.carta;

import java.math.BigDecimal;
import java.util.UUID;

public record CartaItemResponse(

        UUID id,

        String tipo,

        String codigo,

        String nombre,

        UUID idCategoria,

        String categoriaNombre,

        BigDecimal precio,

        Integer stock,

        String unidadMedida,

        Boolean disponible,

        Boolean activo,

        String descripcion

) {
}