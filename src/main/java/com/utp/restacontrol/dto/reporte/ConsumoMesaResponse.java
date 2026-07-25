package com.utp.restacontrol.dto.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ConsumoMesaResponse(
        UUID idAtencion,
        String codigoAtencion,
        LocalDateTime fechaApertura,
        LocalDateTime fechaCierre,
        UUID idMesa,
        String mesaCodigo,
        String clienteNombre,
        String clienteDocumento,
        String mozoNombre,
        Integer cantidadItems,
        BigDecimal subtotal,
        BigDecimal propina,
        BigDecimal total,
        String estadoAtencion,
        String estadoPago
) {
}