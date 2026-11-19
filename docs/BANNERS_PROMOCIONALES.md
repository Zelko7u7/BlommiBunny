# Banners promocionales administrables

Implementado y validado en local el 18 de noviembre de 2026. Acceso: Administración → Banners promocionales, ruta `/admin/banners`. La tienda consulta PostgreSQL mediante la API pública; no existe respaldo automático a los antiguos banners estáticos.

## Cómo administrar las promociones

1. Pulsa **Nuevo banner** e indica un nombre interno.
2. Selecciona una imagen de escritorio desde tu dispositivo. Como referencia, utiliza 1800 × 310 px. Opcionalmente carga un arte diferente para móvil, diseñado con textos legibles en pantallas pequeñas.
3. Espera a que terminen las subidas. Se admiten JPEG, PNG y WebP sin animación, hasta 5 MB por archivo, 20 megapíxeles y 10000 px por lado. Se muestra previsualización, progreso y error con opción de reintento o descarte. Guardar/cerrar queda bloqueado durante una subida; un fallo pendiente debe resolverse o descartarse antes de guardar.
4. Escribe una descripción accesible de la promoción. El nombre interno no se muestra al comprador.
5. Elige el destino: catálogo completo, categoría, producto o sección. Los selectores muestran registros reales y permiten buscar por nombre. Las secciones disponibles son Nosotros y Favoritos; esta última conserva su comportamiento habitual de acceso. No hay subcategorías en el modelo actual, por lo que no se ofrece una opción ficticia.
6. Revisa la vista previa **Escritorio/Móvil**. El enlace público cubrirá toda la imagen; la vista previa administrativa no navega fuera del formulario.
7. Guarda inactivo para preparar una promoción o marca **Activo en la tienda** para publicarla. Las fechas opcionales se introducen en hora de Colombia. Sin fechas permanece publicada hasta desactivarla. El inicio es inclusivo y la finalización exclusiva.

El apartado opcional de texto sirve para artes sin título incorporado. Déjalo vacío si el texto ya está dibujado en la imagen. Los dos banners migrados conservan aquí sus títulos y descripciones, porque los originales tienen espacio vacío a la izquierda. No se agrega un botón HTML duplicado sobre el arte.

La lista permite editar, reemplazar fotos, activar/desactivar, subir/bajar posiciones y eliminar con confirmación. El campo de orden también permite definir una prioridad numérica; los empates se ordenan por ID. Las flechas de la lista normalizan las posiciones y funcionan con teclado. El máximo operativo es de 50 banners.

Estados: **Publicado**, **Inactivo**, **Programado**, **Finalizado** y **Destino no disponible**. Un producto oculto, eliminado o provisional, o una categoría inactiva/eliminada, deja de ser un destino público válido. El banner conserva sus datos para que la administradora lo corrija. Un producto agotado puede seguir siendo destino, mostrando su ficha con las restricciones de compra existentes.

No se borran archivos de Cloudinary al reemplazar fotos, cancelar formularios o eliminar banners: podrían seguir vinculados en otros lugares. No hay una limpieza automática de archivos sin uso. Los errores de red al guardar se pueden reintentar sin duplicar una creación; si se cambia el contenido de un intento ya guardado, se pide revisar la lista. Las versiones evitan sobrescribir cambios de otra sesión.

## Navegación y comportamiento público

El backend construye rutas internas permitidas: `/#catalogo`, `/?categoryId=ID#catalogo`, `/producto/ID`, `/nosotros` y `/favoritos`. No se admiten URLs externas ni rutas introducidas manualmente. El enlace a categoría conserva su ID al renombrarla. La tienda consulta `/api/store/categories`, resuelve el nombre actual y utiliza el filtro existente, reflejado en selector, resumen y URL. Se eliminan búsquedas/categorías incompatibles y se conserva un orden por precio válido. Los enlaces antiguos con `category=nombre` siguen siendo compatibles.

Toda la superficie del banner es un único enlace accesible, con foco visible y cursor de enlace. Flechas e indicadores son controles hermanos, sin botones o enlaces anidados. Un gesto horizontal cambia de diapositiva y no abre la promoción; Enter sigue funcionando después de un gesto. Hay transición de 450 ms y avance cada 6 segundos. El foco, cursor, toque, pestaña oculta y movimiento reducido pausan el avance; al terminar la interacción comienza un intervalo completo nuevo.

En escritorio se conserva la altura anterior de 310 px y las diapositivas ocupan todo el ancho del carrusel, sin el límite heredado de 700 px. La imagen se muestra completa con `contain`. En móvil el carrusel mantiene un mínimo de 280 px y usa el arte específico cuando existe; de lo contrario se muestra completa la imagen de escritorio. El espacio de las imágenes se reserva con dimensiones y proporciones y se mantiene estable entre diapositivas. Con artes de proporciones diferentes puede haber bandas libres. Se ofrecen tamaños responsive de Cloudinary con `c_limit`, `q_auto` y `f_auto`, sin recortar ni ampliar por encima del original. Si falla la variante optimizada se prueba el original; si también falla, se retira la diapositiva. Un fallo de banners nunca bloquea el catálogo.

La API no permite caché de la lista pública. La tienda vuelve a consultar cada 15 segundos y al recuperar foco; los cambios hechos desde administración invalidan inmediatamente la consulta de esa sesión. La finalización de un banner ya recibido también se controla con un temporizador en el navegador. Si todos están inactivos, vencidos, no disponibles o eliminados, no aparece ningún carrusel ni regresan configuraciones estáticas.

## API y almacenamiento

| Método y ruta | Acceso y función |
| --- | --- |
| GET `/api/banners` | Público; solo activos, vigentes y con destino disponible, ordenados |
| GET `/api/store/categories` | Público; IDs y nombres actuales de categorías activas |
| GET `/api/admin/banners` | ADMIN; lista completa, estados y versiones |
| GET `/api/admin/banners/targets` | ADMIN; categorías, productos publicados y secciones permitidas |
| POST `/api/admin/banners` | ADMIN; creación con UUID de solicitud para reintentos |
| PUT `/api/admin/banners/{id}` | ADMIN; edición con versión esperada |
| PATCH `/api/admin/banners/{id}/status` | ADMIN; activación/desactivación con versión |
| PATCH `/api/admin/banners/reorder` | ADMIN; lista completa de IDs y versiones, operación atómica |
| DELETE `/api/admin/banners/{id}?expectedVersion=…` | ADMIN; elimina únicamente el banner |
| POST `/api/admin/banners/images` | ADMIN; multipart `file`, devuelve URL HTTPS, public_id y dimensiones |

La subida reutiliza firma y validación del servidor, con recursos en `bloomi-bunny/banners`. Las claves se leen exclusivamente en el backend desde configuración privada o entorno. Se almacenan URL y public_id de escritorio y móvil, dimensiones, destino por relación estable, fechas, orden, estado y versión. No se aceptan direcciones de otros proveedores ni identificadores ajenos al espacio de banners de la cuenta configurada. El almacenamiento local de desarrollo de productos no se utiliza para banners.

## Migración y carga inicial

`013_promotional_banners.sql` es aditiva: crea `promotional_banner`, su índice y `promotional_banner_seed`. Las relaciones a categoría/producto usan `ON DELETE SET NULL`, por lo que un destino eliminado se vuelve no disponible. No altera tablas de ventas, productos, stock, cuentas o precios. Hibernate no ejecuta automáticamente este archivo.

En este equipo ya se aplicó, con backend detenido y respaldo PostgreSQL completo comprobado mediante `pg_restore --list`. Se ejecutó dos veces para comprobar idempotencia. Las 18 tablas anteriores permanecieron idénticas por huella antes y después.

`tools/migrate_promotional_banners.py` sube y verifica los originales de bienvenida y labios y guarda sus configuraciones una sola vez. La instalación local verificó previamente la categoría real **Labios, ID 14** y la asoció mediante FK. Este ID es un dato de esta instalación, no debe copiarse a otra base sin comprobarla. Los originales permanecen intactos. El marcador independiente `initial-static-v1` impide que una nueva ejecución restaure banners que la administradora haya eliminado o desactivado.

Para otra instalación, después de revisar y respaldar su base, aplicar únicamente la migración que falte. Verificar la categoría real y ejecutar el auxiliar con `--lips-category-id` correspondiente. El auxiliar requiere la configuración privada local de PostgreSQL/Cloudinary y las herramientas PostgreSQL; no incluye credenciales ni configura una cuenta remota. Planes, respaldos, subidas y evidencias locales quedan en `.local/`, excluido de Git.

## Preparación para alojamiento

No se realizó despliegue ni se modificaron servicios remotos. Para Heroku se debe publicar el backend Maven de `BloomiBack`, seleccionar Java 25 y mantener `ddl-auto=validate` después de aplicar la migración a la base alojada respaldada. El comando Java debe recibir `--server.port=$PORT` y cargar una configuración de producción basada en la plantilla del repositorio, sin subir el archivo real del equipo. El monorepositorio requiere preparar el backend como raíz de compilación o configurar su construcción antes del despliegue; aún no hay una aplicación Heroku de destino seleccionada.

Variables privadas backend: `CLOUDINARY_ENABLED=true`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`, `DB_URL` en formato JDBC, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME` y `ADMIN_PASSWORD`. `CORS_ORIGIN` debe ser el origen HTTPS exacto del frontend y `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`. La URL PostgreSQL de Heroku debe adaptarse al formato JDBC/configuración que utiliza este proyecto; no copiar literalmente una URL `postgres://` en `DB_URL`. Las imágenes son persistentes en Cloudinary, sin depender del disco del dyno. La cuenta administradora original se conserva al migrar los datos; el bootstrap de administradora única sigue vigente.

El frontend todavía no tiene un proveedor elegido. Genera `BloomiFront/dist` con `npm run build` y puede alojarse como sitio estático con fallback SPA a `index.html`. Al compilar se necesita `VITE_API_URL=https://BACKEND/api` y la configuración pública habitual de WhatsApp. Nunca colocar API Secret ni contraseñas en variables `VITE_*`. Cualquier hosting elegido debe permitir rutas React, HTTPS, acceso a Cloudinary y la conexión CORS a la API. No usar Vite dev/preview como servidor de producción.

Fuentes oficiales: [Heroku Java](https://devcenter.heroku.com/articles/java-support), [Spring Boot en Heroku](https://devcenter.heroku.com/articles/deploying-spring-boot-apps-to-heroku), [publicación estática Vite](https://vite.dev/guide/static-deploy.html), [imágenes responsive Cloudinary](https://cloudinary.com/documentation/responsive_html).

## Validaciones realizadas

- 179 pruebas backend aprobadas sin fallos, errores ni omisiones; 94 integraciones PostgreSQL en esquema exclusivo, incluidas 15 nuevas de banners. Las pruebas de permisos cubren visitante, CUSTOMER, EMPLOYEE y ADMIN.
- 44 pruebas frontend aprobadas y build TypeScript/Vite final aprobado. Sin dependencias nuevas.
- Navegador real con subidas firmadas de escritorio y móvil: crear inactivo, previsualizar, activar, editar destino, reordenar, desactivar/reactivar y eliminar con confirmación. El banner temporal se eliminó y los dos originales quedaron activos en su orden inicial.
- Categoría aplicada desde toda la imagen, reflejada en URL/selector/resumen y conservada al recargar; cambio posterior a destino de producto comprobado.
- 320/375/430/768/1440/1800 px, sin desbordamiento; `contain`, fuente móvil específica, flechas independientes, toque sin navegación accidental, foco, Enter después de swipe, movimiento reducido, avance de seis segundos y pausa por foco.
- Todos inactivos sin respaldo estático, API de banners con error sin bloquear catálogo, imágenes optimizadas fallidas con recuperación al original y todas las imágenes fallidas con ocultación del carrusel. Cero excepciones JavaScript.
- Las 18 tablas comerciales anteriores permanecen iguales. Solo hay dos banners iniciales activos al terminar; no se crearon productos, ventas, favoritos ni movimientos de prueba. Archivos de pruebas subidos a Cloudinary se conservaron sin borrado automático.

## Archivos del cambio

Nuevos backend:

- `controller/BannerController.java`, `controller/BannerImageController.java`, `controller/StoreCategoryController.java`.
- `dto/BannerDtos.java`, `entity/PromotionalBanner.java`, `exception/BannerConflictException.java`.
- `repository/PromotionalBannerRepository.java`, `service/BannerService.java`.
- `resources/db/migrations/013_promotional_banners.sql`.
- Pruebas `security/BannerAccessTest.java` y `service/BannerIntegrationTest.java`.

Modificados backend: `exception/ApiExceptionHandler.java`, `security/SecurityConfig.java`, `service/CloudinaryImageStorage.java` y `service/ProductImageStorage.java`. Las rutas Java anteriores son relativas a `BloomiBack/src/main/java/com/bloomibunny/`; pruebas bajo `src/test/java/com/bloomibunny/` y recursos bajo `src/main/`.

Nuevos frontend: `src/pages/BannersPage.tsx`, `src/components/banners/BannerImageField.tsx`, `src/components/store/BannerArtwork.tsx`, `src/services/banners.ts`, `src/services/bannerMedia.ts`, `src/styles/banners.css` y `tests/banner-media.test.mjs`.

Modificados frontend: `src/App.tsx`, `src/layouts/AppLayout.tsx`, `src/pages/StorePage.tsx`, `src/hooks/useApi.ts`, `src/services/api.ts`, `src/services/heroNavigation.ts`, `src/components/store/BannerCarousel.tsx`, `src/components/store/StoreHero.tsx`, `src/styles/store-hero.css` y `tests/hero-navigation.test.mjs`. Retirado `src/data/hero-banners.ts`; sus imágenes originales no se eliminaron.

Auxiliar nuevo: `tools/migrate_promotional_banners.py`. Documentación: esta guía, README, revisión técnica, guía de Cloudinary y memoria privada de continuidad. Evidencias y capturas: `.local/banners-dev-2026-11-18/`; comprobantes de los dos originales: `.local/banner-seed/`.
