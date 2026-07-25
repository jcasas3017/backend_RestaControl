package com.utp.restacontrol.dto.reporte.carta;

import java.util.List;

public record CartaReporteResponse(

        boolean success,

        String message,

        List<CartaItemResponse> data,

        long total,

        int page,

        int size,

        CartaResumenResponse summary

) {
}