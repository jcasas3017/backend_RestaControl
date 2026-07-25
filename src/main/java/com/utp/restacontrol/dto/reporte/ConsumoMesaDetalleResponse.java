package com.utp.restacontrol.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConsumoMesaDetalleResponse(
        UUID idAtencion,
        String codigoAtencion,
        String mesaCodigo,
        String clienteNombre,
        String clienteDocumento,
        String mozoNombre,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        BigDecimal subtotal,
        BigDecimal propina,
        BigDecimal total,
        String estadoAtencion,
        String estadoPago,
        String observaciones,
        List<ConsumoMesaItemResponse> items
) {
}