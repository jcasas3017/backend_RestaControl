# Revisión inicial de configuración del proyecto

Revisión de la configuración del backend de RestaControl realizada sobre la rama `master`
(commit `780513e`).

## 1. Stack tecnológico (`pom.xml`)

| Elemento | Valor |
| --- | --- |
| Framework | Spring Boot `4.0.6` (`spring-boot-starter-parent`) |
| Java declarado | `17` |
| Artefacto | `restacontrol` `0.0.1-SNAPSHOT` |
| Web y vistas | `spring-boot-starter-webmvc`, `spring-boot-starter-thymeleaf` |
| Persistencia | `spring-boot-starter-data-jpa`, `spring-boot-starter-jdbc`, driver `postgresql` |
| Seguridad | `spring-boot-starter-security`, `spring-security-crypto` |
| Auditoría | `spring-boot-starter-aspectj` |
| Reportes | `poi-ooxml` `5.4.1`, `commons-io` `2.18.0` |
| Desarrollo | `spring-boot-devtools` |

## 2. Configuración de la aplicación (`application.properties`)

| Propiedad | Valor | Comentario |
| --- | --- | --- |
| `server.port` | `7070` | Puerto de la aplicación |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5433/restaurante` | PostgreSQL local, puerto 5433 |
| `spring.datasource.username` | `postgres` | |
| `spring.sql.init.mode` | `never` | El esquema no lo crea Spring |
| `spring.jpa.hibernate.ddl-auto` | `none` | Hibernate no modifica el esquema |
| `spring.jpa.open-in-view` | `false` | Evita consultas fuera de la capa de servicio |

## 3. Base de datos

El esquema se gestiona de forma manual con los scripts de `database/`, en este orden:

1. `01_schema.sql`: extensión `uuid-ossp`, tablas, restricciones, índices, funciones, triggers y vistas.
2. `02_seed.sql`: catálogos y configuración inicial (categorías, platos, usuarios, mesas y productos).
3. `03_demo_data.sql`: datos de demostración (opcional).

Requisitos: base de datos vacía llamada `restaurante`, PostgreSQL con la extensión `uuid-ossp`
disponible y un usuario con permiso para crearla.

## 4. Observaciones

1. **Contraseña en texto plano.** `spring.datasource.password` está escrita directamente en
   `application.properties` y forma parte del repositorio. Se recomienda leerla de una variable de
   entorno (`${DB_PASSWORD}`) o de un archivo local que no se versione.
2. **Versión de Java.** El proyecto declara Java 17. Antes de compilar conviene confirmar que el JDK
   activo en cada equipo es compatible con esa versión.
3. **Recursos duplicados con el frontend.** `src/main/resources/static` (`app.js`, `auth.js`,
   `login.css`, `styles.css`) y páginas equivalentes a las de `templates/` (dashboard, clientes,
   categorías, platos, cocina) también existen en `frontend_RestaControl`. Conviene definir cuál es la copia vigente para no mantenerlas dos veces.
4. **Versión de PostgreSQL sin fijar.** El proyecto no indica la versión mínima de PostgreSQL requerida.
5. **Pruebas.** Existen pruebas en `src/test` para `ApiController` y `CobrarAtencionRequest`;
   el resto de módulos no tiene pruebas automatizadas.

## 5. Pendiente

- Levantar la base de datos local y ejecutar los tres scripts.
- Arrancar el backend con `./mvnw spring-boot:run` y verificar el acceso en el puerto 7070.
