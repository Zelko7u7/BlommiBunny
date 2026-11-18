# Identidad oficial de Bloomi Bunny

Implementada el 18 de noviembre de 2026. La fuente oficial vigente es `BloomiIconos_Logos/`. Esta ubicación sustituye las referencias históricas a `BloomiIconosYLogos/`; se conserva el cambio de carpeta que ya estaba presente al iniciar esta tarea.

## Recursos utilizados

| Recurso original | Características | Uso |
| --- | --- | --- |
| `Logotipo_BloomiBunny.svg` | SVG vectorial, viewBox `0 0 8663.09 4142.24`, proporción aproximada 2,0914:1, 825.904 bytes. Nombre incluido en los trazados, sin eslogan. Fondo transparente y textura en tonos ciruela/morado oscuro. | Header, bienvenida, pie, acceso/registro y administración expandida/móvil. |
| `isotipoB-Transparente.svg` | SVG vectorial cuadrado, viewBox `0 0 4020 4020`, 680.931 bytes. B transparente oficial. | Favicon vigente y barra administrativa contraída. |

Los viewBox expresan coordenadas vectoriales, no una resolución raster fija. Los recursos conservan todos sus trazados, textura, colores y márgenes originales. No hay recortes, filtros, recoloreado, tipografías aproximadas ni dependencias nuevas. Las copias de `public/brand/` y `dist/brand/` se verificaron byte por byte contra los originales. Los seis SVG de la carpeta oficial permanecen intactos.

También se inspeccionaron `isotipo.svg`, `isotipo_linea.svg`, `isotipo_blanco.svg` e `isotipoB-blanco.svg`. Las variantes blancas aportan un soporte claro para fondos oscuros. No se cargan ni se duplican en el frontend porque las superficies de marca actuales son claras. Se mantiene la B transparente elegida previamente para favicon, con una nueva versión en su URL para renovar la caché.

## Integración

`BrandLogo` centraliza las rutas basadas en `import.meta.env.BASE_URL`, el texto alternativo, las dimensiones, la prioridad y la selección de la B compacta. Un mismo archivo completo se reutiliza en todos los componentes y se precarga desde `index.html`; el pie utiliza carga diferida. El navegador puede compartir su caché entre header e intro. SVG permite adaptar el tamaño sin introducir imágenes raster borrosas.

Por ajuste posterior de la propietaria el 18 de noviembre, el logo se redujo para una presencia más discreta. Header: 164 px en escritorio, 156 px en tablet y 136 px en móviles de 375/430 px. Administración expandida: 164 px; pie: 160 px. Conserva enlace a inicio, búsqueda, navegación y acciones. El nombre no se repite junto al dibujo. El panel mantiene la etiqueta contextual Administración; al contraerlo utiliza la B oficial de 48 px. El menú móvil conserva el logo completo.

Acceso y registro: logo de 176 px en escritorio y 168 px en móvil, centrado respecto al formulario mediante un enlace del ancho de su contenido y márgenes laterales automáticos. Intro: ancho fluido entre 208 y 320 px, sin cambios de tiempos o animaciones. Compilación y revisión a 375/430/768/1024/1440/1920 px aprobadas, con centrado y proporciones comprobados. Evidencias del ajuste en `.local/brand-scale-2026-11-18/`.

La intro conserva la secuencia Compra → Disfruta → Florece, una palabra por vez, y A tu manera ♡. El logo aparece con opacidad y escala de 0,98 a 1 durante 450 ms. La salida empieza a los 2.000 ms y desliza la pantalla hacia abajo durante 500 ms. Se conservan el temporizador de seguridad, el comportamiento por sesión y el soporte de movimiento reducido. No se cambian los tiempos de las palabras.

La paleta lavanda existente se conserva; el foco de los enlaces de marca utiliza `#553184`. Los íconos funcionales, la identificación comercial en los textos legales y las iniciales del perfil de usuario conservan su función.

## Archivos de aplicación

- Nuevos: `BloomiFront/src/components/common/BrandLogo.tsx`, `BloomiFront/src/styles/brand.css` y `BloomiFront/public/brand/Logotipo_BloomiBunny.svg`.
- Integración: `BloomiFront/src/pages/StorePage.tsx`, `BloomiFront/src/pages/LoginPage.tsx`, `BloomiFront/src/layouts/AppLayout.tsx`, `BloomiFront/src/components/store/IntroSplash.tsx` y `BloomiFront/src/components/store/LegalPages.tsx`.
- Animación y metadatos: `BloomiFront/src/styles/intro-splash.css` y `BloomiFront/index.html`.
- El favicon público se cotejó y resultó idéntico al original oficial; no necesitó cambios en su contenido.
- Documentación: esta guía, `README.md`, `docs/REVISION_PROYECTO.md` y memoria privada `PROJECT_MEMORY.md`.

## Validación

Build final con TypeScript y 38 pruebas frontend aprobados. Chrome aislado verificó 375/430/768/1024/1440/1920 px: visitantes, clientes y administradora en el header; acceso, registro, administración, menú móvil y barra contraída. Se comprobaron carga de logos, proporciones, ausencia de filtros, enlaces, favicon, ausencia de desbordamiento/superposición y permanencia del carrusel. La sesión administrativa fue simulada solo en el navegador, con respuestas de lectura interceptadas.

Se midieron la duración aproximada de 2,5 segundos, exclusividad de las palabras, entrada del logo, transformación de salida, ausencia de repetición al recargar y movimiento reducido inicial/dinámico. También se comprobó apertura/cierre del carrito y navegación a inicio al pulsar el logo. Las 23 imágenes renderizadas en la tienda se decodificaron correctamente después de activar su carga en el navegador aislado. Cero excepciones JavaScript y cero solicitudes de escritura API en esta revisión.

Evidencias privadas: `.local/official-brand-2026-11-18/`, con manifiesto de originales, comprobación de recursos, resultados de navegador y capturas. No se modificó backend ni base de datos, no se ejecutaron pruebas backend que escribieran en PostgreSQL y no se cambiaron productos, inventario, favoritos, WhatsApp ni operaciones administrativas. Servicios existentes conservados activos.
