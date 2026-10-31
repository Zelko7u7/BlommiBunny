# Revisión técnica y estado de Bloomi Bunny

## Cierre de implementación del 30 de octubre de 2026

Este resumen prevalece sobre los estados históricos siguientes. Quedan implementadas las ventas con pagos pendientes, parciales y completos, abonos y autorización auditada de cobros; las solicitudes manuales por WhatsApp desde el carrito; y el carrusel responsive de Bienvenida/Labios con flechas laterales e indicadores, sin control visible de pausa. Se retiró el bloque de presentación/redes duplicado bajo el catálogo, conservando el pie y la página Nosotros.

El carrusel mantiene textos y enlaces HTML. Hacer toda la imagen clicable se conversó como alternativa para los banners definitivos, pero no se implementó. Las imágenes actuales son provisionales; cambiar a artes definitivos y sus destinos requiere una decisión posterior. La guía para añadir banners está en el README.

Validaciones acumuladas de esta sesión: 111 pruebas backend aprobadas en la ejecución completa de ventas, incluidas 59 integraciones PostgreSQL aisladas; 38 pruebas frontend y compilación con TypeScript aprobadas. El carrusel se comprobó en navegador a 375/768/1024/1440/1920 px, incluyendo flechas, indicadores, gesto táctil, foco, rotación, visibilidad de pestaña y movimiento reducido. Las pruebas de WhatsApp no enviaron mensajes reales. La apertura de WhatsApp no modifica inventario ni registra ventas; estas se confirman manualmente desde administración.

Se guardan los cambios en commits por funcionalidad y documentación. La memoria comercial, configuración real, evidencias y respaldos permanecen privados y excluidos de Git. Backend y frontend continúan activos. Próximos pendientes: banners definitivos, costos por lote con método por acordar y contenido/tonos aún sin confirmar; los pedidos persistidos y pagos electrónicos siguen fuera de esta etapa.

## Solicitudes por WhatsApp desde el carrito

Actualización posterior del 30 de octubre de 2026: el carrito prepara una solicitud completa por WhatsApp con precios COP, cantidades y tonos del SKU, previa actualización y validación del catálogo. El cliente decide enviar el mensaje. No vacía el carrito ni crea pedidos persistidos, pagos, ventas, reservas o movimientos. Destino por variable pública validada; copia/contacto manual para ventanas bloqueadas, portapapeles no disponible o mensaje largo, sin truncar artículos. No se modifica backend ni esquema. Guía en [VENTAS_WHATSAPP.md](VENTAS_WHATSAPP.md).

Build con tipos, 33 pruebas frontend y 52 backend aprobados; 59 integraciones omitidas. Chrome aislado verificó los flujos y responsive a 320/390/768/1440 px, sin enviar mensajes ni escrituras API. Comparación de catálogo y 16 tablas comerciales sin cambios. Las pruebas previas de 111 corresponden a las ventas administrativas; no se reejecutaron integraciones para esta tarea de frontend.

## Estado vigente del 30 de octubre de 2026

Mejora posterior de registro: el formulario inicial permite Pago pendiente, Pago parcial con abono inicial, o Pago completo; venta y pago se guardan juntos sin autorización automática. Resumen de recibido/saldo y medio del pago real. Conjunto actualizado de111 pruebas aprobado sin omisiones, incluidas59 PostgreSQL aisladas, build con tipos y navegador. No necesita otra migración. Esta mejora sustituye la regla inicial de registrar siempre la venta sin abonos.

Esta actualización prevalece sobre los pendientes y el apagado históricos del 28 de octubre. Backend y frontend están activos. Ventas incorpora abonos parciales/completos, saldo, filtros de pago y autorización explícita del cobro completo solo al llegar a saldo cero. Ni el registro de venta ni el último abono autorizan el cobro automáticamente. Correcciones y autorizaciones tienen auditoría; se conservan precios, costos históricos y las reglas de inventario. Detalle en [Ventas](VENTAS_ADMIN.md#abonos-saldo-y-autorización-de-cobros).

El Dashboard separa importe vendido/margen por fecha de venta, abonos por fecha de pago, cobros completos autorizados por fecha de autorización y saldo pendiente total. Los pagos históricos no se reconstruyen: ventas anteriores requieren revisión expresa. La migración 010 es aditiva y quedó aplicada localmente después de respaldo privado. Comparación comercial intacta: 25 referencias, 84 unidades y cero ventas/abonos comerciales de prueba.

Se comprobaron 105 pruebas distintas en dos ejecuciones: 101 completas sin omisiones, incluidas 53 PostgreSQL aisladas, y 52 sin base de datos tras añadir cuatro pruebas de seguridad. Build con TypeScript y navegador aprobados: respuesta perdida/reintento sin duplicación, pagos parciales/completos, autorización con motivo, revocación, filtros en URL, revisión histórica y responsive hasta 320 px. Nueve escrituras interceptadas y cero escrituras comerciales. Evidencias privadas conservadas en `.local/sales-payments-2026-10-30/`.

Próximo pendiente funcional: costos y unidades restantes por recepción/lote, con método de asignación todavía por acordar. Después, contenido pendiente y pedidos desde carrito. Calendarios de cuotas, intereses, caja, reportes avanzados y pagos públicos no forman parte de esta implementación. Sin commit/push; se preservaron las modificaciones previas de documentación.

Revisión original: 22 de octubre de 2026. Última actualización: cierre del 28 de octubre de 2026.

## Estado vigente al cierre del 28 de octubre de 2026

Esta sección prevalece sobre los diagnósticos y prioridades históricos que se conservan más abajo. La tienda tiene catálogo, variantes, carrito local, cuentas, favoritos y administración operativos. Pedidos, cobros públicos, abonos y valoración por lotes todavía están pendientes.

| Avance del día | Resultado |
| --- | --- |
| Identidad visual | Paleta lavanda/lila centralizada en tokens y aplicada a tienda y administración, conservando las fotografías originales. |
| Fotografías y descripciones | 21 textos oficiales en 23 SKU, once portadas conservadas, doce completadas, 43 fotos secundarias y 66 fotos públicas en total. Importación con respaldo e idempotencia; migración aditiva 009 aplicada localmente. |
| Galerías y tonos | Miniaturas verticales circulares, flechas, zoom, teclado, carga/fallos y descripciones en párrafos. Fotos independientes por SKU de Toy Story; edición de secundarias desde Inventario. |
| Te puede interesar | Hasta seis familias disponibles, puntuadas por tipo, categoría, complementariedad y temática; tarjetas compartidas y carrusel táctil, sin consultas individuales ni datos ficticios. |
| Previsualización con mouse | Primera secundaria ordenada del mismo SKU, cargada bajo demanda y con fundido; vuelve a la portada al salir y conserva esta si la secundaria falla. El toque móvil sigue abriendo el detalle. |
| Distribución responsive | Contenedor máximo de 1400 px con márgenes interiores de 16–32 px; ancho útil amplio de 1132 a 1336 px (+18 %). Cinco columnas desde 1440 px, cuatro desde 1100, tres desde 740, dos desde 360 y una por debajo. Banner, catálogo, fichas y pie alineados; galería equilibrada y descripciones de lectura limitada. |

Guías de implementación: [galerías y descripciones](GALERIAS_PRODUCTOS.md) y [productos relacionados y previsualización](PRODUCTOS_RELACIONADOS.md). Se preservaron precios, existencias, categorías, identidades y fotografías originales. La última lectura de la jornada entregó 23 SKU públicos, equivalentes a 21 tarjetas agrupadas; estas cifras son referencias verificadas, no constantes del catálogo.

## Comprobaciones de la jornada

- Backend: 89 pruebas aprobadas al integrar medios, incluidas 41 PostgreSQL en un esquema aislado; cero fallos, errores u omisiones en esa ejecución completa. No ejecutar las integraciones contra datos comerciales.
- Frontend: `npm run build` con tipos aprobado, diez pruebas de recomendaciones aprobadas y `git diff --check` sin errores. JavaScript inicial de aproximadamente 430 kB, sin aviso de tamaño.
- Medios: 66 rutas HTTP 200, formatos/decodificación y huellas de las copias comprobados; párrafos oficiales y separación de tonos verificados. Datos comerciales comparados antes/después de la importación y de las pruebas.
- Navegador: capturas antes/después a 375/430/768/1024/1440/1920 px sin desbordamiento horizontal. Comprobados miniaturas circulares verticales, flechas, zoom/Escape/foco, tonos y SKU del carrito, precio, carrusel y extremos, navegación al inicio, hover y movimiento reducido, toque y deslizamiento móvil. Las pruebas de interacción no emitieron escrituras API.
- Los ajustes finales de distribución modificaron solo cuatro CSS; no cambiaron React/TypeScript, backend, base de datos ni imágenes. En el cierre se revisó documentación y apagado; no se repitieron pruebas de aplicación ni se arrancaron otra vez los servicios.

## Publicación y apagado

Los cinco avances se publicaron mediante push normal en `main` de [Zelko7u7/BlommiBunny](https://github.com/Zelko7u7/BlommiBunny):

| Commit | Contenido |
| --- | --- |
| `6ac4bfa` | Identidad lavanda en toda la interfaz. |
| `b387b64` | Fotografías, descripciones y galerías por producto/tono. |
| `88f12fb` | Recomendaciones y carrusel en las fichas. |
| `6ca4b19` | Previsualización de fotografías al pasar el mouse. |
| `da7b119` | Distribución y aprovechamiento del espacio de la tienda. |

Al finalizar, por petición expresa de la propietaria, se cerraron los árboles verificados de Maven/Spring Boot y npm/Vite, con sus auxiliares Java, Node, esbuild y consolas. A las 15:39:54 de Colombia se verificaron cero procesos de desarrollo asociados al proyecto y cero escuchas en 8080, 5173 y los puertos de depuración 9240–9245. Los navegadores aislados ya estaban cerrados. El servicio PostgreSQL existente y los procesos ajenos al proyecto se conservaron. No reiniciar automáticamente al retomar; comprobar la nueva tarea.

Los respaldos, perfiles, registros y evidencias privadas permanecen en `.local/`; la memoria privada y la configuración real siguen fuera de Git. No se eliminaron archivos ni se modificaron proyectos vecinos.

## Pendientes vigentes, en orden de trabajo

1. **Ventas a cuotas y cobros pendientes.** Registrar importe, abonos y saldo, distinguiendo venta de dinero recibido. La propietaria exige autorización explícita mediante selector de estado o control equivalente antes de incorporar una venta al total final de cobros/ingresos confirmados. Registrar una venta o un abono no autoriza sumar su importe completo. Precisar qué indicadores se afectan y cómo se contabilizan los abonos antes de implementar. Detalle en [Ventas](VENTAS_ADMIN.md#pendiente-prioritario-ventas-a-cuotas-y-abonos).
2. **Costos variables por resurtido.** Conservar costo y unidades restantes por recepción/lote, fecha e identificador de compra; valorar el stock sumando unidades restantes por su costo de origen. Las recepciones nuevas no revalorizan mercancía anterior ni ganancias históricas. Falta acordar asignación a ventas, por ejemplo FIFO; todavía no está elegida. Detalle en [Compras](COMPRAS_GASTOS.md#pendiente-costos-variables-por-resurtido).
3. **Contenido pendiente.** Fotografías y descripción oficiales de Kit Pizza Planet (ID 3) y Rubor En Crema Lotso (ID 5). Para separar Gloss Corazón (ID 18) en tonos 03/05/06, confirmar existencias reales por tono y fotografías identificables, sin inventar reparto, precios ni imágenes.
4. **Compra pública real.** Pedido persistido desde carrito y confirmación por WhatsApp propuestos para después de las prioridades anteriores; checkout, cobros y pagos públicos siguen sin implementar. El carrito actual no crea pedidos ni reserva stock.
5. **Condiciones y despliegue.** Confirmar identidad y condiciones comerciales de pagos/envíos y finalizar políticas; preparar alojamiento, HTTPS, orígenes, secretos de producción y respaldos automáticos con restauración comprobada.
6. **Aplazados.** Caja/movimientos de dinero, reportes avanzados y configuración permanecen fuera del menú hasta que se autorice su alcance. No incorporarlos por inferencia a un ajuste visual o al retomar contexto.

Antes de futuras migraciones o ajustes de inventario, consultar la base actual y respaldar. No reimportar el Excel histórico ni ejecutar migraciones locales ya aplicadas por suposición. Los valores y PID de revisiones anteriores son antecedentes.

## Revisión original y avances históricos

Las siguientes secciones conservan la secuencia de revisiones anteriores. Sus cifras, hallazgos ya corregidos y sugerencias de prioridad deben interpretarse según su fecha; el estado actual está arriba.

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

## Actualización de catálogos administrativos

`CatalogPage.tsx` conecta los cuatro directorios con `CatalogDirectory.tsx`, una vista tipada para categorías, marcas, proveedores y clientes. Se reemplaza el formulario lateral y el resumen que ocultaba campos por listados de ancho completo, detalle, creación y edición en diálogo. Descripciones, teléfono y correo simultáneos, dirección, notas y cumpleaños de clientes quedan visibles sin recortar el texto. Los datos ausentes se identifican como tales y el estado activo/inactivo se muestra explícitamente.

Se añaden búsqueda sin distinguir tildes, filtros por estado, orden por nombre y páginas de 12 registros, conservados en la URL. Crear/editar reutiliza POST/PUT existentes; eliminar usa confirmación y explica los conflictos por relaciones. Los formularios respetan límites de la API, conservan datos ante error y bloquean controles durante guardado. Las mutaciones refrescan también los datos relacionados de inventario, tienda y dashboard.

El build con tipos pasa y la revisión actual de `src` no encuentra `@ts-nocheck`. Las pruebas de navegador simulan creación, edición completa, desactivación, borrado y fallos en los cuatro directorios; verifican detalle, filtros, paginación, foco inicial y anchos 320/390/768/1440 px. La API real se comprobó por lectura; inventario, ventas y los cuatro directorios comerciales permanecen intactos. No cambia el backend ni requiere migración. El bundle ronda 844 kB y conserva el aviso de tamaño.

## Compras recibidas y gastos

`/admin/compras` y `/admin/gastos` reemplazan las pantallas de Próximamente. `OperationsPage.tsx` incorpora formularios, historial paginado, detalle, filtros persistentes y totales agregados. La API devuelve DTO e importes confirmados que excluyen anulaciones. Los importes abarcan todas las páginas coincidentes.

`AdminPurchaseService` recibe mercancía bajo bloqueos de productos, conserva snapshots y evita duplicación de recepciones mediante UUID/huella. Anular retira unidades una sola vez; si falta stock para cualquier referencia, toda la anulación se revierte. El costo de cada compra queda guardado sin sobrescribir el costo de referencia de los productos. `AdminExpenseService` agrega registro validado y anulación auditada; se retira el borrado físico. Los servicios anteriores de transacciones se reemplazan por estas implementaciones. Se conserva el historial.

La migración manual aditiva `008_admin_purchases_expenses.sql` agrega columnas e índices, sin modificar productos. Fue aplicada localmente después de respaldo y comparación de datos. Pasan 67 pruebas backend con PostgreSQL aislado, build frontend y pruebas de navegador a 320/390/768/1440 px. No se crearon operaciones comerciales para probar. El bundle ronda 864 kB. Caja/movimientos de dinero queda aplazado por decisión de la propietaria. Alcance y limitaciones en [COMPRAS_GASTOS.md](COMPRAS_GASTOS.md).

## Revisión funcional general y correcciones

Se revisaron los flujos existentes de tienda, cuentas, favoritos, carrito, inventario, catálogos, dashboard, ventas, compras y gastos, junto con sus servicios, DTO, protección de acceso y relaciones. Las escrituras se comprobaron en PostgreSQL aislado o mediante respuestas de navegador simuladas; la base comercial solo se consultó por lectura. No se realizó una auditoría de dependencias, prueba de penetración ni validación legal nueva.

| Hallazgo confirmado | Corrección |
| --- | --- |
| El listado público reutilizaba la consulta administrativa y entregaba referencias deshabilitadas. | Consulta pública propia: solo productos activos, no eliminados y confirmados. Fichas, variantes y favoritos también excluyen los provisionales. Las fichas agotadas siguen disponibles. |
| Inventario solo cargaba las primeras 100 referencias y los empates de nombre podían desordenar páginas. | Carga de todas las páginas y orden secundario por ID. Catálogo y selectores comparten el lector paginado con validación de respuesta. |
| Ajustes manuales podían desbordar enteros o perder incrementos simultáneos. | Bloqueo de producto, cálculo en long, rango válido y rechazo de ajuste cero; edición exige versión. |
| Precios de producto aceptaban precisión o magnitud incompatibles con sus columnas. | Validación de 12 enteros y dos decimales; errores de formato de fechas/opciones devuelven 400 con DTO de error. |
| Inventario redondeaba importes a pesos completos y no permitía elegir marca/proveedor. | Hasta dos decimales y selectores opcionales de marca/proveedor/fecha, preservando relaciones existentes inactivas. |
| Buscador del panel sin acción y accesos a módulos aplazados. | Enter abre inventario filtrado; se ocultan Caja, Reportes y Configuración del menú y se retira el indicador estático «Todo al día». |
| Filtros de ventas se perdían al recargar y sesión almacenada no contemplaba base64url/sincronización. | Filtros en URL, lectura validada de sesión y actualización/caché limpia ante cambios en otra pestaña. |
| La tienda descargaba también todas las pantallas y gráficos administrativos. | Importaciones bajo demanda y estilos compartidos independientes de la ruta; JavaScript inicial 421,05 kB frente a unos 866 kB. Sin aviso de tamaño en build. |
| Inventario desbordaba en móvil de 320 px. | Cabecera apilada y buscador adaptable, comprobados también abriendo directamente la ruta. |
| Varias consultas del dashboard podían leer momentos diferentes durante una venta concurrente. | Transacción de lectura con snapshot consistente REPEATABLE_READ. |

La validación final aprueba **77 pruebas backend**: 20 de acceso/validación, 17 de productos, 5 unitarias del dashboard y 35 PostgreSQL. Incluye protección de información pública, paginación con 101 nombres iguales, ajustes concurrentes, aislamiento de favoritos y persistencia de marca/proveedor/fecha. La compilación frontend con tipos pasa.

Las cinco regresiones de navegador de tienda, dashboard, catálogos, ventas y compras/gastos pasaron. Se añaden pruebas de 101 referencias administrativas, centavos, edición de relaciones, búsqueda del panel, filtros al recargar y sesión entre pestañas. Se verifican anchos 320/390/768/1440, foco y errores. Evidencias locales privadas en `.local/final-review/`. La API real responde con 15 referencias públicas filtradas, fichas ocultas 404 e inputs inválidos 400. Comparación final de productos, operaciones, movimientos, favoritos, catálogos y cuentas: datos comerciales idénticos.

Para esta etapa conviene mantener fuera del menú Caja/movimientos de dinero, reportes avanzados y configuración sin funciones. Antes de usar la tienda para recibir pedidos faltan el flujo real de pedido/confirmación desde carrito y las condiciones comerciales definitivas. Fotografías y descripciones deben completarse con información real. Antes de desplegar faltan configurar alojamiento/HTTPS/orígenes/secretos de producción y automatizar respaldos con prueba de restauración. El costo de referencia sigue siendo manual: las compras guardan su costo propio, sin implementar promedio o lotes; el margen mostrado no es utilidad neta. Estas tareas pendientes no se presentan como implementadas.

## Recapitulación de la jornada y siguiente sesión

Durante la jornada se preparó el repositorio independiente de Bloomi Bunny y se completaron la experiencia del catálogo/carrito, ventas externas con anulación segura, dashboard real y directorios administrativos con toda su información. El avance final incorpora compras recibidas, gastos, la migración correspondiente y la revisión funcional de los módulos anteriores.

La revisión corrigió publicación de referencias ocultas/provisionales, el límite de 100 productos administrativos, precisión de importes, ajustes concurrentes de inventario, selección de marca/proveedor, filtros de ventas, sesiones y buscador del panel. También redujo la descarga inicial mediante carga de pantallas bajo demanda y retiró del menú las funciones aplazadas. Validación de cierre: 77 pruebas backend, build frontend con tipos, regresiones de los cinco flujos y comparación de datos comerciales intactos.

En esa revisión se propuso conectar el carrito con un pedido real y confirmación por WhatsApp. La decisión posterior de la propietaria prioriza ventas a cuotas/abonos y costos por resurtido, como se precisa en el estado vigente del 28 de octubre. Pedidos y cobros públicos siguen pendientes; Caja, reportes avanzados y configuración continúan aplazados.

La memoria privada de continuidad se actualizó en `PROJECT_MEMORY.md`, conservada localmente y excluida de Git. Los respaldos, datos comerciales y evidencias privadas también permanecen locales. Este informe y las guías de los módulos forman parte de la documentación versionada del proyecto.
