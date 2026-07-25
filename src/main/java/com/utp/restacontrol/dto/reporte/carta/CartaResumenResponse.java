package com.utp.restacontrol.dto.reporte.carta;

public record CartaResumenResponse(

        Long total,

        Long platos,

        Long productos,

        Long disponibles

) {
}