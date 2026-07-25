package com.utp.restacontrol.dto.reporte;

import java.math.BigDecimal;

public record ConsumoMesaResumenResponse(
        Long atenciones,
        BigDecimal totalConsumido,
        BigDecimal ticketPromedio,
        String mesaMayorConsumo,
        BigDecimal mesaMayorConsumoMonto
) {
}