# Base de datos de RestaControl

Estos archivos concentran en el backend el esquema y los datos que antes estaban
en <code>frontend_RestaControl/db_restaurante_postgresql.sql</code>. Son scripts
de inicialización manual para una base de datos nueva; no son migraciones
idempotentes ni deben ejecutarse sobre una base con datos.

## Configuración detectada en el backend

El proyecto usa PostgreSQL mediante el driver JDBC de PostgreSQL y espera:

- Base de datos: <code>restaurante</code>
- Host y puerto de desarrollo: <code>localhost:5433</code>
- Usuario configurado actualmente: <code>postgres</code>
- Gestión de esquema: manual

<code>src/main/resources/application.properties</code> conserva deliberadamente:

~~~properties
spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=none
~~~

Por ello Spring Boot no crea ni carga estas estructuras automáticamente. La
versión exacta de PostgreSQL no está fijada en el proyecto. Se necesita una
instalación compatible con PL/pgSQL y con la extensión
<code>uuid-ossp</code>; el usuario que ejecute <code>01_schema.sql</code> debe
tener permisos para crearla. No se requiere <code>pgcrypto</code>.

Las doce tablas con clave primaria UUID, incluida <code>auditoria</code>, usan
<code>uuid_generate_v4()</code> de <code>uuid-ossp</code>.

## Propósito de cada archivo

| Archivo | Contenido | Cuándo ejecutarlo |
| --- | --- | --- |
| <code>01_schema.sql</code> | Extensión UUID, el ENUM compatible de unidad, tablas, claves, restricciones <code>CHECK</code>, índices, secuencias, funciones, triggers, vistas, comentarios y <code>auditoria</code>. | Siempre, primero y conectado a una base <code>restaurante</code> vacía. |
| <code>02_seed.sql</code> | Catálogos y configuración inicial: categorías, platos, usuarios, mesas y productos. | Después del esquema. |
| <code>03_demo_data.sql</code> | Clientes ficticios y registros transaccionales de ejemplo: reservas, atenciones, pedidos y detalles. | Opcional; solo en desarrollo o demostración, después del seed. |
| <code>README.md</code> | Guía de instalación, orden de uso, validación y diferencias detectadas. | Consultar antes de ejecutar; no es un script SQL. |

El seed y los datos de demostración no envían la columna <code>codigo</code>.
Los triggers generan los mismos prefijos del script fuente y avanzan las
secuencias, evitando que una alta posterior choque con un código ya usado.

## Crear la base de datos

Ejecuta este comando una sola vez desde una consola con <code>psql</code>
disponible:

~~~powershell
psql -h localhost -p 5433 -U postgres -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE restaurante;"
~~~

Si usas pgAdmin, crea una base llamada exactamente <code>restaurante</code>
desde la interfaz. El script fuente incluía un locale <code>es_ES.UTF-8</code>;
no se fuerza aquí porque depende del sistema operativo y de la instalación del
servidor. Configura el locale al crear la base solo si tu servidor lo tiene
disponible.

## Orden exacto de ejecución

El orden de uso es: 1) <code>01_schema.sql</code>; 2)
<code>02_seed.sql</code>; 3) <code>03_demo_data.sql</code> solo si se desean
datos ficticios; y 4) consultar este <code>README.md</code>. El README no se
ejecuta contra PostgreSQL.

Ubicado en la raíz de <code>backend_RestaControl</code>, ejecuta:

~~~powershell
psql -h localhost -p 5433 -U postgres -d restaurante -v ON_ERROR_STOP=1 -1 -f database/01_schema.sql
psql -h localhost -p 5433 -U postgres -d restaurante -v ON_ERROR_STOP=1 -1 -f database/02_seed.sql
~~~

Para cargar los datos ficticios opcionales:

~~~powershell
psql -h localhost -p 5433 -U postgres -d restaurante -v ON_ERROR_STOP=1 -1 -f database/03_demo_data.sql
~~~

<code>01_schema.sql</code> y <code>02_seed.sql</code> son obligatorios para una
instalación nueva. <code>03_demo_data.sql</code> es opcional: omitirlo deja el
esquema y los datos maestros necesarios para arrancar sin clientes, reservas ni
operaciones ficticias.

<code>-1</code> ejecuta cada archivo dentro de una transacción. Si un archivo
falla, corrige la causa antes de repetirlo; no vuelvas a ejecutar scripts ya
aplicados sobre la misma base sin restaurarla o preparar una migración explícita.

## Verificación

Después de <code>01_schema.sql</code>, comprueba las tablas:

~~~sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
~~~

El resultado debe incluir:

~~~text
atenciones, auditoria, categorias, clientes, comprobantes, detalle_pedidos,
mesas, pedidos, platos, productos, reservas, usuarios
~~~

Comprueba que las extensiones y vistas fueron creadas:

~~~sql
SELECT extname
FROM pg_extension
WHERE extname = 'uuid-ossp'
ORDER BY extname;

SELECT table_name
FROM information_schema.views
WHERE table_schema = 'public'
ORDER BY table_name;
~~~

La consulta de extensiones debe devolver solo <code>uuid-ossp</code>, requerida
por todos los DEFAULT <code>uuid_generate_v4()</code>, incluido
<code>auditoria.id</code>. <code>pgcrypto</code> no forma parte del esquema.

Después de <code>02_seed.sql</code>, verifica los registros iniciales:

~~~sql
SELECT 'categorias' AS tabla, COUNT(*) AS registros FROM categorias
UNION ALL
SELECT 'platos', COUNT(*) FROM platos
UNION ALL
SELECT 'usuarios', COUNT(*) FROM usuarios
UNION ALL
SELECT 'mesas', COUNT(*) FROM mesas
UNION ALL
SELECT 'productos', COUNT(*) FROM productos
ORDER BY tabla;
~~~

Los conteos esperados son 4 categorías, 4 platos, 4 usuarios, 4 mesas y 5
productos. También puedes verificar los códigos generados:

~~~sql
SELECT codigo, nombre, orden, activo
FROM categorias
ORDER BY orden;

SELECT codigo, username, rol, activo
FROM usuarios
ORDER BY codigo;
~~~

Después de <code>03_demo_data.sql</code>, los conteos de demostración esperados
son:

~~~sql
SELECT 'clientes' AS tabla, COUNT(*) AS registros FROM clientes
UNION ALL
SELECT 'reservas', COUNT(*) FROM reservas
UNION ALL
SELECT 'atenciones', COUNT(*) FROM atenciones
UNION ALL
SELECT 'pedidos', COUNT(*) FROM pedidos
UNION ALL
SELECT 'detalle_pedidos', COUNT(*) FROM detalle_pedidos
ORDER BY tabla;
~~~

Debe devolver 3 clientes, 2 reservas, 2 atenciones, 2 pedidos y 3 detalles.

## Hallazgos técnicos documentados

### Tipos controlados y backend actual

El backend escribe varios campos mediante JPA o JDBC como <code>String</code>.
Un ENUM nombrado de PostgreSQL no es compatible de forma fiable con esos
parámetros <code>VARCHAR</code>: afecta, entre otros, a
<code>repository.save(...)</code>, filtros JPQL con <code>LOWER(...)</code> y
<code>PreparedStatement.setString(...)</code>. Para que el código actual
funcione sin casts ni cambios Java, el esquema usa <code>VARCHAR + CHECK</code>
en esos dominios.

| Columnas con <code>VARCHAR + CHECK</code> | Motivo |
| --- | --- |
| <code>usuarios.rol</code>, <code>mesas.estado</code>, <code>reservas.tipo</code>, <code>reservas.estado</code> | Son campos <code>String</code> de entidades JPA y se escriben con <code>repository.save</code>. |
| <code>atenciones.estado</code>, <code>atenciones.estado_pago</code>, <code>pedidos.estado</code> | Estados usados por SQL/JDBC actual sin un mapeo ENUM JPA explícito. |
| <code>detalle_pedidos.tipo_item</code>, <code>detalle_pedidos.estado_cocina</code> | Se insertan o actualizan con <code>setString</code>; cocina también filtra por parámetro. |
| <code>comprobantes.tipo_comprobante</code>, <code>comprobantes.metodo_pago</code>, <code>comprobantes.estado</code> | El flujo de cobro inserta <code>metodo_pago</code> con <code>setString</code> y consulta estas columnas como texto. |

Los CHECK conservan los valores actualmente admitidos por el esquema y el
backend. En particular, se mantienen <code>Completada</code>,
<code>Cerrada</code> y <code>Pagado</code> porque el flujo de cobro los escribe
literalmente; no son valores históricos sin uso.

El único ENUM PostgreSQL es <code>unidad_enum</code> de
<code>productos.unidad</code>. La entidad <code>Producto</code> usa
<code>Unidad</code>, <code>@Enumerated(EnumType.STRING)</code> y
<code>@JdbcTypeCode(SqlTypes.NAMED_ENUM)</code>, por lo que ese mapeo sí es
explícito y compatible.

Puedes comprobar los dominios validados por el esquema con:

~~~sql
SELECT conrelid::regclass AS tabla, conname, pg_get_constraintdef(oid) AS regla
FROM pg_constraint
WHERE contype = 'c'
ORDER BY conrelid::regclass::text, conname;
~~~

Y para comprobar que solo <code>productos.unidad</code> mantiene un ENUM:

~~~sql
SELECT table_name, column_name, data_type, udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (
      (table_name = 'productos' AND column_name = 'unidad')
      OR (table_name, column_name) IN (
          ('usuarios', 'rol'),
          ('mesas', 'estado'),
          ('reservas', 'tipo'),
          ('reservas', 'estado'),
          ('atenciones', 'estado'),
          ('atenciones', 'estado_pago'),
          ('pedidos', 'estado'),
          ('detalle_pedidos', 'tipo_item'),
          ('detalle_pedidos', 'estado_cocina'),
          ('comprobantes', 'tipo_comprobante'),
          ('comprobantes', 'metodo_pago'),
          ('comprobantes', 'estado')
      )
  )
ORDER BY table_name, column_name;
~~~

También existe una diferencia de presentación en cocina: la capa de operaciones
puede guardar <code>entregado</code>, pero <code>CocinaRepository</code> solo
traduce <code>despachado</code> como “Entregado”. No se modificó ese código en
esta tarea.

El script también conserva dos diferencias de la fuente frente a las entidades:
<code>clientes.documento</code> sigue siendo nullable y
<code>reservas.id_mesa</code> sigue admitiendo <code>NULL</code>, aunque el
backend los trata como obligatorios. No se endurecieron esas restricciones porque
esta tarea organiza el esquema existente, no lo rediseña.

### Auditoría y UUID

La tabla <code>auditoria</code> estaba después del marcador final del script
fuente, pese a que el backend la utiliza. Ahora forma parte del esquema. Como su
UUID usa <code>uuid_generate_v4()</code>, igual que las otras once tablas con
clave primaria UUID, el esquema requiere únicamente <code>uuid-ossp</code>.
No hay ningún uso de <code>gen_random_uuid()</code> ni dependencia de
<code>pgcrypto</code>.

Las entidades JPA usan identificadores <code>UUID</code>; no definen un
generador alternativo que requiera cambiar estos DEFAULT. Por tanto, los tipos
son coherentes y los DEFAULT siguen sirviendo para inserciones que omitan
explícitamente el identificador.

<code>auditoria</code> coincide exactamente con la entidad
<code>Auditoria</code>: <code>id</code>, <code>fecha</code>,
<code>usuario</code>, <code>rol</code>, <code>modulo</code>,
<code>accion</code>, <code>descripcion</code>, <code>metodo_http</code>,
<code>endpoint</code>, <code>ip</code>, <code>exitoso</code> y
<code>error</code>. No se añadieron columnas.

### Movimientos de stock

El backend intenta insertar en <code>movimientos_stock</code> dentro de un
savepoint y captura el error como opcional. No hay tabla ni diseño de kardex en
el SQL fuente, por lo que no se crea aquí. Si el proyecto requiere historial de
stock, debe definirse como una tarea y migración separadas.

### Credenciales iniciales de desarrollo

<code>POST /api/auth/login</code> usa <code>AuthenticationManager</code> con
<code>DaoAuthenticationProvider</code> y <code>BCryptPasswordEncoder</code>.
Por ello, <code>usuarios.password</code> almacena hashes BCrypt de coste 10;
ninguna contraseña en texto plano se guarda en <code>02_seed.sql</code>.

| Usuario | Contraseña de desarrollo |
| --- | --- |
| <code>marcos</code> | <code>admin123</code> |
| <code>elena</code> | <code>recep123</code> |
| <code>dario</code> | <code>mozo123</code> |
| <code>sofia</code> | <code>cajero123</code> |

Los cuatro usuarios seed quedan activos y sus hashes fueron generados para las
claves de la tabla, por lo que pueden autenticarse mediante el endpoint de
login en una instalación nueva. Estas credenciales son solo para desarrollo y
deben reemplazarse antes de cualquier despliegue real.
