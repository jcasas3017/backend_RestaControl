-- ============================================================================
-- RestaControl - datos iniciales indispensables
--
-- Ejecutar después de 01_schema.sql sobre una base de datos nueva.
-- Los códigos se omiten intencionalmente: los triggers los generan y avanzan
-- sus secuencias, evitando colisiones en las siguientes altas del sistema.
-- ============================================================================

-- Categorías de carta
INSERT INTO categorias (id, nombre, orden, activo) VALUES
    ('00000000-0000-0000-0000-000000000101', 'Entradas', 1, TRUE),
    ('00000000-0000-0000-0000-000000000102', 'Fondos', 2, TRUE),
    ('00000000-0000-0000-0000-000000000103', 'Bebidas', 3, TRUE),
    ('00000000-0000-0000-0000-000000000104', 'Postres', 4, FALSE);

-- Platos base; dependen de categorias.
INSERT INTO platos (
    id,
    id_categoria,
    nombre,
    descripcion,
    precio,
    disponible,
    activo
) VALUES
    (
        '00000000-0000-0000-0000-000000001101',
        '00000000-0000-0000-0000-000000000101',
        'Causa limeña',
        'Papa amarilla, pollo y palta.',
        18.50,
        TRUE,
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000001102',
        '00000000-0000-0000-0000-000000000102',
        'Lomo saltado',
        'Clasico con papas crocantes.',
        32.00,
        TRUE,
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000001103',
        '00000000-0000-0000-0000-000000000102',
        'Aji de gallina',
        'Guiso cremoso tradicional.',
        28.00,
        FALSE,
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000001104',
        '00000000-0000-0000-0000-000000000103',
        'Chicha morada',
        'Bebida de maiz morado.',
        9.50,
        TRUE,
        TRUE
    );

-- Usuarios iniciales de desarrollo. La columna password almacena hashes BCrypt
-- de coste 10 compatibles con POST /api/auth/login; no contiene texto plano.
-- Las credenciales de desarrollo se documentan exclusivamente en README.md.
INSERT INTO usuarios (
    id,
    nombres,
    apellidos,
    username,
    password,
    rol,
    activo
) VALUES
    (
        '00000000-0000-0000-0000-000000003101',
        'Marcos',
        'Salazar',
        'marcos',
        '$2a$10$w6IqZw2Oz8vBRjUa1d2pjuiC/Xx0NuoK8WE5tSNMgMZYIYvexflZ2',
        'Administrador',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000003102',
        'Elena',
        'Torres',
        'elena',
        '$2a$10$BSC4ko09C.I72PAAsuwlcOkApBXtQOaEvyyCCDGpNlEMtxSzsI7P6',
        'Recepcion',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000003103',
        'Dario',
        'Lopez',
        'dario',
        '$2a$10$yko3yN6l0k5bUdqOTArw4uQEV0NVQieYZGlDyvNK6Q3BB9mlIFNzS',
        'Mozo',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000003104',
        'Sofia',
        'Marin',
        'sofia',
        '$2a$10$HIL.FD6lSQf3gBVnQQ/XsuZD97Djwl4MNLcUv.Fp04VbwQAqNYAF.',
        'Cajero',
        TRUE
    );

-- Mesas iniciales.
INSERT INTO mesas (
    id,
    capacidad,
    ubicacion,
    activa,
    estado
) VALUES
    (
        '00000000-0000-0000-0000-000000004101',
        4,
        'Ventana',
        TRUE,
        'disponible'
    ),
    (
        '00000000-0000-0000-0000-000000004102',
        2,
        'Salon central',
        TRUE,
        'disponible'
    ),
    (
        '00000000-0000-0000-0000-000000004103',
        6,
        'Terraza',
        TRUE,
        'disponible'
    ),
    (
        '00000000-0000-0000-0000-000000004104',
        8,
        'VIP',
        FALSE,
        'mantenimiento'
    );

-- Productos base.
INSERT INTO productos (
    id,
    nombre,
    descripcion,
    precio,
    stock,
    stock_minimo,
    unidad,
    activo
) VALUES
    (
        '00000000-0000-0000-0000-000000009101',
        'Inca Kola 500ml',
        'Gaseosa nacional 500ml',
        5.00,
        50,
        20,
        'unidad',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000009102',
        'Coca Cola 500ml',
        'Gaseosa importada 500ml',
        5.50,
        40,
        20,
        'unidad',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000009103',
        'Cerveza Pilsen',
        'Cerveza lata 355ml',
        9.00,
        30,
        15,
        'unidad',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000009104',
        'Agua San Luis',
        'Agua mineral 625ml',
        3.50,
        60,
        30,
        'unidad',
        TRUE
    ),
    (
        '00000000-0000-0000-0000-000000009105',
        'Jugo de naranja',
        'Jugo natural 300ml',
        7.00,
        20,
        10,
        'unidad',
        TRUE
    );
