# Bloomi Bunny

Tienda virtual de maquillaje y cuidado de belleza con administración de inventario. Incluye catálogo con búsqueda, filtros y orden por precio, fichas por tonalidad, carrito local, cuentas, favoritos, inventario, ventas administrativas, compras recibidas y gastos. Los pedidos y pagos en línea siguen pendientes.

Las fichas incorporan galerías ordenadas por producto/tono, miniaturas, flechas, ampliación y descripciones oficiales persistidas en PostgreSQL. Inventario permite editar las fotografías secundarias. Alcance, productos actualizados, excepciones y procedimiento seguro de importación en [galerías de productos](docs/GALERIAS_PRODUCTOS.md).

## Estructura

| Carpeta | Contenido |
| --- | --- |
| `BloomiFront/` | React 19, TypeScript, Vite y TanStack Query. |
| `BloomiBack/` | Java 25, Spring Boot 4, Spring Security, JPA y PostgreSQL. |
| `Blommiimages/` | Fotografías originales de productos. Las copias usadas por la web están en `BloomiFront/public/products/`. |
| `tools/` | Importador histórico del inventario. Su uso es independiente del arranque de la aplicación. |
| `docs/` | Revisión técnica y estado del avance. |

## Ejecutar en desarrollo

Requisitos: Java 25, Maven, Node compatible con Vite 7 (`^20.19.0` o `>=22.12.0`), npm y PostgreSQL (la instalación local usa la versión 18).

1. Crear una base PostgreSQL y un usuario con permisos sobre esa base.
2. En una instalación nueva, copiar `BloomiBack/src/main/resources/application.properties.example` a `application.properties` en la misma carpeta. Si ya existe la configuración local, conservarla.
3. Configurar `DB_PASSWORD`, `JWT_SECRET` y `ADMIN_PASSWORD` como variables de entorno del proceso que ejecuta Maven. La clave JWT debe tener al menos 32 bytes y ser aleatoria. Opcionalmente configurar `DB_URL`, `DB_USERNAME`, `ADMIN_USERNAME` y `CORS_ORIGIN`. No guardar secretos en Git. El backend no carga archivos `.env` automáticamente.
4. Desde `BloomiBack`, ejecutar `mvn spring-boot:run`.
5. Desde `BloomiFront`, ejecutar `npm ci`. Para cambiar la URL de la API, copiar `.env.example` a `.env` y ajustar `VITE_API_URL`; esta variable es pública en el navegador.
6. Desde `BloomiFront`, ejecutar `npm run dev -- --host 127.0.0.1 --port 5173 --strictPort` y abrir `http://localhost:5173`. El backend escucha en `http://localhost:8080`.

Al primer arranque se crean la cuenta administrativa configurada y categorías iniciales. El registro público crea clientes. El inventario comercial no se distribuye con el repositorio: una base nueva empieza sin productos.

Hibernate usa `ddl-auto=update` para desarrollo. Los SQL de `BloomiBack/src/main/resources/db/migrations/` son manuales; no hay Flyway ni Liquibase. En bases existentes hay que revisar cuáles se aplicaron. No ejecutar todos ciegamente ni usar esta configuración como estrategia de migraciones de producción.

El importador histórico requiere Python, `openpyxl`, el Excel privado y, para aplicar cambios, las herramientas PostgreSQL 18 en la ruta configurada en el script. No forma parte de la instalación normal. **No ejecutarlo contra el inventario actual para reconstruir datos**: los registros de la tienda evolucionaron después de ese Excel.

## Validación del avance

```powershell
# Desde BloomiBack
mvn test

# Desde BloomiFront
npm run build
.\node_modules\.bin\tsc.cmd --noEmit --noCheck false --incremental false
```

Actualización del 22 de octubre de 2026: build frontend aprobado con comprobación de tipos activada, sin exclusiones `@ts-nocheck`. Las pantallas administrativas se cargan bajo demanda: el JavaScript inicial ronda 421 kB y el gráfico se descarga al abrir Dashboard. La compilación ya no muestra el aviso de tamaño. Ver [revisión técnica](docs/REVISION_PROYECTO.md).

El backend cuenta con 77 pruebas aprobadas: 20 de acceso/validación, 17 de productos, 5 unitarias del dashboard y 35 de integración PostgreSQL de ventas, dashboard, compras, gastos y revisión general. Las 35 de integración se omiten en el `mvn test` normal y exigen un esquema aislado de pruebas; ver [ventas administrativas](docs/VENTAS_ADMIN.md), [dashboard](docs/DASHBOARD.md) y [compras y gastos](docs/COMPRAS_GASTOS.md).

Ventas permite registrar operaciones externas, consultar historial y detalle y anular devolviendo existencias. El dashboard usa ventas confirmadas, costos históricos, margen de productos, gráficos reales y alertas con filtros de inventario. Trabaja con fechas de Colombia y señala costos desconocidos. Compras registra recepciones de proveedores y suma inventario; Gastos conserva conceptos, importes y referencias de comprobantes. Ambos tienen historial, detalle, filtros, totales y anulación auditada. Caja/movimientos de dinero, reportes y configuración siguen pendientes. Las políticas están en borrador.

Categorías, marcas, proveedores y clientes permiten crear, consultar el detalle y editar todos sus campos, además de eliminar con confirmación. Los directorios muestran descripción o teléfono, correo, dirección y notas completos; clientes incluye cumpleaños. Incorporan estado activo/inactivo, búsqueda, orden y páginas de 12 registros con filtros en URL. Un registro relacionado que la API impide eliminar puede conservarse como inactivo.

La API pública excluye productos deshabilitados, eliminados y provisionales. Inventario, selectores de ventas/compras y catálogo consultan todas sus páginas; la ordenación por ID evita empates inestables entre productos con el mismo nombre. El formulario de inventario permite seleccionar marca, proveedor y fecha de ingreso, conserva centavos y exige versión al editar. El buscador del panel abre el inventario filtrado. Los módulos aplazados se ocultan del menú.

## Repositorio Git

Repositorio del proyecto: [Zelko7u7/BlommiBunny](https://github.com/Zelko7u7/BlommiBunny). Frontend y backend se versionan juntos en la rama `main`.

El `.gitignore` conserva fuera de Git las credenciales locales, `.local/`, el Excel comercial, la memoria privada, los scripts de corrección histórica de inventario, las dependencias y compilados. Los archivos siguen en el equipo. La estructura duplicada `Django/` está excluida porque solo contiene un archivo vacío y no es la aplicación activa.

En el equipo de origen `Proyecto` tiene su propio repositorio `.git`, independiente del repositorio de la carpeta superior. Ejecutar los comandos desde `Proyecto` y comprobar que la raíz corresponde a esta carpeta:

```powershell
git rev-parse --show-toplevel
git remote get-url origin
git status --short
```

La raíz debe terminar en `Django/Proyecto` y `origin` debe apuntar a `Zelko7u7/BlommiBunny`. El historial de los proyectos vecinos no forma parte de este repositorio.

Los archivos privados de memoria y respaldo corresponden al trabajo local y no son requisitos para ejecutar un clon nuevo.
