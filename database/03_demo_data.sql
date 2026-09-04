-- ============================================================================
-- RestaControl - datos de demostración
--
-- Opcional. Ejecutar únicamente después de 01_schema.sql y 02_seed.sql, sobre
-- una base de desarrollo o de demostración. No ejecutar en producción.
-- Los códigos se generan por trigger para mantener las secuencias sincronizadas.
-- ============================================================================

-- Clientes ficticios.
INSERT INTO clientes (
    id,
    nombres,
    apellidos,
    documento,
    telefono,
    email,
    activo
) VALUES
    (
        '00000000-0000-0000-0000-000000002101',
        'Lucia',
        'Fernandez',
        '71234567',
        '987111222',
        'lucia@demo.com',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000002102',
        'Carlos',
        'Ramos',
        '70333444',
        '987333555',
        'carlos@demo.com',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000002103',
        'Patricia',
        'Gomez',
        '72888999',
        '982000111',
        'patty@demo.com',
        FALSE
    );

-- Reservas de demostración; dependen de clientes y mesas del seed.
INSERT INTO reservas (
    id,
    tipo,
    id_cliente,
    nombre_contacto,
    id_mesa,
    fecha_hora,
    cantidad_personas,
    estado,
    confirmada
) VALUES
    (
        '00000000-0000-0000-0000-000000005101',
        'Salon',
        '00000000-0000-0000-0000-000000002101',
        'Lucia Fernandez',
        '00000000-0000-0000-0000-000000004101',
        '2026-04-09 20:00:00',
        4,
        'confirmada',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000005102',
        'Terraza',
        '00000000-0000-0000-0000-000000002102',
        'Carlos Ramos',
        '00000000-0000-0000-0000-000000004103',
        '2026-04-10 13:00:00',
        3,
        'pendiente',
        FALSE
    );

-- Atenciones de demostración; dependen de clientes, reservas, mesas y usuarios.
INSERT INTO atenciones (
    id,
    id_cliente,
    id_reserva,
    id_mesa,
    id_mozo,
    estado,
    estado_pago,
    apertura_en,
    cierre_en,
    total_pagado
) VALUES
    (
        '00000000-0000-0000-0000-000000006101',
        '00000000-0000-0000-0000-000000002101',
        '00000000-0000-0000-0000-000000005101',
        '00000000-0000-0000-0000-000000004101',
        '00000000-0000-0000-0000-000000003103',
        'en_curso',
        'pendiente',
        '2026-04-09 20:05:00',
        NULL,
        0
    ),
    (
        '00000000-0000-0000-0000-000000006102',
        '00000000-0000-0000-0000-000000002102',
        NULL,
        '00000000-0000-0000-0000-000000004102',
        '00000000-0000-0000-0000-000000003103',
        'cerrada',
        'Pagado',
        '2026-04-09 13:10:00',
        '2026-04-09 14:30:00',
        18.50
    );

-- La atención abierta ocupa la mesa durante la demostración.
UPDATE mesas
SET estado = 'ocupada'
WHERE id = '00000000-0000-0000-0000-000000004101';

-- Pedidos de demostración.
INSERT INTO pedidos (
    id,
    id_atencion,
    creado_por,
    creado_en,
    notas
) VALUES
    (
        '00000000-0000-0000-0000-000000007101',
        '00000000-0000-0000-0000-000000006101',
        '00000000-0000-0000-0000-000000003103',
        '2026-04-09 20:10:00',
        'Sin cebolla en uno.'
    ),
    (
        '00000000-0000-0000-0000-000000007102',
        '00000000-0000-0000-0000-000000006102',
        '00000000-0000-0000-0000-000000003103',
        '2026-04-09 13:15:00',
        ''
    );

-- Detalle de los pedidos de demostración.
INSERT INTO detalle_pedidos (
    id,
    id_pedido,
    id_plato,
    cantidad,
    precio_unit,
    descuento,
    tipo_item,
    estado_cocina
) VALUES
    (
        '00000000-0000-0000-0000-000000008101',
        '00000000-0000-0000-0000-000000007101',
        '00000000-0000-0000-0000-000000001102',
        2,
        32.00,
        0,
        'plato',
        'pendiente'
    ),
    (
        '00000000-0000-0000-0000-000000008102',
        '00000000-0000-0000-0000-000000007101',
        '00000000-0000-0000-0000-000000001104',
        4,
        9.50,
        2,
        'plato',
        'pendiente'
    ),
    (
        '00000000-0000-0000-0000-000000008103',
        '00000000-0000-0000-0000-000000007102',
        '00000000-0000-0000-0000-000000001101',
        1,
        18.50,
        0,
        'plato',
        'pendiente'
    );
