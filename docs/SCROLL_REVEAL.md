# Aparición progresiva del contenido público

Implementada el 18 de noviembre de 2026, exclusivamente para la experiencia del comprador.

## Alcance

- Encabezado Encuentra tus favoritos / Mis favoritos.
- Tarjetas del catálogo público y favoritos.
- Título y tarjetas de Te puede interesar, incluidas las que entran al desplazar horizontalmente sus recomendaciones.
- Descripción informativa de la ficha de producto.
- Bloques de Nosotros, información comercial y secciones de las políticas públicas.

El header, búsqueda, filtros, splash, carrusel principal, carrito, navegación, controles de recomendaciones y modales quedan fuera del efecto. La administración y sus formularios no registran elementos ni crean observadores de reveal. Los botones de compra y corazones conservan sus manejadores; se anima la tarjeta completa, sin efectos independientes sobre sus controles.

No hay secciones públicas independientes de categorías/destacados adicionales que requieran crearse para esta tarea. Las categorías existentes continúan funcionando como filtros del catálogo.

## Funcionamiento

`useRevealOnScroll` comparte un solo IntersectionObserver entre los elementos pendientes. No instala manejadores de scroll ni hace actualizaciones de estado React en cada desplazamiento. Al entrar en pantalla, deja de observar el elemento y aplica opacidad 0→1 y translateY 20→0 durante 550 ms con ease-out. Las tarjetas de un grupo reciben intervalos de 70 ms, con una espera máxima de 280 ms para evitar colas largas después de un desplazamiento rápido.

Los elementos ya visibles al montarse se muestran inmediatamente. Cada instancia se revela una sola vez; volver a subir, ordenar o actualizar sus datos no la reinicia. Las tarjetas que se montan después de una búsqueda/cambio de categoría se registran automáticamente; las eliminadas liberan observación, eventos y temporizadores.

Las dimensiones y jerarquía DOM existentes se conservan. `RevealOnScroll` puede renderizar el mismo elemento semántico (div, section, título o párrafo), mientras las tarjetas usan el hook directamente sobre su article. Las reglas CSS están limitadas a elementos expresamente marcados dentro de `.store`. `ProductCard` exige `reveal` explícito; el valor por defecto es false.

Con movimiento reducido, tanto inicial como activado durante la animación, el contenido se muestra sin efectos. Lo mismo ocurre si IntersectionObserver no existe o falla al crearse. El foco por teclado muestra el contenido inmediatamente. Un temporizador de respaldo completa una animación cancelada/sobrescrita. Al terminar se retiran el will-change y la animación; al liberar el último elemento también se desconectan el observador y la escucha de preferencia de movimiento.

## Archivos

Nuevos:

- `BloomiFront/src/hooks/useRevealOnScroll.ts`.
- `BloomiFront/src/components/store/RevealOnScroll.tsx`.
- `BloomiFront/src/styles/scroll-reveal.css`.

Integraciones:

- `StorePage.tsx`: encabezado del catálogo y activación explícita en tarjetas públicas.
- `ProductCard.tsx`: referencia al article existente y propiedad reveal.
- `ProductRecommendations.tsx`: título y tarjetas recomendadas; controles intactos.
- `ProductDetails.tsx`: sección de descripción; bloque de compra intacto.
- `LegalPages.tsx`: bloques informativos públicos; footer y navegación intactos.

## Validaciones

Compilación TypeScript/Vite aprobada y 38 pruebas frontend existentes aprobadas. Las pruebas necesitaron ejecución autorizada fuera del aislamiento por restricciones de esbuild. No se instalaron dependencias.

Chrome aislado comprobó 375/430/768/1024/1440/1920 px: visibilidad inicial, aparición progresiva, duración/desplazamiento/easing, escalonado, estabilidad de la posición de layout, un observador compartido, ausencia de repetición, categorías, búsqueda, limpieza de búsqueda/filtros, foco, favoritos de visitante, agregar al carrito y apertura/cierre del carrito. También descripción/recomendaciones, desplazamiento horizontal y contenido informativo público. Sin desbordamientos, excepciones JavaScript ni solicitudes de escritura API.

Movimiento reducido inicial/dinámico, ausencia/fallo de IntersectionObserver y animación interrumpida comprobados. Títulos por debajo del primer viewport verificados en móvil/escritorio con pantalla de 480 px de alto. Administración a 375/1440 px y formulario de categoría comprobados con datos de lectura simulados en el navegador: cero atributos, animaciones u observadores de reveal.

Se compararon los archivos con una referencia tomada antes de editar: 232 archivos protegidos permanecen intactos, incluidos backend, fotografías, identidad oficial, splash, carrusel, carrito, WhatsApp, autenticación y administración. Solo cambian los cinco componentes públicos enumerados y los tres archivos nuevos. No hubo conexiones directas a PostgreSQL ni pruebas backend. Servicios existentes conservados activos; navegador de prueba cerrado y evidencias privadas conservadas en `.local/scroll-reveal-2026-11-18/`.
