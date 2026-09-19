# Revisión inicial de Clientes y Usuarios

Inventario de los módulos de Clientes y Usuarios del backend, tomado de `master`.

## 1. Clientes

| Elemento | Detalle |
| --- | --- |
| Controlador | `ClienteController` (`/api/clientes`) |
| Servicios | `ClienteService`, `ClienteCrudService` |
| Repositorio | `ClienteRepository` |
| Entidad | `Cliente` |
| DTO | `dto/cliente` (crear, actualizar, estado y respuesta) |

Endpoints: listar, buscar por documento, consultar por id, crear, actualizar, cambiar estado y eliminar.

## 2. Usuarios

| Elemento | Detalle |
| --- | --- |
| Controlador | `UsuarioController` (`/api/usuarios`) |
| Servicios | `UsuarioCrudService`, `UsuarioUserDetailsService` |
| Repositorio | `UsuarioRepository` |
| Entidad | `Usuario` |
| DTO | `dto/usuario` (crear, actualizar, estado, contraseña y respuesta) |

Endpoints: listar, consultar por id, crear, actualizar, cambiar estado, cambiar contraseña y eliminar.

## 3. Conclusión de la revisión

Pendiente de revisar.
