-- ============================================================================
-- RestaControl - esquema PostgreSQL
-- Fuente inicial: frontend_RestaControl/db_restaurante_postgresql.sql
--
-- Ejecutar este archivo conectado previamente a la base de datos "restaurante".
-- No crea la base de datos y no contiene datos iniciales ni de demostración.
-- ============================================================================

-- ============================================================================
-- EXTENSIONES Y TIPOS PERSONALIZADOS
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- unidad_enum se conserva porque Producto.unidad usa @Enumerated(EnumType.STRING)
-- junto con @JdbcTypeCode(SqlTypes.NAMED_ENUM). Los demás dominios controlados
-- usan VARCHAR + CHECK para ser compatibles con String en JPA y JDBC.
CREATE TYPE unidad_enum AS ENUM (
    'unidad',
    'litro',
    'kg',
    'gramo'
);

-- ============================================================================
-- FUNCIONES BASE
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_generar_codigo(
    p_prefijo TEXT,
    p_secuencia TEXT,
    p_ancho INT DEFAULT 6
)
RETURNS TEXT AS $$
DECLARE
    v_numero BIGINT;
BEGIN
    EXECUTE format('SELECT nextval(%L)', p_secuencia) INTO v_numero;
    RETURN p_prefijo || lpad(v_numero::TEXT, p_ancho, '0');
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_asignar_codigo()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.codigo IS NULL OR NEW.codigo = '' THEN
        NEW.codigo := fn_generar_codigo(TG_ARGV[0], TG_ARGV[1]);
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_actualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.fecha_actualizacion := CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TABLAS
-- ============================================================================

CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(100) NOT NULL UNIQUE,
    orden INT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE platos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    id_categoria UUID NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    disponible BOOLEAN DEFAULT TRUE,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_categoria) REFERENCES categorias(id) ON DELETE RESTRICT
);

CREATE TABLE clientes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    documento VARCHAR(20) UNIQUE,
    telefono VARCHAR(20),
    email VARCHAR(100),
    direccion TEXT,
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL CHECK (
        rol IN ('Administrador', 'Recepcion', 'Mozo', 'Cajero', 'Cocinero')
    ),
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE mesas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    capacidad INT NOT NULL,
    ubicacion VARCHAR(100),
    activa BOOLEAN DEFAULT TRUE,
    estado VARCHAR(30) NOT NULL DEFAULT 'disponible' CHECK (
        estado IN ('disponible', 'ocupada', 'reservada', 'mantenimiento')
    ),
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL CHECK (
        tipo IN ('Salon', 'Terraza', 'Privado')
    ),
    id_cliente UUID NOT NULL,
    nombre_contacto VARCHAR(150) NOT NULL,
    id_mesa UUID,
    fecha_hora TIMESTAMP NOT NULL,
    cantidad_personas INT NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente' CHECK (
        estado IN ('pendiente', 'confirmada', 'cancelada', 'atendida', 'Completada')
    ),
    confirmada BOOLEAN DEFAULT FALSE,
    notas TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_mesa) REFERENCES mesas(id) ON DELETE SET NULL
);

CREATE TABLE atenciones (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    id_cliente UUID NOT NULL,
    id_reserva UUID,
    id_mesa UUID NOT NULL,
    id_mozo UUID NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'en_curso' CHECK (
        estado IN ('en_curso', 'cerrada', 'cancelada', 'Cerrada')
    ),
    estado_pago VARCHAR(20) NOT NULL DEFAULT 'pendiente' CHECK (
        estado_pago IN ('pendiente', 'Pagado')
    ),
    apertura_en TIMESTAMP NOT NULL,
    cierre_en TIMESTAMP NULL,
    total_pagado DECIMAL(10, 2) DEFAULT 0,
    propina DECIMAL(10, 2) DEFAULT 0,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_reserva) REFERENCES reservas(id) ON DELETE SET NULL,
    FOREIGN KEY (id_mesa) REFERENCES mesas(id) ON DELETE RESTRICT,
    FOREIGN KEY (id_mozo) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE pedidos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    id_atencion UUID NOT NULL,
    creado_por UUID NOT NULL,
    creado_en TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'pendiente' CHECK (
        estado IN ('pendiente')
    ),
    notas TEXT,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_atencion) REFERENCES atenciones(id) ON DELETE RESTRICT,
    FOREIGN KEY (creado_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

CREATE TABLE detalle_pedidos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_pedido UUID NOT NULL,
    id_plato UUID,
    id_producto UUID,
    cantidad INT NOT NULL,
    precio_unit DECIMAL(10, 2) NOT NULL,
    descuento DECIMAL(10, 2) DEFAULT 0,
    tipo_item VARCHAR(20) NOT NULL DEFAULT 'plato' CHECK (
        tipo_item IN ('plato', 'producto', 'otro')
    ),
    estado_cocina VARCHAR(30) NOT NULL DEFAULT 'pendiente' CHECK (
        estado_cocina IN (
            'pendiente', 'en preparacion', 'listo', 'despachado', 'entregado', 'cancelado'
        )
    ),
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pedido) REFERENCES pedidos(id) ON DELETE CASCADE,
    FOREIGN KEY (id_plato) REFERENCES platos(id) ON DELETE RESTRICT,
    CHECK (
        (tipo_item = 'plato' AND id_plato IS NOT NULL AND id_producto IS NULL)
        OR
        (tipo_item = 'producto' AND id_producto IS NOT NULL AND id_plato IS NULL)
        OR
        (tipo_item = 'otro' AND id_plato IS NULL AND id_producto IS NULL)
    )
);

CREATE TABLE productos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nombre VARCHAR(150) NOT NULL UNIQUE,
    descripcion TEXT,
    precio DECIMAL(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    stock_minimo INT DEFAULT 10,
    unidad unidad_enum NOT NULL DEFAULT 'unidad',
    activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE detalle_pedidos
    ADD CONSTRAINT fk_detalle_pedidos_producto
    FOREIGN KEY (id_producto) REFERENCES productos(id) ON DELETE RESTRICT;

CREATE TABLE comprobantes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_atencion UUID NOT NULL,
    numero_comprobante VARCHAR(50) UNIQUE NOT NULL,
    tipo_comprobante VARCHAR(20) NOT NULL DEFAULT 'Boleta' CHECK (
        tipo_comprobante IN ('Boleta', 'Factura', 'Ticket')
    ),
    monto_subtotal DECIMAL(10, 2) NOT NULL,
    monto_igv DECIMAL(10, 2) DEFAULT 0,
    monto_descuento DECIMAL(10, 2) DEFAULT 0,
    monto_total DECIMAL(10, 2) NOT NULL,
    metodo_pago VARCHAR(30) NOT NULL DEFAULT 'Efectivo' CHECK (
        metodo_pago IN ('Efectivo', 'Tarjeta', 'Transferencia', 'Mixto')
    ),
    estado VARCHAR(20) NOT NULL DEFAULT 'Generado' CHECK (
        estado IN ('Generado', 'Emitido', 'Anulado')
    ),
    emitido_por UUID NOT NULL,
    fecha_emision TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_atencion) REFERENCES atenciones(id) ON DELETE RESTRICT,
    FOREIGN KEY (emitido_por) REFERENCES usuarios(id) ON DELETE RESTRICT
);

-- Esta tabla estaba después del marcador FIN del script fuente, pero es usada
-- por la entidad y el aspecto de auditoría del backend.
CREATE TABLE auditoria (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario VARCHAR(100),
    rol VARCHAR(80),
    modulo VARCHAR(100) NOT NULL,
    accion VARCHAR(50) NOT NULL,
    descripcion TEXT,
    metodo_http VARCHAR(10),
    endpoint VARCHAR(255),
    ip VARCHAR(80),
    exitoso BOOLEAN NOT NULL DEFAULT TRUE,
    error TEXT
);

-- ============================================================================
-- ÍNDICES
-- ============================================================================

CREATE INDEX idx_platos_categoria ON platos(id_categoria);
CREATE INDEX idx_platos_disponible ON platos(disponible);
CREATE INDEX idx_clientes_documento ON clientes(documento);
CREATE INDEX idx_clientes_email ON clientes(email);
CREATE INDEX idx_usuarios_rol ON usuarios(rol);
CREATE INDEX idx_usuarios_activo ON usuarios(activo);
CREATE INDEX idx_mesas_estado ON mesas(estado);
CREATE INDEX idx_reservas_cliente ON reservas(id_cliente);
CREATE INDEX idx_reservas_mesa ON reservas(id_mesa);
CREATE INDEX idx_reservas_fecha ON reservas(fecha_hora);
CREATE INDEX idx_atenciones_mesa ON atenciones(id_mesa);
CREATE INDEX idx_atenciones_mozo ON atenciones(id_mozo);
CREATE INDEX idx_atenciones_estado ON atenciones(estado);
CREATE INDEX idx_pedidos_atencion ON pedidos(id_atencion);
CREATE INDEX idx_pedidos_estado ON pedidos(estado);
CREATE INDEX idx_detalle_pedido ON detalle_pedidos(id_pedido);
CREATE INDEX idx_detalle_plato ON detalle_pedidos(id_plato);
CREATE INDEX idx_detalle_producto ON detalle_pedidos(id_producto);
CREATE INDEX idx_productos_stock ON productos(stock);
CREATE INDEX idx_comprobantes_atencion ON comprobantes(id_atencion);
CREATE INDEX idx_comprobantes_fecha ON comprobantes(fecha_emision);

-- ============================================================================
-- SECUENCIAS, FUNCIONES Y TRIGGERS DE CÓDIGOS
-- ============================================================================

CREATE SEQUENCE seq_categorias_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_platos_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_clientes_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_usuarios_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_mesas_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_reservas_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_atenciones_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_pedidos_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_productos_codigo START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE seq_comprobantes_numero START WITH 1 INCREMENT BY 1;

CREATE TRIGGER trg_categorias_codigo
BEFORE INSERT ON categorias
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('CAT', 'seq_categorias_codigo');

CREATE TRIGGER trg_platos_codigo
BEFORE INSERT ON platos
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('PLA', 'seq_platos_codigo');

CREATE TRIGGER trg_clientes_codigo
BEFORE INSERT ON clientes
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('CLI', 'seq_clientes_codigo');

CREATE TRIGGER trg_usuarios_codigo
BEFORE INSERT ON usuarios
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('USR', 'seq_usuarios_codigo');

CREATE TRIGGER trg_mesas_codigo
BEFORE INSERT ON mesas
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('MESA', 'seq_mesas_codigo');

CREATE TRIGGER trg_reservas_codigo
BEFORE INSERT ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('RES', 'seq_reservas_codigo');

CREATE TRIGGER trg_atenciones_codigo
BEFORE INSERT ON atenciones
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('ATE', 'seq_atenciones_codigo');

CREATE TRIGGER trg_pedidos_codigo
BEFORE INSERT ON pedidos
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('PED', 'seq_pedidos_codigo');

CREATE TRIGGER trg_productos_codigo
BEFORE INSERT ON productos
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_codigo('PROD', 'seq_productos_codigo');

CREATE OR REPLACE FUNCTION fn_asignar_numero_comprobante()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.numero_comprobante IS NULL OR NEW.numero_comprobante = '' THEN
        NEW.numero_comprobante := fn_generar_codigo('COM', 'seq_comprobantes_numero');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_comprobantes_numero
BEFORE INSERT ON comprobantes
FOR EACH ROW
EXECUTE FUNCTION fn_asignar_numero_comprobante();

-- ============================================================================
-- FUNCIONES Y PROCEDIMIENTOS ALMACENADOS
-- ============================================================================

CREATE OR REPLACE FUNCTION fn_total_pedido(p_id_pedido UUID)
RETURNS DECIMAL AS $$
DECLARE
    total DECIMAL(10, 2);
BEGIN
    SELECT COALESCE(SUM(cantidad * precio_unit - descuento), 0)
    INTO total
    FROM detalle_pedidos
    WHERE id_pedido = p_id_pedido;
    RETURN total;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_estado_mesa(p_id_mesa UUID)
RETURNS VARCHAR AS $$
DECLARE
    estado VARCHAR(30);
BEGIN
    SELECT m.estado
    INTO estado
    FROM mesas m
    WHERE m.id = p_id_mesa;
    RETURN estado;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_total_ventas_rango(
    p_fecha_inicio TIMESTAMP,
    p_fecha_fin TIMESTAMP
)
RETURNS DECIMAL AS $$
DECLARE
    total DECIMAL(10, 2);
BEGIN
    SELECT COALESCE(SUM(c.monto_total), 0)
    INTO total
    FROM comprobantes c
    WHERE c.fecha_emision BETWEEN p_fecha_inicio AND p_fecha_fin
      AND c.estado = 'Emitido';
    RETURN total;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sp_crear_atencion(
    p_id_cliente UUID,
    p_id_mesa UUID,
    p_id_mozo UUID
)
RETURNS UUID AS $$
DECLARE
    v_id_atencion UUID;
BEGIN
    INSERT INTO atenciones (
        id_cliente,
        id_mesa,
        id_mozo,
        apertura_en,
        estado,
        estado_pago
    )
    VALUES (
        p_id_cliente,
        p_id_mesa,
        p_id_mozo,
        NOW(),
        'en_curso',
        'pendiente'
    )
    RETURNING id INTO v_id_atencion;

    UPDATE mesas SET estado = 'ocupada' WHERE id = p_id_mesa;

    RETURN v_id_atencion;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sp_cerrar_atencion(
    p_id_atencion UUID
)
RETURNS VOID AS $$
BEGIN
    UPDATE atenciones
    SET estado = 'cerrada',
        cierre_en = NOW(),
        estado_pago = 'pendiente'
    WHERE id = p_id_atencion;

    UPDATE mesas
    SET estado = 'disponible'
    WHERE id = (SELECT id_mesa FROM atenciones WHERE id = p_id_atencion);
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION sp_registrar_pago(
    p_id_atencion UUID,
    p_monto DECIMAL,
    p_metodo VARCHAR
)
RETURNS VOID AS $$
BEGIN
    UPDATE atenciones
    SET total_pagado = p_monto,
        estado_pago = 'Pagado'
    WHERE id = p_id_atencion;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- TRIGGERS DE TIMESTAMP
-- ============================================================================

CREATE TRIGGER trg_categorias_actualizar_timestamp
BEFORE UPDATE ON categorias
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_platos_actualizar_timestamp
BEFORE UPDATE ON platos
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_clientes_actualizar_timestamp
BEFORE UPDATE ON clientes
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_usuarios_actualizar_timestamp
BEFORE UPDATE ON usuarios
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_mesas_actualizar_timestamp
BEFORE UPDATE ON mesas
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_reservas_actualizar_timestamp
BEFORE UPDATE ON reservas
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_atenciones_actualizar_timestamp
BEFORE UPDATE ON atenciones
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_pedidos_actualizar_timestamp
BEFORE UPDATE ON pedidos
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_detalle_pedidos_actualizar_timestamp
BEFORE UPDATE ON detalle_pedidos
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_productos_actualizar_timestamp
BEFORE UPDATE ON productos
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

CREATE TRIGGER trg_comprobantes_actualizar_timestamp
BEFORE UPDATE ON comprobantes
FOR EACH ROW
EXECUTE FUNCTION fn_actualizar_timestamp();

-- ============================================================================
-- VISTAS
-- ============================================================================

CREATE OR REPLACE VIEW v_atenciones_detalle AS
SELECT
    a.id,
    c.nombres AS cliente_nombres,
    c.apellidos AS cliente_apellidos,
    m.codigo AS mesa_codigo,
    u.nombres AS mozo_nombres,
    a.estado,
    a.estado_pago,
    a.apertura_en,
    a.cierre_en
FROM atenciones a
JOIN clientes c ON a.id_cliente = c.id
JOIN mesas m ON a.id_mesa = m.id
JOIN usuarios u ON a.id_mozo = u.id;

CREATE OR REPLACE VIEW v_pedidos_detalle AS
SELECT
    p.id AS pedido_id,
    COALESCE(pl.nombre, pr.nombre) AS item_nombre,
    dp.tipo_item,
    dp.cantidad,
    dp.precio_unit,
    (dp.cantidad * dp.precio_unit - dp.descuento) AS subtotal,
    dp.estado_cocina,
    dp.observaciones
FROM pedidos p
JOIN detalle_pedidos dp ON p.id = dp.id_pedido
LEFT JOIN platos pl ON dp.id_plato = pl.id AND dp.tipo_item = 'plato'
LEFT JOIN productos pr ON dp.id_producto = pr.id AND dp.tipo_item = 'producto'
ORDER BY p.id, COALESCE(pl.nombre, pr.nombre);

CREATE OR REPLACE VIEW v_reservas_pendientes AS
SELECT
    r.id,
    c.nombres AS cliente_nombres,
    r.fecha_hora,
    r.cantidad_personas,
    m.codigo AS mesa_codigo,
    r.estado
FROM reservas r
JOIN clientes c ON r.id_cliente = c.id
LEFT JOIN mesas m ON r.id_mesa = m.id
WHERE r.estado = 'pendiente'
ORDER BY r.fecha_hora ASC;

CREATE OR REPLACE VIEW v_atenciones_abiertas AS
SELECT
    a.id,
    m.codigo AS mesa,
    c.nombres,
    a.apertura_en,
    COUNT(dp.id) AS total_items
FROM atenciones a
JOIN mesas m ON a.id_mesa = m.id
JOIN clientes c ON a.id_cliente = c.id
LEFT JOIN pedidos p ON a.id = p.id_atencion
LEFT JOIN detalle_pedidos dp ON p.id = dp.id_pedido
WHERE a.estado = 'en_curso'
GROUP BY a.id, m.codigo, c.nombres, a.apertura_en
ORDER BY a.apertura_en DESC;

-- ============================================================================
-- COMENTARIOS
-- ============================================================================

COMMENT ON TABLE categorias IS 'Categorías de platos disponibles en el restaurante';
COMMENT ON TABLE platos IS 'Menú de platos del restaurante con precios y disponibilidad';
COMMENT ON TABLE clientes IS 'Registro de clientes del restaurante';
COMMENT ON TABLE usuarios IS 'Personal del restaurante con roles y permisos';
COMMENT ON TABLE mesas IS 'Mesas disponibles en el restaurante';
COMMENT ON TABLE reservas IS 'Reservas de mesas realizadas por clientes';
COMMENT ON TABLE atenciones IS 'Servicios/atenciones brindadas a los clientes';
COMMENT ON TABLE pedidos IS 'Pedidos realizados durante una atención';
COMMENT ON TABLE detalle_pedidos IS 'Detalles de items en cada pedido';
COMMENT ON TABLE productos IS 'Productos de venta (bebidas, insumos, etc.)';
COMMENT ON TABLE comprobantes IS 'Comprobantes de venta (boletas, facturas)';
COMMENT ON TABLE auditoria IS 'Eventos de auditoría generados por el backend';
