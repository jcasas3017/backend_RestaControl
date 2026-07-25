package com.utp.restacontrol.dto.reporte;

import java.math.BigDecimal;
import java.util.UUID;

public record ConsumoMesaItemResponse(
        UUID idDetalle,
        String nombreItem,
        String tipoItem,
        Integer cantidad,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal subtotal
) {
}