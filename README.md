# Bloomi Bunny

Tienda virtual de maquillaje y cuidado de belleza con administración de inventario. Incluye catálogo con búsqueda, filtros y orden por precio, fichas por tonalidad, carrito local, solicitudes de compra por WhatsApp, cuentas, favoritos, inventario, ventas administrativas, compras recibidas y gastos. Los pedidos persistidos y pagos electrónicos siguen pendientes.

## Carrusel de banners

El Hero muestra Bienvenida y Labios con textos HTML, altura estable de 310 px en escritorio, desvanecido lavanda y versión móvil sin fotografías detrás del texto. Avanza cada 6 segundos, con flechas laterales, indicadores inferiores y gesto táctil. No muestra control de pausa/reanudación. El cursor, el foco y el gesto táctil pausan temporalmente; cuando termina la interacción y el foco/cursor salen del carrusel comienza un nuevo intervalo completo. La pestaña oculta y el movimiento reducido también detienen el cambio automático. La preferencia de movimiento reducido elimina la transición, conservando los controles manuales.

La configuración está en `BloomiFront/src/data/hero-banners.ts`. Para añadir un tercer banner:

1. Conservar el original en `BloomiBanners/` y copiar el WebP a `BloomiFront/public/banners/`, sin alterar la imagen.
2. Añadir un objeto con `id` único, `image` (ruta pública), `label`, `title`, `description`, `buttonText` y `buttonAction` al arreglo `heroBanners`.
3. Usar `buttonAction: { type: 'catalog' }` para ir al catálogo actual; para una categoría, usar `{ type: 'category', category: 'Nombre real', aliases: ['Alias opcional'], fallbackQuery: 'búsqueda alternativa' }`. No crear categorías en la base para un banner. El CTA de Labios utiliza la categoría real existente, limpia búsquedas incompatibles y conserva el orden seleccionado.

`BannerCarousel.tsx` genera controles, indicadores y navegación circular según el número de objetos, sin duplicar componentes. `StoreHero.tsx` conecta las acciones con los filtros de la tienda. Build con tipos, 38 pruebas frontend y comprobaciones en navegador a 375/768/1024/1440/1920 px aprobados; sin cambios de backend, productos o base de datos.

## Pedidos por WhatsApp desde el carrito

El botón **Realizar pedido por WhatsApp** prepara todos los artículos con sus SKU/tonos, cantidades, precios COP y subtotal para que el cliente los revise y envíe manualmente, sin registro obligatorio. Actualiza el catálogo al abrir el carrito y bloquea solicitudes incompletas o inválidas. Incluye copia completa y contacto manual si la ventana se bloquea o el mensaje excede el límite preventivo del enlace.

Configurar `VITE_WHATSAPP_NUMBER=573242213841` en `BloomiFront/.env.local` (número público oficial confirmado; también aparece en `.env.example`). Debe tener formato internacional sin `+`, espacios o guiones; sin una configuración válida la función queda deshabilitada. No vacía el carrito, reserva stock, crea ventas ni registra cobros. Configuración, variantes, límites y pruebas en [VENTAS_WHATSAPP.md](docs/VENTAS_WHATSAPP.md).

Validación de esta funcionalidad: build con tipos, 33 pruebas frontend, 52 backend sin base de datos y navegador responsive aprobados; 59 integraciones backend omitidas. No se modificaron backend, base de datos ni inventario comercial. Las aperturas de WhatsApp de las pruebas fueron simuladas, sin mensajes reales.

## Avance del 30 de octubre de 2026

Ventas permite elegir **Pago pendiente, Pago parcial o Pago completo** al registrar la venta, incluyendo el abono inicial y su medio cuando corresponda. También permite añadir pagos posteriores, consultar saldo y filtrar por estado. Toda venta nueva comienza sin autorización. Cuando el saldo llega a cero, la administradora puede autorizar expresamente el cobro con motivo; se conserva responsable, fecha e historial. Corregir un abono retira la autorización. El Dashboard distingue importe vendido, abonos por fecha del pago, cobros completos autorizados por fecha de autorización y saldo pendiente total. Guía y límites en [ventas administrativas](docs/VENTAS_ADMIN.md).

La migración aditiva `010_sale_payments.sql` está aplicada localmente con respaldo. No inventa pagos anteriores ni modifica stock; una instalación existente distinta debe revisar y respaldar su base antes de aplicarla. Validación posterior al selector de pago inicial: 111 pruebas backend aprobadas sin omisiones (59 PostgreSQL aisladas), build con tipos y navegador responsive aprobados. No se crearon ventas comerciales de prueba. Backend y frontend quedan activos. Costos por lote son el siguiente pendiente; cuotas programadas, caja, pedidos persistidos y pagos electrónicos públicos siguen sin implementarse.

Las fichas incorporan galerías ordenadas por producto/tono, miniaturas, flechas, ampliación y descripciones oficiales persistidas en PostgreSQL. Inventario permite editar las fotografías secundarias. Alcance, productos actualizados, excepciones y procedimiento seguro de importación en [galerías de productos](docs/GALERIAS_PRODUCTOS.md).

Las fichas también incluyen **Te puede interesar**, un carrusel de hasta seis productos disponibles relacionados por categoría, tipo, complementariedad y temática. Reutiliza el catálogo público y las tarjetas existentes, respeta tonos y límites del carrito y no requiere consultas individuales ni migraciones. Reglas y validación en [productos relacionados](docs/PRODUCTOS_RELACIONADOS.md).

## Estado al cierre del 28 de octubre de 2026

Identidad lavanda pastel unificada, galerías verticales con miniaturas circulares, 21 descripciones oficiales persistidas en 23 SKU y previsualización de la segunda fotografía al pasar el mouse. La distribución pública comparte un contenedor máximo de 1400 px y márgenes interiores fluidos; el catálogo adapta entre una y cinco columnas. Las fichas equilibran galería e información y conservan tonos, carrito y recomendaciones.

Los cinco avances del día están publicados en `main`, hasta `da7b119`: paleta lavanda, integración de fotografías/descripciones, recomendaciones, previsualización con mouse y distribución responsive. La [revisión técnica](docs/REVISION_PROYECTO.md) reúne el alcance, las comprobaciones y los pendientes.

Backend, frontend y auxiliares de desarrollo quedaron **apagados por petición de la propietaria** al terminar la jornada. Los navegadores aislados de prueba están cerrados. Para retomar, arrancar los servicios siguiendo las instrucciones inferiores; los PID históricos no acreditan que un proceso siga activo.

Las prioridades registradas en este cierre histórico eran ventas a cuotas/abonos y costos por lote. Los abonos y la autorización de cobros se completaron el 30 de octubre; continúan pendientes [costos variables por lote de recepción](docs/COMPRAS_GASTOS.md#pendiente-costos-variables-por-resurtido), pedido real desde carrito, pagos públicos, contenido de dos productos, confirmación de existencias por tono de Gloss Corazón y preparación del despliegue.

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
node --test tests/*.test.mjs
```

Actualización del 28 de octubre de 2026: build frontend aprobado con comprobación de tipos activada, sin exclusiones `@ts-nocheck`. Las pantallas administrativas se cargan bajo demanda: el JavaScript inicial ronda 430 kB y el gráfico se descarga al abrir Dashboard. La compilación no muestra el aviso de tamaño. Diez pruebas de recomendaciones aprobadas y comparación visual antes/después a 375, 430, 768, 1024, 1440 y 1920 px sin desbordamiento horizontal. Ver [revisión técnica](docs/REVISION_PROYECTO.md).

El conjunto completo del backend aprobó 89 pruebas durante la integración de galerías, incluidas 41 de integración PostgreSQL de productos, ventas, dashboard, compras, gastos y revisión general. Esas 41 se omiten en el `mvn test` normal y exigen un esquema aislado de pruebas; ver [galerías](docs/GALERIAS_PRODUCTOS.md), [ventas administrativas](docs/VENTAS_ADMIN.md), [dashboard](docs/DASHBOARD.md) y [compras y gastos](docs/COMPRAS_GASTOS.md). En el cierre de documentación no se reiniciaron servicios ni se repitieron pruebas ya aprobadas.

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
