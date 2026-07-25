package com.utp.restacontrol.repository;

import com.utp.restacontrol.dto.reporte.carta.CartaItemResponse;
import com.utp.restacontrol.dto.reporte.carta.CartaResumenResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ReporteCartaRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReporteCartaRepository(
            JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    private static BigDecimal decimal(
            BigDecimal value) {

        return value == null
                ? BigDecimal.ZERO
                : value;
    }


    public List<CartaItemResponse> listar(
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search,
            int page,
            int size) {

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM (
                SELECT
                    pl.id,
                    'plato'::text AS tipo,
                    pl.codigo,
                    pl.nombre,
                    ca.id AS id_categoria,
                    ca.nombre AS categoria_nombre,
                    pl.precio::numeric AS precio,
                    NULL::integer AS stock,
                    NULL::text AS unidad_medida,
                    pl.disponible AS disponible,
                    pl.activo AS activo,
                    COALESCE(pl.descripcion, '') AS descripcion
                FROM platos pl
                INNER JOIN categorias ca
                    ON ca.id = pl.id_categoria

                UNION ALL

                SELECT
                    pr.id,
                    'producto'::text AS tipo,
                    pr.codigo,
                    pr.nombre,
                    NULL::uuid AS id_categoria,
                    'Productos'::text AS categoria_nombre,
                    pr.precio::numeric AS precio,
                    pr.stock,
                    pr.unidad::text AS unidad_medida,
                    (pr.stock > 0) AS disponible,
                    pr.activo AS activo,
                    COALESCE(pr.descripcion, '') AS descripcion
                FROM productos pr
            ) carta
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        agregarFiltros(
                sql,
                params,
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        sql.append("""
            ORDER BY
                carta.tipo,
                carta.categoria_nombre,
                carta.nombre
            LIMIT ? OFFSET ?
            """);

        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        int offset = (safePage - 1) * safeSize;

        params.add(safeSize);
        params.add(offset);

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new CartaItemResponse(
                        rs.getObject("id", UUID.class),
                        rs.getString("tipo"),
                        rs.getString("codigo"),
                        rs.getString("nombre"),
                        rs.getObject(
                                "id_categoria",
                                UUID.class
                        ),
                        rs.getString("categoria_nombre"),
                        decimal(
                                rs.getBigDecimal("precio")
                        ),
                        rs.getObject(
                                "stock",
                                Integer.class
                        ),
                        rs.getString("unidad_medida"),
                        rs.getBoolean("disponible"),
                        rs.getBoolean("activo"),
                        rs.getString("descripcion")
                ),
                params.toArray()
        );
    }

    private void agregarFiltros(
            StringBuilder sql,
            List<Object> params,
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search) {

        if (tipo != null && !tipo.isBlank()) {
            sql.append("""
                AND carta.tipo = ?
                """);

            params.add(
                    tipo.trim().toLowerCase()
            );
        }

        if (idCategoria != null) {
            sql.append("""
                AND carta.id_categoria = ?
                """);

            params.add(idCategoria);
        }

        if (estado != null && !estado.isBlank()) {
            String value =
                    estado.trim().toLowerCase();

            if ("activo".equals(value)) {
                sql.append("""
                    AND carta.activo = true
                    """);
            }

            if ("inactivo".equals(value)) {
                sql.append("""
                    AND carta.activo = false
                    """);
            }
        }

        if (
            disponibilidad != null &&
            !disponibilidad.isBlank()
        ) {
            String value =
                    disponibilidad.trim().toLowerCase();

            if ("disponible".equals(value)) {
                sql.append("""
                    AND carta.disponible = true
                    """);
            }

            if ("no_disponible".equals(value)) {
                sql.append("""
                    AND carta.disponible = false
                    """);
            }
        }

        if (search != null && !search.isBlank()) {
            sql.append("""
                AND (
                    LOWER(carta.codigo) LIKE ?
                    OR LOWER(carta.nombre) LIKE ?
                    OR LOWER(carta.categoria_nombre) LIKE ?
                    OR LOWER(carta.descripcion) LIKE ?
                )
                """);

            String value =
                    "%" + search.trim().toLowerCase() + "%";

            params.add(value);
            params.add(value);
            params.add(value);
            params.add(value);
        }
    }


    public long contar(
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search) {

        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM (
                SELECT
                    pl.id,
                    'plato'::text AS tipo,
                    pl.codigo,
                    pl.nombre,
                    ca.id AS id_categoria,
                    ca.nombre AS categoria_nombre,
                    pl.disponible AS disponible,
                    pl.activo AS activo,
                    COALESCE(pl.descripcion, '') AS descripcion
                FROM platos pl
                INNER JOIN categorias ca
                    ON ca.id = pl.id_categoria

                UNION ALL

                SELECT
                    pr.id,
                    'producto'::text AS tipo,
                    pr.codigo,
                    pr.nombre,
                    NULL::uuid AS id_categoria,
                    'Productos'::text AS categoria_nombre,
                    (pr.stock > 0) AS disponible,
                    pr.activo AS activo,
                    COALESCE(pr.descripcion, '') AS descripcion
                FROM productos pr
            ) carta
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        agregarFiltros(
                sql,
                params,
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        Long total = jdbcTemplate.queryForObject(
                sql.toString(),
                Long.class,
                params.toArray()
        );

        return total == null ? 0 : total;
    }

    public CartaResumenResponse obtenerResumen(
            String tipo,
            UUID idCategoria,
            String estado,
            String disponibilidad,
            String search) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                COUNT(*) AS total,
                COUNT(*) FILTER (
                    WHERE carta.tipo = 'plato'
                ) AS platos,
                COUNT(*) FILTER (
                    WHERE carta.tipo = 'producto'
                ) AS productos,
                COUNT(*) FILTER (
                    WHERE carta.disponible = true
                    AND carta.activo = true
                ) AS disponibles
            FROM (
                SELECT
                    pl.id,
                    'plato'::text AS tipo,
                    pl.codigo,
                    pl.nombre,
                    ca.id AS id_categoria,
                    ca.nombre AS categoria_nombre,
                    pl.disponible AS disponible,
                    pl.activo AS activo,
                    COALESCE(pl.descripcion, '') AS descripcion
                FROM platos pl
                INNER JOIN categorias ca
                    ON ca.id = pl.id_categoria

                UNION ALL

                SELECT
                    pr.id,
                    'producto'::text AS tipo,
                    pr.codigo,
                    pr.nombre,
                    NULL::uuid AS id_categoria,
                    'Productos'::text AS categoria_nombre,
                    (pr.stock > 0) AS disponible,
                    pr.activo AS activo,
                    COALESCE(pr.descripcion, '') AS descripcion
                FROM productos pr
            ) carta
            WHERE 1 = 1
            """);

        List<Object> params = new ArrayList<>();

        agregarFiltros(
                sql,
                params,
                tipo,
                idCategoria,
                estado,
                disponibilidad,
                search
        );

        return jdbcTemplate.queryForObject(
                sql.toString(),
                (rs, rowNum) ->
                        new CartaResumenResponse(
                                rs.getLong("total"),
                                rs.getLong("platos"),
                                rs.getLong("productos"),
                                rs.getLong("disponibles")
                        ),
                params.toArray()
        );
    }

    public Optional<CartaItemResponse> obtenerDetalle(
            String tipo,
            UUID id) {

        if ("plato".equalsIgnoreCase(tipo)) {
            return obtenerDetallePlato(id);
        }

        if ("producto".equalsIgnoreCase(tipo)) {
            return obtenerDetalleProducto(id);
        }

        return Optional.empty();
    }

    private Optional<CartaItemResponse> obtenerDetallePlato(
            UUID id) {

        List<CartaItemResponse> rows =
                jdbcTemplate.query(
                        """
                        SELECT
                            pl.id,
                            'plato'::text AS tipo,
                            pl.codigo,
                            pl.nombre,
                            ca.id AS id_categoria,
                            ca.nombre AS categoria_nombre,
                            pl.precio::numeric AS precio,
                            NULL::integer AS stock,
                            NULL::text AS unidad_medida,
                            pl.disponible AS disponible,
                            pl.activo AS activo,
                            COALESCE(
                                pl.descripcion,
                                ''
                            ) AS descripcion
                        FROM platos pl
                        INNER JOIN categorias ca
                            ON ca.id = pl.id_categoria
                        WHERE pl.id = ?
                        """,
                        (rs, rowNum) ->
                                new CartaItemResponse(
                                        rs.getObject(
                                                "id",
                                                UUID.class
                                        ),
                                        rs.getString("tipo"),
                                        rs.getString("codigo"),
                                        rs.getString("nombre"),
                                        rs.getObject(
                                                "id_categoria",
                                                UUID.class
                                        ),
                                        rs.getString(
                                                "categoria_nombre"
                                        ),
                                        decimal(
                                                rs.getBigDecimal(
                                                        "precio"
                                                )
                                        ),
                                        null,
                                        null,
                                        rs.getBoolean(
                                                "disponible"
                                        ),
                                        rs.getBoolean("activo"),
                                        rs.getString(
                                                "descripcion"
                                        )
                                ),
                        id
                );

        return rows.stream().findFirst();
    }
    private Optional<CartaItemResponse> obtenerDetalleProducto(
            UUID id) {

        List<CartaItemResponse> rows =
                jdbcTemplate.query(
                        """
                        SELECT
                            pr.id,
                            'producto'::text AS tipo,
                            pr.codigo,
                            pr.nombre,
                            NULL::uuid AS id_categoria,
                            'Productos'::text AS categoria_nombre,
                            pr.precio::numeric AS precio,
                            pr.stock,
                            pr.unidad::text AS unidad_medida,
                            (pr.stock > 0) AS disponible,
                            pr.activo AS activo,
                            COALESCE(
                                pr.descripcion,
                                ''
                            ) AS descripcion
                        FROM productos pr
                        WHERE pr.id = ?
                        """,
                        (rs, rowNum) ->
                                new CartaItemResponse(
                                        rs.getObject(
                                                "id",
                                                UUID.class
                                        ),
                                        rs.getString("tipo"),
                                        rs.getString("codigo"),
                                        rs.getString("nombre"),
                                        null,
                                        rs.getString(
                                                "categoria_nombre"
                                        ),
                                        decimal(
                                                rs.getBigDecimal(
                                                        "precio"
                                                )
                                        ),
                                        rs.getObject(
                                                "stock",
                                                Integer.class
                                        ),
                                        rs.getString(
                                                "unidad_medida"
                                        ),
                                        rs.getBoolean(
                                                "disponible"
                                        ),
                                        rs.getBoolean("activo"),
                                        rs.getString(
                                                "descripcion"
                                        )
                                ),
                        id
                );

        return rows.stream().findFirst();
    }

}