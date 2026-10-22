# Revisión de Bloomi Bunny para Git

Fecha: 22 de octubre de 2026.

Bloomi Bunny tiene una base funcional para presentar como avance de tienda y administración de inventario. Todavía no es una tienda con pedidos y pagos completos. Esta revisión cubre el código propio, configuración, SQL, herramientas y recursos del proyecto; dependencias, compilados, perfiles de navegador y copias privadas se clasifican como artefactos, sin auditarlos como código propio.

## Comprobaciones

| Comprobación | Resultado y alcance |
| --- | --- |
| `mvn test` | 25 pruebas aprobadas: 11 de acceso y 14 del ciclo de productos. Usan mocks, sin consultar PostgreSQL. |
| `npm run build` | Aprobado. JavaScript principal de 833,96 kB; aviso por superar 500 kB. |
| TypeScript con `--noCheck false --incremental false` | Falla con tres TS2322 en `DashboardPage.tsx`, por inferencia de la lista de indicadores que mezcla textos e iconos. |
| Git del proyecto | Ningún archivo de esta carpeta estaba registrado al comenzar. El repositorio real está en la carpeta superior y contiene otros proyectos. |
| Selección para Git | 131 archivos candidatos después de las exclusiones; sin rutas privadas/generadas ni coincidencias con valores secretos de la configuración local. No sustituye una auditoría de seguridad completa. |
| Recursos binarios | 11 PNG públicos íntegros, rutas del manifiesto presentes y SHA256 idénticos a los 11 originales. Excel histórico íntegro como contenedor, con nueve hojas. |
| Navegador y base de datos | No se arrancaron servidores ni se ejecutaron pruebas funcionales en navegador en esta revisión. No se verificó ni modificó el inventario actual. |

## Qué se puede presentar como avance

| Área | Estado |
| --- | --- |
| Catálogo y ficha | Implementados, con fotos, precios COP, estados de disponibilidad y variantes por tono. |
| Carrito | Implementado en el navegador por visitante y cuenta, con límites de stock. No reserva unidades ni crea pedidos. |
| Acceso | Login, registro público de clientes, JWT y protección administrativa implementados. |
| Favoritos | API por usuario autenticado, persistencia PostgreSQL y controles en catálogo y detalle. |
| Inventario | Creación, edición, publicación, borrado lógico, movimientos y resumen global implementados; edición con versión. |
| Categorías, marcas y contactos | Listar, crear y eliminar desde la interfaz; la API también permite editar. Manejo de errores incompleto en pantalla. |
| Dashboard | Mezcla indicadores de API con gráficos y porcentajes fijos. Varios botones no ejecutan acciones. |
| Ventas, compras y gastos | Base de API; sin pantallas operativas. Falta probar integración y corregir riesgos de serialización. |
| Caja, reportes y configuración | Rutas a pantalla de Próximamente. |
| Nosotros y políticas | Páginas implementadas; textos declarados como borrador y datos comerciales por completar. No se evaluó vigencia normativa en esta revisión técnica. |

## Problemas y prioridades

1. **Proteger lo que se sube a Git.** El archivo local `application.properties` contiene configuración sensible. Queda excluido y se incorpora una plantilla por variables de entorno. También se excluyen Excel, memoria comercial, respaldos y SQL de ajustes históricos. Ninguno se borra.
2. **Restablecer comprobación de tipos.** `noCheck=true` permite que el build pase a pesar de los tres errores del dashboard. `CatalogPage.tsx` omite sus comprobaciones con `@ts-nocheck`. Corregir los tipos y retirar estas omisiones antes de usar el build como validación de TypeScript.
3. **Filtrar publicación en el backend.** `StoreController.list` reutiliza el listado administrativo, que excluye eliminados pero incluye deshabilitados. El frontend los oculta, pero la API pública entrega sus datos comerciales. Aplicar el filtro de activos antes de paginar. El detalle sí rechaza deshabilitados.
4. **Completar paginación administrativa.** `api.products` lee solo la primera página de hasta 100 SKU. Más productos quedan fuera de la tabla administrativa; el resumen de inventario sí considera todos los resultados. La mejora posterior de experiencia de compra hace que `api.storeProducts` consulte todas las páginas para búsqueda, catálogo y carrito.
5. **Revisar las respuestas de transacciones.** GET de ventas y compras devuelve entidades JPA con relaciones bidireccionales `Sale.items → SaleItem.sale` y `Purchase.items → PurchaseItem.purchase`, sin DTO ni control de ciclo JSON. Existe riesgo de recursión/errores de serialización al listar registros con ítems; confirmarlo con una prueba de integración y devolver DTO. POST sí usa DTO.
6. **Corregir indicadores antes de contabilidad real.** `grossProfit` resta gastos a ventas, pero no resta costo de mercancía vendida; `SaleItem` no conserva costo histórico. El dashboard usa cortes UTC, porcentajes/gráficos fijos, muestra ceros ante error y llama activos al total de productos no eliminados, incluidos deshabilitados.
7. **Completar controles administrativos.** Categorías/contactos no ofrecen edición en pantalla ni errores visibles para fallos de consulta o mutación; la creación no bloquea envíos mientras está pendiente. Dashboard, buscador global y pantalla de Próximamente incluyen controles sin acción.
8. **Hacer reproducible la base.** Los seis SQL no se aplican automáticamente ni conforman un esquema inicial completo. `ddl-auto=update` genera estructuras en desarrollo, pero no garantiza migraciones de restricciones o índices en bases existentes. No hay inventario de demostración sin datos privados ni pruebas con PostgreSQL.
9. **Revisar seguridad del navegador y accesibilidad.** El JWT queda en `localStorage`, por lo que un XSS podría leerlo; no implica que se haya encontrado un XSS. El carrito usa un diálogo simulado sin contención completa de foco ni bloqueo del fondo. La lectura de sesión decodifica JWT con `atob` directo en lugar de normalizar base64url y UTF-8.
10. **Optimizar después de corregir funcionamiento.** Separar cargas de administración y gráficos para reducir el bundle público. Revisar reglas CSS repetidas y selectores del layout que no tienen estilos propios. Completar fotos/descripciones faltantes usando referencias reales.

Las existencias exactas siguen presentes en el JSON público para limitar el carrito; la interfaz de detalle muestra solo Disponible o Agotado. Esta distinción respeta la decisión visual actual y no equivale a ocultar stock a quien inspecciona la API.

## Revisión por carpeta y archivo

Las tablas indican la función y el límite observado de cada archivo propio. Los recursos binarios se verifican por manifiesto, formato, tamaño y correspondencia, sin atribuirles comprobaciones visuales o contables.

### Raíz

| Archivo o carpeta | Evaluación |
| --- | --- |
| `.gitignore` | Se amplía para separar código, configuración privada, datos comerciales y archivos generados. |
| `AGENTS.md` | Instrucciones de trabajo válidas; conservar. La memoria referida se mantiene local. |
| `PROJECT_MEMORY.md` | Historial interno útil, con datos comerciales; conservar local y actualizar sin credenciales. |
| `Bloomi_Bunny(Agosto 5).xlsx` | Fuente histórica privada; no representa automáticamente el inventario actual. Conservar sin reimportar. |
| `Django/Proyecto/BloomiFront/src/contexts/AuthContext.tsx` | Vacío y fuera del frontend activo; no aporta funcionalidad. Se conserva, excluido de Git. |
| `.local/inventory-import/` | Respaldos y resultados históricos; privados, conservar. |
| `.local/store-layout/` | Scripts, capturas, evidencias y perfiles Chrome. Algunos contienen sesiones o supuestos antiguos; conservar sin publicar ni ejecutar masivamente. |
| `README.md` | Se añade guía de instalación, validación y límites del avance. |
| `docs/REVISION_PROYECTO.md` | Revisión técnica para acompañar el avance. |

### Configuración del frontend

| Archivo | Evaluación |
| --- | --- |
| `BloomiFront/package.json` | Dependencias y scripts dev/build/preview válidos. No hay scripts de pruebas ni lint. |
| `BloomiFront/package-lock.json` | Fija la resolución de dependencias; conservar para `npm ci`. No se ha realizado auditoría de vulnerabilidades. |
| `BloomiFront/tsconfig.json` | Configura strict pero `noCheck=true` desactiva la comprobación. Requiere corrección. |
| `BloomiFront/vite.config.ts` | Configuración React y puerto 5173. Sin partición del bundle ni proxy API. |
| `BloomiFront/.env.example` | Plantilla pública de URL API; sirve. Nunca colocar secretos en variables VITE. |
| `BloomiFront/index.html` | Entrada en español y metadatos básicos; título inicial orientado a Gestión pese a abrir la tienda. |
| `BloomiFront/tsconfig.tsbuildinfo` | Caché regenerable, excluida. |
| `BloomiFront/node_modules/` | Dependencias instaladas, regenerar con npm; excluidas. |
| `BloomiFront/dist/` | Compilación generada, válida para distribución con alojamiento configurado; excluida del código fuente. |

### Código del frontend

Todos los siguientes archivos están en `BloomiFront/src/`.

| Archivo | Evaluación |
| --- | --- |
| `main.tsx` | Monta React, router, Query, sesión, toasts y estilos. Sirve como entrada. |
| `App.tsx` | Rutas públicas y control ADMIN; varios módulos administrativos desembocan en placeholder. |
| `vite-env.d.ts` | Tipos de Vite; conservar. |
| `types/index.ts` | Contratos comunes de sesión, productos, resumen y contactos. Revisar opcionales/null con respuestas reales al reactivar tipos. |
| `contexts/AuthContext.tsx` | Sesión, login, signup, logout y limpieza de caché. JWT en almacenamiento local; decodificación y fallos de almacenamiento mejorables. |
| `hooks/useApi.ts` | Consultas, mutaciones e invalidaciones; refresco de inventario. No implementa paginación completa. |
| `services/http.ts` | Axios, Bearer, errores y cierre por 401. No coordina otros pestañas; el manejo de almacenamiento puede fallar si el navegador lo bloquea. |
| `services/api.ts` | Contratos HTTP usados. Catálogo público carga todas las páginas; listado administrativo limitado a 100. Ventas/compras no tienen cliente operativo propio. |
| `services/catalog.ts` | Normaliza nombres, asigna imágenes y agrupa variantes sin fusionar SKU. Oculta grupos sin stock en catálogo y conserva favoritos agotados. |
| `services/cart.ts` | Valida almacenamiento, duplicados y cantidades; limita stock al calcular líneas. No reserva stock ni confirma pedidos. |
| `layouts/AppLayout.tsx` | Menú y sesión administrativa. Buscador sin lógica y estado Todo al día fijo; revisar estilos de menú móvil/compacto. |
| `pages/StorePage.tsx` | Tienda, favoritos y carrito por cuenta. Agrupa SKU y usa datos comerciales; al fallar catálogo avisa que se pueden ver fotos pero no tiene catálogo alternativo de imágenes. |
| `pages/LoginPage.tsx` | Login y registro con estados y errores. Falta completar el flujo comercial de privacidad/consentimiento antes de operación pública. |
| `pages/InventoryPage.tsx` | Gestión real de productos, resumen, búsqueda y menús. Tabla limitada a la página de 100; resumen global. |
| `pages/DashboardPage.tsx` | Indicadores conectados mezclados con demostración; tres errores de tipos, errores de API ocultos y botones sin acción. |
| `pages/CatalogPage.tsx` | Alta/listado/borrado de catálogos y contactos. Omite tipos y no muestra errores de red/mutación; edición solo existe en API. |
| `pages/PlaceholderPage.tsx` | Marcador de módulos pendientes. El botón Explorar arquitectura no tiene acción. |
| `components/common/Ui.tsx` | Componentes de estado, tarjetas, búsqueda y moneda COP reutilizables. |
| `components/inventory/InventoryDialogs.tsx` | Diálogos de crear/editar/eliminar. Preserva relaciones, fecha, versión y grupo de tonos; no sube imágenes, guarda una ruta o URL. |
| `components/store/ProductDetails.tsx` | Ficha, selección de tonos, favorito, stock visual sin cifra exacta y zoom con diálogo nativo. Descripción/foto faltante muestra aviso. |
| `components/store/LegalPages.tsx` | Nosotros, pie, documentos y contacto. Oculta datos vacíos y señala borrador. |
| `data/legal.ts` | Datos comerciales públicos, fuentes y textos de trabajo; `finalized=false`. No convertir en políticas finales sin completar datos y revisar normas. |
| `data/product-images.json` | Manifiesto de 11 fotos; conservar junto a sus archivos públicos. |
| `styles/index.css` | Base visual y login. Muchas reglas comprimidas; algunas clases del panel no tienen definición propia. |
| `styles/store.css` | Tienda, carrito y responsive. Contiene también reglas de inventario y varias redefiniciones de tamaños; consolidar sin cambiar el diseño acordado. |
| `styles/inventory.css` | Menú y formularios de inventario; incorpora enlaces y selector de tonos de la tienda. Funciona por carga global, pero mezcla responsabilidades. |
| `styles/product-detail.css` | Ficha, zoom y diseño móvil; conserva selector antiguo de conteo de stock sin uso actual. |
| `styles/legal.css` | Documentos, contacto y pie responsive. Incluye clase de enlaces de registro que la pantalla de registro no usa. |

### Backend y configuración

| Archivo | Evaluación |
| --- | --- |
| `BloomiBack/pom.xml` | Compilación Java 25 y dependencias Spring/JPA/JWT/pruebas; pruebas pasan. Sin gestor automático de migraciones ni Maven Wrapper. |
| `BloomiBack/src/main/resources/application.properties` | Configuración real del equipo; conservar local y excluir. |
| `BloomiBack/src/main/resources/application.properties.example` | Nueva plantilla sin valores secretos para otra instalación. |
| `BloomiBack/target/` | Clases, JAR y resultados generados; excluir. Un JAR construido con la configuración local puede contener secretos. |

Los siguientes archivos Java están en `BloomiBack/src/main/java/com/bloomibunny/`.

| Archivo | Evaluación |
| --- | --- |
| `BloomiBackApplication.java` | Entrada Spring Boot; sirve. |
| `config/DataInitializer.java` | Crea admin si no existe y categorías si la tabla está vacía. No importa productos ni cambia contraseña de un admin existente. |
| `controller/AuthController.java` | Login/signup públicos; registro con rol protegido por seguridad. Signup fuerza CUSTOMER. |
| `controller/CatalogController.java` | CRUD de categorías, marcas y contactos ADMIN. Borrados físicos pueden entrar en conflicto con referencias y responden mediante el manejador global. |
| `controller/DashboardController.java` | Expone resumen ADMIN; exactitud depende del servicio. |
| `controller/FavoriteController.java` | Usa Principal, sin aceptar identidad ajena. Endpoints idempotentes para agregar/quitar. |
| `controller/ProductController.java` | CRUD, stock, publicación y resumen ADMIN. Parámetros de paginación/sort necesitan validación y límites inferiores. |
| `controller/StoreController.java` | DTO público evita costos/proveedor. Detalle valida publicación; listado aún deja pasar deshabilitados. |
| `controller/TransactionController.java` | API de ventas/compras/gastos ADMIN; listados de ventas/compras devuelven entidades con riesgo de ciclo JSON. |
| `security/SecurityConfig.java` | ADMIN en APIs administrativas; favoritos autenticados; resto denegado. CORS de un origen configurable. |
| `security/JwtAuthFilter.java` | Firma válida y usuario activo/rol actual desde BD. Evita conservar permisos antiguos. |
| `security/JwtService.java` | Genera/verifica JWT firmado y expiración. Depende de secreto seguro externo; conservar. |
| `service/AuthService.java` | Contrato login/registro. |
| `service/DashboardService.java` | Contrato de resumen. |
| `service/ProductService.java` | Contrato inventario, variantes y resumen. |
| `service/TransactionService.java` | Contrato ventas/compras. |
| `service/FavoriteService.java` | Lista publicados, persiste favoritos propios y valida usuario/producto al agregar. Falta prueba de integración PostgreSQL. |
| `service/impl/AuthServiceImpl.java` | Autenticación, unicidad y contraseña BCrypt; rol procede del endpoint que llama. |
| `service/impl/DashboardServiceImpl.java` | Resumen conectado, pero ganancia incorrecta para contabilidad y fechas UTC. Conteo no equivale a productos activos. |
| `service/impl/ProductServiceImpl.java` | Validación de código, borrado lógico, movimientos por diferencia y resumen completo. `expectedVersion` es opcional; el formulario sí lo envía. Ajuste de stock suma enteros sin control explícito de desbordamiento. |
| `service/impl/TransactionServiceImpl.java` | Transacciones y movimientos; no guarda costo histórico de venta ni implementa pedidos públicos. Compras reemplazan costo unitario, sin definir valoración histórica. Falta cobertura específica. |
| `exception/ApiExceptionHandler.java` | Respuestas 400/401/404/409 y errores de campos; mensajes de integridad genéricos pueden ser inexactos fuera de productos. |
| `exception/BusinessException.java` | Excepción de reglas de negocio; conservar. |
| `exception/ResourceNotFoundException.java` | Excepción de recurso ausente; conservar. |

### Entidades del backend

Todos en `BloomiBack/src/main/java/com/bloomibunny/entity/`.

| Archivo | Evaluación |
| --- | --- |
| `BaseEntity.java` | ID y fechas automáticas; base común. |
| `AppUser.java` | Usuario único, hash de contraseña, rol y activo; no devolver entidad como perfil público. |
| `Role.java` | ADMIN, CUSTOMER y EMPLOYEE heredado; solo ADMIN administra. |
| `NamedEntity.java` | Nombre único, descripción y estado para catálogos. |
| `Brand.java` | Catálogo de marcas. |
| `Category.java` | Catálogo de categorías. |
| `Contact.java` | Base de datos de contacto y notas; información administrativa. |
| `Customer.java` | Cliente/contacto y cumpleaños; separado de AppUser, sin vinculación automática al registro web. |
| `Supplier.java` | Proveedor de compras/productos. |
| `Product.java` | SKU, cantidades, importes, publicación, borrado lógico, versión y grupo. Base funcional del inventario. |
| `Favorite.java` | Relación única usuario/producto; preservar. |
| `InventoryMovement.java` | Movimiento y stock resultante; sin referencia estructurada a pedido/venta ni usuario responsable. |
| `MovementType.java` | Tipos de movimiento, algunos aún sin módulo operativo. |
| `PaymentMethod.java` | Métodos nominales para registro administrativo; no integra pasarela. |
| `Sale.java` | Cabecera de venta y sus ítems; relación bidireccional necesita DTO de lectura. |
| `SaleItem.java` | Producto, cantidad/precio/subtotal; falta costo histórico, referencia al padre genera ciclo al serializar entidad. |
| `Purchase.java` | Cabecera de compra con proveedor y sus ítems; mismo riesgo de serialización. |
| `PurchaseItem.java` | Cantidad/costo/subtotal de compra y referencia al padre. |
| `Expense.java` | Gasto por categoría/fecha; longitudes de entidad no están replicadas en validación del DTO. |

### DTO y repositorios del backend

Archivos bajo `BloomiBack/src/main/java/com/bloomibunny/`.

| Archivo | Evaluación |
| --- | --- |
| `dto/AuthDtos.java` | Peticiones y respuesta de autenticación; validación de usuario/contraseña. |
| `dto/CatalogDtos.java` | DTO de nombres/contactos; validación de correo y longitudes. |
| `dto/ProductDtos.java` | Petición/respuesta privada, cantidades no negativas, versión y tonos. No sustituye el DTO público. |
| `dto/InventorySummary.java` | Distingue valor de venta, costo confirmado y datos provisionales. |
| `dto/DashboardResponse.java` | Contrato de indicadores y movimientos; nombre grossProfit excede el cálculo implementado. |
| `dto/TransactionDtos.java` | Líneas y totales de venta/compra, gastos. Completar límites de texto y reglas contables. |
| `repository/AppUserRepository.java` | Busca usuario por nombre; usado por login/JWT. |
| `repository/BrandRepository.java` | CRUD de marcas; sirve. |
| `repository/CategoryRepository.java` | CRUD de categorías; sirve. |
| `repository/CustomerRepository.java` | CRUD de clientes/contactos; sirve. |
| `repository/SupplierRepository.java` | CRUD de proveedores; sirve. |
| `repository/ProductRepository.java` | Busca por nombre/código y excluye eliminados; faltaría consulta pública activa paginada. Métodos antiguos de stock no excluyen eliminados por nombre. |
| `repository/FavoriteRepository.java` | Consulta con fetch y ON CONFLICT PostgreSQL para alta idempotente; requiere probar contra PostgreSQL. |
| `repository/InventoryMovementRepository.java` | Diez movimientos recientes; no hay consulta paginada de historial expuesta. |
| `repository/SaleRepository.java` | Totales/consulta por fecha y CRUD; revisar límites temporales inclusivos y huso al corregir dashboard. |
| `repository/PurchaseRepository.java` | CRUD inicial de compras; sirve como base. |
| `repository/ExpenseRepository.java` | Suma por rango de fecha; sirve para gastos, sin reemplazar costo de ventas. |

### SQL y pruebas del backend

| Archivo | Evaluación |
| --- | --- |
| `BloomiBack/src/main/resources/db/migrations/001_customer_role.sql` | Ajusta restricción de roles sobre tablas existentes; manual. |
| `BloomiBack/src/main/resources/db/migrations/002_provisional_inventory.sql` | Agrega indicadores de costo confirmado/dato provisional; manual. |
| `BloomiBack/src/main/resources/db/migrations/003_deleted_products.sql` | Agrega borrado lógico; manual. |
| `BloomiBack/src/main/resources/db/migrations/004_product_version.sql` | Agrega versión para concurrencia; manual. |
| `BloomiBack/src/main/resources/db/migrations/005_favorites.sql` | Crea favoritos con FK y unicidad; depende de usuarios y productos previos. |
| `BloomiBack/src/main/resources/db/migrations/006_product_variants.sql` | Agrega grupo e índice de variantes; no agrupa los datos por sí solo. |
| `BloomiBack/src/test/java/com/bloomibunny/security/AccessControlTest.java` | 11 pruebas MockMvc: roles, registro, DTO público, favoritos y revocación de permisos actuales. |
| `BloomiBack/src/test/java/com/bloomibunny/service/ProductLifecycleTest.java` | 14 pruebas Mockito: producto, concurrencia, valoración y variantes. Los datos de costo desconocido son escenarios sintéticos, no lectura del inventario actual. |

### Herramientas y fotografías

| Archivo o carpeta | Evaluación |
| --- | --- |
| `tools/import_inventory.py` | Importador histórico: valida Excel y no sobrescribe coincidencias diferentes; genera respaldo antes de aplicar. Ruta PostgreSQL y lectura de configuración ligadas al equipo original. Falta manifiesto de dependencias Python. |
| `tools/add_provisional_products.sql` | Ajuste histórico con valores provisionales ya superados; conservar privado, no ejecutar al instalar. |
| `tools/confirm_kitty_skull_inventory.sql` | Corrección histórica con valores absolutos de stock; volver a ejecutarlo podría restablecer cantidades después de nuevas ventas. Conservar privado. |
| `tools/__pycache__/import_inventory.cpython-314.pyc` | Caché Python, excluir. |
| `Blommiimages/` | 11 originales; útiles como fuente visual, conservar. |
| `BloomiFront/public/products/` | 11 PNG usados por el manifiesto y la tienda; conservar. Copias de originales con nombres aptos para URL. |

El inventario exhaustivo de rutas y la correspondencia de cada fotografía quedan en los CSV privados de `.local/review-2026-10-22/`. Se conservan los originales y todas las copias previas.

## Preparación realizada para Git

Se añaden `.gitignore` ampliado, plantilla de configuración, README y esta revisión. No se modifican funcionalidades, inventario, dependencias ni proyectos vecinos. Se elimina un espacio al final de una línea en `InventoryPage.tsx` para superar la comprobación de formato de Git.

Por autorización posterior de la propietaria, `Proyecto/` tiene su propio `.git` con historial independiente, rama `main` y remoto [Zelko7u7/BlommiBunny](https://github.com/Zelko7u7/BlommiBunny). Se seleccionan los 131 archivos revisados, sin trasladar el historial del repositorio superior. La configuración, índice, referencias y logs del Git superior se comparan con una huella previa para comprobar que permanecen intactos.

## Mejora posterior de experiencia de compra

Se incorporan búsqueda sin distinguir tildes o mayúsculas, categorías, orden por precio y nombre, filtros conservados en URL, resultados vacíos con restablecer y errores con reintento. `CatalogFilters.tsx` contiene los controles; `services/catalog.ts` filtra grupos completos de tonalidades. El catálogo público consulta todas las páginas.

`CartDialog.tsx` aporta diálogo nativo con foco contenido, Escape, fondo bloqueado y regreso al botón de apertura. `ProductPhoto.tsx` maneja imágenes ausentes o fallidas. El menú público se mantiene visible en móvil y el registro agrega confirmación de contraseña y mejores estados de envío. Las fichas conservan las imágenes existentes y permiten consultar el producto por WhatsApp; las fotografías y descripciones faltantes requieren referencias reales.

Build frontend aprobado después de los cambios, con JavaScript principal de 841,25 kB. La comprobación explícita de tipos mantiene únicamente los tres errores previos del dashboard. Las pruebas CDP verifican filtros, conservación al volver/recargar, tonos y límites del carrito, teclado, formularios, favoritos agotados, 101 productos simulados, imagen fallida y recuperación de errores. Los tamaños 320, 390, 768 y 1440 px no desbordan horizontalmente. El inventario real se comparó antes/después y permanece intacto; los escenarios de registro y favoritos usaron respuestas simuladas.

## Actualización posterior: dashboard con datos reales

El dashboard reemplaza sus series, ranking y porcentajes de muestra por consultas de ventas confirmadas. Calcula costos desde los snapshots de cada línea y margen de productos después del descuento; deja costo completo y margen pendientes cuando falta algún costo histórico. Excluye anulaciones de ingresos, gráficos y ranking, y usa `America/Bogota` para días y meses. El detalle del alcance está en [DASHBOARD.md](DASHBOARD.md).

Las alertas abren filtros reales de inventario por existencias o código; sus resúmenes consideran todos los resultados coincidentes. El ranking y los indicadores abren el historial con fechas/estado, y Registrar venta abre el formulario administrativo. Carga, ausencia de ventas y error se presentan como estados distintos.

Se eliminaron los errores previos del dashboard y `noCheck=true`; el build ya comprueba tipos. La exclusión local `@ts-nocheck` de `CatalogPage.tsx` sigue pendiente. Pasan 49 pruebas backend al habilitar PostgreSQL aislado, incluidas diez nuevas del dashboard; quince de integración se omiten sin sus variables. La API real y las alertas se verificaron por lectura, y los escenarios visuales con respuestas simuladas pasaron a 320/390/768/1440 px. Productos, ventas, líneas y movimientos reales permanecen idénticos. El aviso por tamaño del bundle y los demás pendientes históricos no resueltos siguen vigentes.
