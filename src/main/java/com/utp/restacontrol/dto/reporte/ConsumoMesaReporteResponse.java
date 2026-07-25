package com.utp.restacontrol.dto.reporte;

import java.util.List;

public record ConsumoMesaReporteResponse(
        boolean success,
        String message,
        List<ConsumoMesaResponse> data,
        long total,
        int page,
        int size,
        ConsumoMesaResumenResponse summary
) {
}