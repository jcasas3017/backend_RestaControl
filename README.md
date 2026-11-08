# RestaControl - Backend

Backend del sistema RestaControl, un sistema de gestión para restaurantes.

El proyecto cubre la atención en salón, la administración de clientes y usuarios,
el manejo de mesas y reservas, la carta de platos, el control de productos y la
generación de reportes. Incluye autenticación por roles y registro de auditoría.

Este repositorio contiene el backend y también sirve las pantallas web del
sistema, por lo que no se necesita levantar un servidor aparte para el frontend.

## Tecnologías

- Java 17
- Spring Boot 4.0.6
- Spring Web MVC
- Spring Data JPA
- Spring Security con BCrypt
- Thymeleaf para las vistas
- PostgreSQL
- Apache POI para exportar reportes a Excel
- Maven (el repositorio incluye el wrapper, no hace falta instalarlo)

## Requisitos previos

| Requisito | Versión | Notas |
|---|---|---|
| JDK | 17 o superior | Verificar con `java -version` |
| PostgreSQL | Compatible con PL/pgSQL y la extensión `uuid-ossp` | La versión exacta no está fijada |
| Git | Cualquiera reciente | Para clonar el repositorio |
| Maven | No se instala | Se usa `mvnw` incluido en el repositorio |

El usuario de PostgreSQL con el que se ejecuten los scripts debe tener permisos
para crear extensiones, porque el esquema usa `uuid-ossp` para generar los UUID.

## Estructura del proyecto

```
backend_RestaControl/
├── database/                  Scripts SQL de la base de datos
│   ├── 01_schema.sql          Tablas, llaves, índices, funciones y triggers
│   ├── 02_seed.sql            Catálogos y usuarios iniciales
│   ├── 03_demo_data.sql       Datos de demostración (opcional)
│   └── README.md              Guía de la base de datos y credenciales de prueba
├── src/main/java/com/utp/restacontrol/
│   ├── audit/                 Registro de auditoría
│   ├── config/                Configuración de Spring y seguridad
│   ├── controller/            Endpoints REST y controladores web
│   ├── dto/                   Objetos de transferencia de datos
│   ├── model/                 Entidades JPA
│   ├── repository/            Repositorios de acceso a datos
│   ├── service/               Lógica de negocio
│   └── util/                  Utilidades
├── src/main/resources/
│   ├── static/                CSS y JavaScript de las pantallas
│   ├── templates/             Vistas Thymeleaf
│   └── application.properties Configuración de la aplicación
├── mvnw / mvnw.cmd            Maven wrapper
└── pom.xml                    Dependencias del proyecto
```

## Instalación

### 1. Clonar el repositorio

```bash
git clone https://github.com/jcasas3017/backend_RestaControl.git
cd backend_RestaControl
```

La rama principal es `master`.

### 2. Crear la base de datos

Crear una base vacía llamada `restaurante`:

```sql
CREATE DATABASE restaurante;
```

### 3. Ejecutar los scripts SQL

Los scripts se ejecutan a mano y en este orden. La aplicación no crea las tablas
por sí sola, porque `application.properties` mantiene `spring.jpa.hibernate.ddl-auto=none`
y `spring.sql.init.mode=never`.

```bash
psql -U tu_usuario -h localhost -p tu_puerto -d restaurante -f database/01_schema.sql
psql -U tu_usuario -h localhost -p tu_puerto -d restaurante -f database/02_seed.sql
psql -U tu_usuario -h localhost -p tu_puerto -d restaurante -f database/03_demo_data.sql
```

Qué hace cada uno:

- `01_schema.sql` crea la extensión UUID, las 12 tablas, restricciones, índices, funciones, triggers y vistas. Se ejecuta siempre y sobre una base vacía.
- `02_seed.sql` carga categorías, platos, usuarios, mesas y productos.
- `03_demo_data.sql` carga clientes, reservas, atenciones y pedidos de ejemplo. Es opcional y sirve para ver el sistema con datos.

También se pueden ejecutar desde pgAdmin, abriendo cada archivo en el Query Tool.
Importante: el Query Tool debe estar conectado a la base `restaurante` y no a la
base `postgres`.

Los scripts no son idempotentes. Si fallan a medias, conviene eliminar la base y
volver a crearla antes de reintentar.

Para verificar que todo cargó, ejecutar en `restaurante`:

```sql
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;
```

Deben aparecer 12 tablas, entre ellas `auditoria`.

### 4. Configurar la conexión

Editar `src/main/resources/application.properties` con los datos de la instalación
local de PostgreSQL:

```properties
spring.application.name=restacontrol
server.port=7070

spring.datasource.url=jdbc:postgresql://localhost:TU_PUERTO/restaurante
spring.datasource.username=TU_USUARIO
spring.datasource.password=TU_PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

spring.sql.init.mode=never
spring.jpa.hibernate.ddl-auto=none
spring.jpa.open-in-view=false
```

El archivo versionado usa el puerto **5433** para PostgreSQL. La instalación por
defecto de PostgreSQL usa el **5432**, así que este es el ajuste más común al
preparar el proyecto.

No se deben subir al repositorio contraseñas reales ni credenciales de servidores
distintos al ambiente local.

## Ejecución

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux o macOS
./mvnw spring-boot:run
```

La primera ejecución descarga las dependencias de Maven y toma varios minutos.

La aplicación arranca en el puerto **7070**. El mensaje de confirmación en la
consola es parecido a este:

```
Tomcat started on port 7070 (http)
Started RestacontrolApplication in 8.123 seconds
```

Luego se abre el sistema en el navegador:

```
http://localhost:7070
```

Para compilar sin ejecutar:

```bash
.\mvnw.cmd clean package
```

## Acceso al sistema

El sistema pide usuario y contraseña. El endpoint de autenticación es
`POST /api/auth/login` y valida contra hashes BCrypt guardados en la tabla
`usuarios`.

Los cuatro usuarios de desarrollo que carga `02_seed.sql`, con sus contraseñas de
prueba y sus roles, están documentados en [`database/README.md`](database/README.md).
Son credenciales para uso local y deben reemplazarse antes de cualquier
despliegue real.

## Observaciones pendientes

Estos puntos se detectaron al validar el levantamiento del proyecto desde cero y
quedan registrados para su corrección:

1. El archivo `src/main/resources/static/js/auth.js` tiene la URL del API fija en
   `http://localhost:9090/api/auth/login`, mientras que `application.properties`
   configura el puerto **7070**. Por ese desajuste la pantalla de login que sirve
   el backend muestra "Error conectando con el backend". Conviene usar una ruta
   relativa (`/api/auth/login`), porque el backend sirve esas pantallas.

2. Existen dos versiones distintas de `auth.js`. La del repositorio
   `frontend_RestaControl` está actualizada y usa `APP_CONFIG`, incluye los
   endpoints `/me` y `/logout` y envía cookies de sesión. La copia dentro de
   `static/js/` del backend quedó en una versión anterior.

3. La pantalla de login muestra solo tres usuarios de acceso, pero `02_seed.sql`
   carga cuatro. Falta el usuario con rol Cajero.

4. `application.properties` incluye usuario y contraseña de la base de datos en
   texto plano. Aunque son valores de desarrollo local, conviene moverlos a
   variables de entorno.

5. El repositorio `frontend_RestaControl` no tiene README y no indica que el
   backend ya sirve las pantallas del sistema.
