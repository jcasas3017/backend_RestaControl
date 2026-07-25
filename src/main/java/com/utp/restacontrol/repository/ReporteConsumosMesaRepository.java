package com.utp.restacontrol.repository;

import com.utp.restacontrol.dto.reporte.ConsumoMesaDetalleResponse;
import com.utp.restacontrol.dto.reporte.ConsumoMesaItemResponse;
import com.utp.restacontrol.dto.reporte.ConsumoMesaResponse;
import com.utp.restacontrol.dto.reporte.ConsumoMesaResumenResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReporteConsumosMesaRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReporteConsumosMesaRepository(
            JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ConsumoMesaResponse> listar(
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            UUID idMesa,
            String estado,
            String search,
            int page,
            int size) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                a.id AS id_atencion,
                a.codigo AS codigo_atencion,
                a.apertura_en AS fecha_apertura,
                a.cierre_en AS fecha_cierre,
                m.id AS id_mesa,
                m.codigo AS mesa_codigo,
                CONCAT(c.nombres, ' ', c.apellidos) AS cliente_nombre,
                c.documento AS cliente_documento,
                CONCAT(u.nombres, ' ', u.apellidos) AS mozo_nombre,
                COALESCE(SUM(dp.cantidad), 0) AS cantidad_items,
                COALESCE(
                    SUM(
                        (dp.cantidad * dp.precio_unit)
                        - COALESCE(dp.descuento, 0)
                    ),
                    0
                ) AS subtotal,
                COALESCE(MAX(a.propina),0) AS propina,
                COALESCE(
                    MAX(a.total_pagado),
                    SUM(
                        (dp.cantidad * dp.precio_unit)
                        - COALESCE(dp.descuento,0)
                    )
                ) AS total,
                a.estado::text AS estado_atencion,
                a.estado_pago::text AS estado_pago
            FROM atenciones a
            INNER JOIN clientes c
                ON c.id = a.id_cliente
            INNER JOIN mesas m
                ON m.id = a.id_mesa
            INNER JOIN usuarios u
                ON u.id = a.id_mozo
            LEFT JOIN pedidos p
                ON p.id_atencion = a.id
            LEFT JOIN detalle_pedidos dp
                ON dp.id_pedido = p.id
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        if (fechaDesde != null) {
            sql.append("""
                AND a.apertura_en >= ?::date
                """);

            params.add(fechaDesde);
        }

        if (fechaHasta != null) {
            sql.append("""
                AND a.apertura_en < (?::date + INTERVAL '1 day')
                """);

            params.add(fechaHasta);
        }

        if (idMesa != null) {
            sql.append("""
                AND a.id_mesa = ?
                """);

            params.add(idMesa);
        }

        if (estado != null && !estado.isBlank()) {
            String estadoNormalizado = estado.trim().toLowerCase();

            if ("pagada".equals(estadoNormalizado)) {
                sql.append("""
                    AND LOWER(a.estado_pago::text) = 'pagado'
                    """);
            } else {
                sql.append("""
                    AND LOWER(a.estado::text) = ?
                    """);

                params.add(estadoNormalizado);
            }
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                AND (
                    LOWER(a.codigo) LIKE ?
                    OR LOWER(m.codigo) LIKE ?
                    OR LOWER(CONCAT(c.nombres, ' ', c.apellidos)) LIKE ?
                    OR LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE ?
                    OR LOWER(c.documento) LIKE ?
                )
                """);

            String value = "%" + search.trim().toLowerCase() + "%";

            params.add(value);
            params.add(value);
            params.add(value);
            params.add(value);
            params.add(value);
        }

        sql.append("""
            GROUP BY
                a.id,
                a.codigo,
                a.apertura_en,
                a.cierre_en,
                m.id,
                m.codigo,
                c.nombres,
                c.apellidos,
                c.documento,
                u.nombres,
                u.apellidos,
                a.estado,
                a.estado_pago
            ORDER BY a.apertura_en DESC
            LIMIT ? OFFSET ?
            """);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        params.add(safeSize);
        params.add(offset);

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new ConsumoMesaResponse(
                        rs.getObject(
                                "id_atencion",
                                UUID.class
                        ),
                        rs.getString("codigo_atencion"),
                        rs.getTimestamp("fecha_apertura") == null
                                ? null
                                : rs.getTimestamp(
                                        "fecha_apertura"
                                ).toLocalDateTime(),
                        rs.getTimestamp("fecha_cierre") == null
                                ? null
                                : rs.getTimestamp(
                                        "fecha_cierre"
                                ).toLocalDateTime(),
                        rs.getObject(
                                "id_mesa",
                                UUID.class
                        ),
                        rs.getString("mesa_codigo"),
                        rs.getString("cliente_nombre"),
                        rs.getString("cliente_documento"),
                        rs.getString("mozo_nombre"),
                        rs.getInt("cantidad_items"),
                        getBigDecimal(rs.getBigDecimal("subtotal")),
                        getBigDecimal(rs.getBigDecimal("propina")),
                        getBigDecimal(rs.getBigDecimal("total")),
                        rs.getString("estado_atencion"),
                        rs.getString("estado_pago")
                ),
                params.toArray()
        );
    }

    private static BigDecimal getBigDecimal(
            BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value;
    }
    public long contar(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        UUID idMesa,
        String estado,
        String search) {

    StringBuilder sql = new StringBuilder("""
        SELECT COUNT(DISTINCT a.id)
        FROM atenciones a
        INNER JOIN clientes c
            ON c.id = a.id_cliente
        INNER JOIN mesas m
            ON m.id = a.id_mesa
        INNER JOIN usuarios u
            ON u.id = a.id_mozo
        WHERE 1 = 1
        """);

    List<Object> params = new ArrayList<>();

    agregarFiltros(
            sql,
            params,
            fechaDesde,
            fechaHasta,
            idMesa,
            estado,
            search
    );

    Long total = jdbcTemplate.queryForObject(
            sql.toString(),
            Long.class,
            params.toArray()
    );

    return total == null ? 0 : total;
}
private void agregarFiltros(
        StringBuilder sql,
        List<Object> params,
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        UUID idMesa,
        String estado,
        String search) {

    if (fechaDesde != null) {
        sql.append("""
            AND a.apertura_en >= ?::date
            """);

        params.add(fechaDesde);
    }

    if (fechaHasta != null) {
        sql.append("""
            AND a.apertura_en < (?::date + INTERVAL '1 day')
            """);

        params.add(fechaHasta);
    }

    if (idMesa != null) {
        sql.append("""
            AND a.id_mesa = ?
            """);

        params.add(idMesa);
    }

    if (estado != null && !estado.isBlank()) {
        String estadoNormalizado =
                estado.trim().toLowerCase();

        if ("pagada".equals(estadoNormalizado)) {
            sql.append("""
                AND LOWER(a.estado_pago::text) = 'pagado'
                """);
        } else {
            sql.append("""
                AND LOWER(a.estado::text) = ?
                """);

            params.add(estadoNormalizado);
        }
    }

    if (search != null && !search.isBlank()) {
        sql.append("""
            AND (
                LOWER(a.codigo) LIKE ?
                OR LOWER(m.codigo) LIKE ?
                OR LOWER(CONCAT(c.nombres, ' ', c.apellidos)) LIKE ?
                OR LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE ?
                OR LOWER(c.documento) LIKE ?
            )
            """);

        String value =
                "%" + search.trim().toLowerCase() + "%";

        params.add(value);
        params.add(value);
        params.add(value);
        params.add(value);
        params.add(value);
    }
}
public ConsumoMesaResumenResponse obtenerResumen(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        UUID idMesa,
        String estado,
        String search) {

    StringBuilder sql = new StringBuilder("""
        WITH consumos AS (
            SELECT
                a.id,
                m.codigo AS mesa_codigo,
                COALESCE(
                    MAX(a.total_pagado),
                    SUM(
                        (dp.cantidad * dp.precio_unit)
                        - COALESCE(dp.descuento,0)
                    )
                ) AS total
            FROM atenciones a
            INNER JOIN clientes c
                ON c.id = a.id_cliente
            INNER JOIN mesas m
                ON m.id = a.id_mesa
            INNER JOIN usuarios u
                ON u.id = a.id_mozo
            LEFT JOIN pedidos p
                ON p.id_atencion = a.id
            LEFT JOIN detalle_pedidos dp
                ON dp.id_pedido = p.id
            WHERE 1 = 1
        """);

    List<Object> params = new ArrayList<>();

    agregarFiltros(
            sql,
            params,
            fechaDesde,
            fechaHasta,
            idMesa,
            estado,
            search
    );

    sql.append("""
            GROUP BY a.id, m.codigo
        ),
        mesas_acumuladas AS (
            SELECT
                mesa_codigo,
                SUM(total) AS total_mesa
            FROM consumos
            GROUP BY mesa_codigo
        ),
        mejor_mesa AS (
            SELECT
                mesa_codigo,
                total_mesa
            FROM mesas_acumuladas
            ORDER BY total_mesa DESC
            LIMIT 1
        )
        SELECT
            COUNT(*) AS atenciones,
            COALESCE(SUM(total), 0) AS total_consumido,
            CASE
                WHEN COUNT(*) > 0
                THEN COALESCE(SUM(total), 0) / COUNT(*)
                ELSE 0
            END AS ticket_promedio,
            (
                SELECT mesa_codigo
                FROM mejor_mesa
            ) AS mesa_mayor_consumo,
            COALESCE(
                (
                    SELECT total_mesa
                    FROM mejor_mesa
                ),
                0
            ) AS mesa_mayor_consumo_monto
        FROM consumos
        """);

    return jdbcTemplate.queryForObject(
            sql.toString(),
            (rs, rowNum) ->
                    new ConsumoMesaResumenResponse(
                            rs.getLong("atenciones"),
                            getBigDecimal(
                                    rs.getBigDecimal(
                                            "total_consumido"
                                    )
                            ),
                            getBigDecimal(
                                    rs.getBigDecimal(
                                            "ticket_promedio"
                                    )
                            ),
                            rs.getString(
                                    "mesa_mayor_consumo"
                            ),
                            getBigDecimal(
                                    rs.getBigDecimal(
                                            "mesa_mayor_consumo_monto"
                                    )
                            )
                    ),
            params.toArray()
    );
}
public Optional<ConsumoMesaDetalleResponse> obtenerDetalle(
        UUID idAtencion) {

    List<ConsumoMesaDetalleResponse> cabeceras =
            jdbcTemplate.query(
                    """
                    SELECT
                        a.id AS id_atencion,
                        a.codigo AS codigo_atencion,
                        m.codigo AS mesa_codigo,
                        CONCAT(c.nombres, ' ', c.apellidos)
                            AS cliente_nombre,
                        c.documento AS cliente_documento,
                        CONCAT(u.nombres, ' ', u.apellidos)
                            AS mozo_nombre,
                        a.apertura_en AS fecha_apertura,
                        a.cierre_en AS fecha_cierre,
                        a.estado::text AS estado_atencion,
                        a.estado_pago::text AS estado_pago,

                        COALESCE(a.propina, 0) AS propina,
                        COALESCE(a.total_pagado, 0) AS total,

                        COALESCE(
                            STRING_AGG(
                                DISTINCT NULLIF(p.notas, ''),
                                ' | '
                            ),
                            ''
                        ) AS observaciones
                    FROM atenciones a
                    INNER JOIN clientes c
                        ON c.id = a.id_cliente
                    INNER JOIN mesas m
                        ON m.id = a.id_mesa
                    INNER JOIN usuarios u
                        ON u.id = a.id_mozo
                    LEFT JOIN pedidos p
                        ON p.id_atencion = a.id
                    WHERE a.id = ?
                    GROUP BY
                        a.id,
                        a.codigo,
                        m.codigo,
                        c.nombres,
                        c.apellidos,
                        c.documento,
                        u.nombres,
                        u.apellidos,
                        a.apertura_en,
                        a.cierre_en,
                        a.estado,
                        a.estado_pago,
                        a.propina,
                        a.total_pagado
                    """,
                    (rs, rowNum) ->
                            new ConsumoMesaDetalleResponse(
                                    rs.getObject(
                                            "id_atencion",
                                            UUID.class
                                    ),
                                    rs.getString(
                                            "codigo_atencion"
                                    ),
                                    rs.getString(
                                            "mesa_codigo"
                                    ),
                                    rs.getString(
                                            "cliente_nombre"
                                    ),
                                    rs.getString(
                                            "cliente_documento"
                                    ),
                                    rs.getString(
                                            "mozo_nombre"
                                    ),
                                    rs.getTimestamp(
                                            "fecha_apertura"
                                    ) == null
                                            ? null
                                            : rs.getTimestamp(
                                                    "fecha_apertura"
                                            ).toLocalDateTime(),
                                    rs.getTimestamp(
                                            "fecha_cierre"
                                    ) == null
                                            ? null
                                            : rs.getTimestamp(
                                                    "fecha_cierre"
                                            ).toLocalDateTime(),
                                    BigDecimal.ZERO,

                                    getBigDecimal(
                                            rs.getBigDecimal("propina")
                                    ),

                                    getBigDecimal(
                                            rs.getBigDecimal("total")
                                    ),
                                    rs.getString(
                                            "estado_atencion"
                                    ),
                                    rs.getString(
                                            "estado_pago"
                                    ),
                                    rs.getString(
                                            "observaciones"
                                    ),
                                    List.of()
                            ),
                    idAtencion
            );

    if (cabeceras.isEmpty()) {
        return Optional.empty();
    }

    ConsumoMesaDetalleResponse cabecera =
            cabeceras.get(0);

    List<ConsumoMesaItemResponse> items =
            jdbcTemplate.query(
                    """
                    SELECT
                        dp.id AS id_detalle,
                        COALESCE(
                            pl.nombre,
                            pr.nombre,
                            'Ítem'
                        ) AS nombre_item,
                        CASE
                            WHEN dp.id_plato IS NOT NULL
                                THEN 'plato'
                            WHEN dp.id_producto IS NOT NULL
                                THEN 'producto'
                            ELSE 'item'
                        END AS tipo_item,
                        dp.cantidad,
                        dp.precio_unit,
                        COALESCE(dp.descuento, 0)
                            AS descuento,
                        (
                            dp.cantidad * dp.precio_unit
                        ) - COALESCE(dp.descuento, 0)
                            AS subtotal
                    FROM pedidos p
                    INNER JOIN detalle_pedidos dp
                        ON dp.id_pedido = p.id
                    LEFT JOIN platos pl
                        ON pl.id = dp.id_plato
                    LEFT JOIN productos pr
                        ON pr.id = dp.id_producto
                    WHERE p.id_atencion = ?
                    ORDER BY
                        p.creado_en,
                        dp.fecha_creacion
                    """,
                    (rs, rowNum) ->
                            new ConsumoMesaItemResponse(
                                    rs.getObject(
                                            "id_detalle",
                                            UUID.class
                                    ),
                                    rs.getString(
                                            "nombre_item"
                                    ),
                                    rs.getString(
                                            "tipo_item"
                                    ),
                                    rs.getInt("cantidad"),
                                    getBigDecimal(
                                            rs.getBigDecimal(
                                                    "precio_unit"
                                            )
                                    ),
                                    getBigDecimal(
                                            rs.getBigDecimal(
                                                    "descuento"
                                            )
                                    ),
                                    getBigDecimal(
                                            rs.getBigDecimal(
                                                    "subtotal"
                                            )
                                    )
                            ),
                    idAtencion
            );

    BigDecimal subtotal = items.stream()
            .map(ConsumoMesaItemResponse::subtotal)
            .reduce(
                    BigDecimal.ZERO,
                    BigDecimal::add
            );

    BigDecimal propina = cabecera.propina();

    BigDecimal total = cabecera.total();

    if (total.compareTo(BigDecimal.ZERO) == 0) {
        total = subtotal.add(propina);
    }

    return Optional.of(
            new ConsumoMesaDetalleResponse(
                    cabecera.idAtencion(),
                    cabecera.codigoAtencion(),
                    cabecera.mesaCodigo(),
                    cabecera.clienteNombre(),
                    cabecera.clienteDocumento(),
                    cabecera.mozoNombre(),
                    cabecera.fechaApertura(),
                    cabecera.fechaCierre(),
                    subtotal,
                    propina,
                    total,
                    cabecera.estadoAtencion(),
                    cabecera.estadoPago(),
                    cabecera.observaciones(),
                    items
            )
    );
}


}