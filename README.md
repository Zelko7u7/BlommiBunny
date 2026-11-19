# Bloomi Bunny

El carrusel se administra desde **Administración → Banners promocionales**: imágenes de escritorio/móvil en Cloudinary, destinos reales por ID, programación, activación, orden y vista previa. Toda la imagen es clicable; sin promociones vigentes se oculta sin recuperar banners estáticos. Los dos originales ya están migrados. Guía, migración 013, pruebas y preparación de alojamiento en [Banners promocionales](docs/BANNERS_PROMOCIONALES.md).

El acceso administrativo está limitado a una única cuenta ADMIN, incluso si está inactiva. El registro público crea exclusivamente clientes; `/api/auth/register` está bloqueado también para la administradora. El arranque solo crea la cuenta inicial si no existe ninguna cuenta y conserva los datos de la administradora existente aunque cambien las variables de bootstrap. Si quedan clientes sin administradora, se requiere recuperación manual, sin ascensos automáticos. La migración `012_single_admin.sql` añade la restricción PostgreSQL; en otras instalaciones debe aplicarse con respaldo y revisión de cuentas, sin eliminar duplicados automáticamente. Esta medida no incluye segundo factor ni limitación de intentos de acceso.

La administración permite subir fotos al crear/editar productos y gestionar portada/galería. Consulta [configuración de Cloudinary](docs/CLOUDINARY.md); requiere credenciales privadas en el backend y reinicio local. El despliegue en Heroku se deja para una etapa posterior.

Las 66 fotos existentes de productos también están migradas a Cloudinary: las portadas, galerías y opciones de colección utilizan sus enlaces HTTPS. Se verificaron todos los archivos remotos y las 23 fichas publicadas, conservando orden, tonos, precios y existencias. Los originales locales y el respaldo de la base se conservan.

## Aparición progresiva pública — 18 de noviembre de 2026

El catálogo, favoritos, recomendaciones y bloques informativos públicos incorporan Scroll Reveal: opacidad y desplazamiento de 20 px durante 550 ms, con escalonado de 70 ms en las tarjetas. El contenido inicialmente visible aparece inmediatamente; cada elemento se anima una vez por montaje y respeta movimiento reducido y navegación por teclado. Búsqueda, filtros y acciones conservados. Administración, header, splash, banners, carrito y modales sin este efecto.

Build con tipos, 38 pruebas frontend y navegador a 375/430/768/1024/1440/1920 px aprobados. Sin cambios de backend, base, fotografías ni inventario; servicios activos. Detalle de componentes, comportamiento y verificaciones en [Scroll Reveal público](docs/SCROLL_REVEAL.md).

## Logotipo oficial integrado — 18 de noviembre de 2026

La tienda usa el SVG oficial de `BloomiIconos_Logos/` en header, intro, pie, acceso/registro y administración. Un componente `BrandLogo` comparte el recurso y conserva sus colores, proporciones y transparencia; la B oficial sigue como favicon y se utiliza también al contraer la barra administrativa. Se retiran las letras y textos provisionales de esas ubicaciones, sin duplicar el nombre del dibujo.

Intro de unos 2,5 segundos conservada, con entrada suave del logo y las palabras Compra → Disfruta → Florece. Build con tipos, 38 pruebas frontend y revisión en navegador a 375/430/768/1024/1440/1920 px aprobados. Backend, base de datos y flujos comerciales intactos; servicios activos. Recursos, archivos y validaciones en [Identidad visual oficial](docs/IDENTIDAD_VISUAL.md). Esta carpeta oficial sustituye las referencias históricas a `BloomiIconosYLogos/`.

## Sesión activa y validada — 18 de noviembre de 2026

Backend y frontend **activos en local** por petición de la propietaria: [tienda](http://localhost:5173), [administración](http://localhost:5173/admin) y [catálogo API](http://localhost:8080/api/store/products). Este estado sustituye los apagados históricos inferiores. Usar localhost por CORS. Raíz de trabajo: `C:\Users\Zelko Jun\Desktop\UrkuForge\BloomiBunny`.

Revisión de fuentes, documentación, configuración y recursos propios: 342 archivos inventariados. Build con TypeScript y 38 pruebas frontend aprobados; Maven clean package con 126 pruebas backend aprobadas sin omisiones, incluidas 72 integraciones PostgreSQL aisladas. 133 comprobaciones HTTP aprobadas de rutas, componentes, recursos, autenticación, permisos y CORS. Las 18 tablas públicas permanecen idénticas antes/después; sin operaciones comerciales de prueba ni ajustes de inventario.

Backend ejecutado desde el JAR compilado con Hibernate `validate`, sin cambios automáticos de esquema ni cambios a la configuración persistida. Logs y evidencias privadas en `.local/session-2026-11-18/`; frontend vigente en frontend-authorized.log y procesos comprobados en verified-runtime.json. Sin cambios funcionales, commit ni push. Las prioridades de producción documentadas siguen pendientes; no se repitió la revisión visual responsive ni una auditoría exhaustiva de dependencias.

## Ubicación local vigente — 18 de noviembre de 2026

La carpeta se renombró de `Proyecto` a `BloomiBunny` por petición de la propietaria.

El proyecto se trasladó a `C:\Users\Zelko Jun\Desktop\UrkuForge\BloomiBunny`. Abrir esa carpeta como raíz de trabajo. La carpeta anterior en `Desktop\Django\Proyecto` se conserva como respaldo; evitar desarrollar en ambas copias. PostgreSQL permanece en su instalación existente.

Copia completa comprobada mediante SHA-256, incluidos Git, configuración privada y respaldos. Compilación frontend/backend, 38 pruebas frontend, 54 pruebas backend y 127 comprobaciones HTTP aprobadas; 72 integraciones que requieren un esquema aislado se omitieron. Las 18 tablas públicas permanecen idénticas. Los servicios temporales de validación quedaron apagados. Evidencias privadas en `.local/migration-2026-11-18/`.

Tienda virtual de maquillaje y cuidado de belleza con administración de inventario. Incluye catálogo con búsqueda, filtros y orden por precio, fichas por tonalidad, carrito local, solicitudes de compra por WhatsApp, cuentas, favoritos, inventario, ventas administrativas, compras recibidas y gastos. Los pedidos persistidos y pagos electrónicos siguen pendientes.

## Cierre del 12 de noviembre de 2026

Backend, frontend y auxiliares del proyecto **apagados por petición de la propietaria**. Verificados cero procesos del proyecto restantes y ninguna escucha en 5173/8080/9257/9258/9259. PostgreSQL compartido y aplicaciones ajenas conservados. Este cierre prevalece sobre los arranques históricos siguientes; no iniciar de nuevo hasta una nueva tarea.

Avances publicados en `main`: `669b091` (entrada suave del carrito y revisión de producción) y `0b20954` (favicon B transparente y recursos oficiales de marca). Hoy también se limpiaron los perfiles temporales de Chrome y el duplicado interno Django, recuperando unos 3,26 GiB y conservando respaldos/evidencias. El favicon elegido es **isotipoB-Transparente.svg**; se retiraron las copias públicas de las alternativas descartadas, manteniendo los originales en `BloomiIconosYLogos/` para futuros recursos de marca.

Validación del día: 126 pruebas backend, 38 frontend, build con TypeScript, 129 verificaciones HTTP y navegador responsive aprobados. Apertura del carrito de 200 ms y fondo de 180 ms, con movimiento reducido respetado. Sin operaciones comerciales de prueba; datos comparados intactos. Los pendientes de seguridad, despliegue, políticas y contenido están en [revisión técnica](docs/REVISION_PROYECTO.md). La próxima sesión debe empezar leyendo `PROJECT_MEMORY.md` y consultando la base antes de cualquier cambio de inventario.

La documentación de cierre y la retirada de copias públicas descartadas quedan como cambios locales posteriores a `0b20954`, sin nuevo commit/push solicitado para este cierre. Configuración real, memoria y evidencias privadas siguen excluidas de Git.

## Retoma y verificación del 12 de noviembre de 2026

El carrito incorpora una entrada suave de 200 ms con fundido y desplazamiento lateral corto; el fondo aparece gradualmente en 180 ms. Respeta movimiento reducido y conserva el foco, Escape y los controles. Build con tipos y comprobación en Chrome a 320/390/768/1440 px aprobados.

Backend y frontend **activos** para continuar el trabajo: [tienda](http://localhost:5173), [administración](http://localhost:5173/admin) y [catálogo API](http://localhost:8080/api/store/products). Usar localhost por CORS. Este arranque sustituye el apagado histórico inferior.

Comprobación actual: build con TypeScript, 38 pruebas frontend y 126 backend aprobadas sin omisiones, incluidas 72 integraciones PostgreSQL aisladas; 129 comprobaciones HTTP y navegador público a 320/390/768/1440 px. Datos comerciales intactos. La revisión de producción encontró avisos de seguridad en dos paquetes npm de ejecución; deben evaluarse y corregirse antes de publicar. Prioridades y límites en [revisión técnica](docs/REVISION_PROYECTO.md#revisión-y-prioridades-de-producción--12-de-noviembre-de-2026).

## Cierre del 3 de noviembre de 2026

Backend, frontend y auxiliares de pruebas quedan **apagados por petición de la propietaria**. Se verificó ausencia de procesos de desarrollo del proyecto y de escuchas en 5173/8080/9255/9256. PostgreSQL compartido y procesos ajenos conservados. Este estado prevalece sobre los arranques históricos descritos más abajo; iniciar de nuevo solo cuando una nueva tarea lo requiera.

El avance de hoy está publicado en `main`, commit `6aacb10`: intro tipográfica de 2,5 segundos por sesión, imágenes cuadradas en tarjetas con recorte centrado y carrito simplificado, conservando la vista del mensaje. Compilación con tipos y comprobaciones de navegador aprobadas. No se modificaron backend ni datos comerciales. En el cierre solo se actualiza documentación y se apagan procesos; no se repiten pruebas que requieran arrancar servicios.

Pendientes: artes definitivos de banners PC/móvil, contenido de Kit Pizza Planet y Rubor Lotso, reparto real por tonos de Gloss Corazón, condiciones comerciales/políticas y preparación del despliegue. Los lotes FIFO y los abonos/autorización ya están implementados.

## Avance de la jornada del 2 de noviembre de 2026

Restock con costos por lote y salida FIFO, eliminación de lotes agotados salvo el último vacío, valorización del inventario y costos exactos de ventas. En la tienda, búsqueda superior y filtros desplegables; se retiró el contador público de productos y resultados. Se conservan cantidades del carrito y controles administrativos. La arquitectura y validaciones están en [revisión técnica](docs/REVISION_PROYECTO.md).

Próximo ajuste visual acordado: artes independientes para PC y móvil y toda la imagen de cada banner clicable. Está pendiente de recibir los artes definitivos y de implementarse; los banners actuales conservan su contenido y enlaces HTML. También siguen pendientes contenido/tonos de productos y condiciones comerciales por confirmar.

## Búsqueda superior y filtros desplegables

La tienda incorpora una barra de búsqueda en la cabecera, visible en PC y móvil. En catálogo/favoritos filtra al escribir; Enter o la lupa llevan a los resultados. Desde una ficha o página informativa, enviar la búsqueda abre el catálogo. Categoría y orden están en el botón **Filtros**, cerrado inicialmente: se puede desplegar/ocultar sin perder selecciones. Muestra un indicador de filtros activos; **Limpiar filtros** conserva la búsqueda. Consulta, categoría y orden siguen en la URL. Conserva navegación, carrito y favoritos, con identidad lavanda.

Build con TypeScript, 38 pruebas frontend y comprobaciones de navegador entre 320 y 1920 px aprobados. Sin cambios de backend o datos comerciales.

## Retoma local del 2 de noviembre de 2026

Avance posterior: **restock por lotes FIFO implementado**. Inventario → menú del producto → Ver lotes / Reponer permite consultar y reponer un SKU existente con el costo de esa compra. Los lotes agotados se eliminan, conservando solo el último vacío; compras y costos de ventas permanecen como historial independiente. Stock valorizado por lotes y devolución al costo original. Migración 011 aplicada localmente con respaldo, sin cambiar las 84 unidades anteriores. Guía en [Compras y lotes](docs/COMPRAS_GASTOS.md). Validación completa: 126 pruebas backend, 38 frontend, build con tipos y navegador responsive aprobados.

Arquitectura revisada por carpetas y documentada en la sección inicial de [revisión técnica](docs/REVISION_PROYECTO.md). Backend y frontend quedaron activos en [API local](http://localhost:8080/api/store/products) y [tienda local](http://localhost:5173), con [panel administrativo](http://localhost:5173/admin). Usar `localhost` para mantener el origen CORS configurado. Este estado sustituye los apagados históricos descritos más abajo.

Validación de la revisión inicial: build con TypeScript y 38 pruebas frontend aprobados; 52 pruebas backend aprobadas y 59 integraciones omitidas; 126 comprobaciones HTTP satisfactorias. Durante esa revisión los datos comerciales se conservaron y no se modificaron funcionalidades ni se ejecutaron importaciones o SQL manuales. El avance posterior de lotes descrito arriba incorporó la migración 011 y su validación completa.

## Intro tipográfica de bienvenida

Desde el 3 de noviembre, la tienda muestra una intro de unos 2,5 segundos con Bloomi Bunny, Compra → Disfruta → Florece (una palabra a la vez) y A tu manera ♡. Fondo lavanda, tipografía Georgia y salida deslizándose hacia abajo; la tienda se renderiza detrás. `IntroSplash.tsx` y `intro-splash.css` controlan la secuencia y sus estilos, sin dependencias adicionales. Se monta desde `StorePage`, sin alterar el carrusel.

Se muestra una vez por sesión de pestaña mediante `sessionStorage`, clave `bloomi.intro.seen`, y se omite con `prefers-reduced-motion: reduce` o almacenamiento de sesión no disponible. Al terminar se desmonta y limpia temporizadores y listener. Para volver a verla en la misma pestaña, ejecutar `sessionStorage.removeItem('bloomi.intro.seen')` en la consola de la tienda y recargar. La reducción de movimiento debe estar desactivada para probarla.

Build con TypeScript y revisión en Chrome aprobados: secuencia exclusiva de palabras, altura fija, duración/desmontaje, tienda detrás, recarga/navegación, almacenamiento bloqueado, cambio de preferencia de movimiento y centrado a 320/390/768/1440 px. Sin escrituras API.

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
| `BloomiIconosYLogos/` | Originales oficiales de los iconos y logos de la tienda. El favicon elegido usa una copia idéntica de `isotipoB-Transparente.svg`, único favicon en `BloomiFront/public/brand/`. Los originales de marca se conservan intactos. Los próximos reemplazos de iconos provisionales tomarán sus recursos de esta carpeta. |
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

El `.gitignore` conserva fuera de Git las credenciales locales, `.local/`, el Excel comercial, la memoria privada, los scripts de corrección histórica de inventario, las dependencias y compilados. El 12 de noviembre se eliminaron por autorización los perfiles temporales de Chrome de pruebas y la estructura duplicada interna `Django/`, que solo contenía un archivo vacío. Respaldos, capturas, resultados y scripts privados se conservan. La exclusión `/Django/` queda como protección ante una recreación accidental del duplicado.

En el equipo de origen `Proyecto` tiene su propio repositorio `.git`, independiente del repositorio de la carpeta superior. Ejecutar los comandos desde `Proyecto` y comprobar que la raíz corresponde a esta carpeta:

```powershell
git rev-parse --show-toplevel
git remote get-url origin
git status --short
```

La raíz vigente es `C:/Users/Zelko Jun/Desktop/UrkuForge/BloomiBunny` y `origin` debe apuntar a `Zelko7u7/BlommiBunny`. El historial de los proyectos vecinos no forma parte de este repositorio.

Los archivos privados de memoria y respaldo corresponden al trabajo local y no son requisitos para ejecutar un clon nuevo.
