# Fotografías, galerías y descripciones de Bloomi Bunny

Actualización del 28 de octubre de 2026. En las comprobaciones de la jornada se conservaron 25 referencias y 84 unidades; consultar la base antes de futuros cambios, porque estas cifras son una referencia temporal. Se incorporaron las 21 descripciones oficiales a 23 SKU, contando los tonos separados de Toy Story. La importación no modificó precios, costos, cantidades, categorías, identificadores, nombres ni estados de publicación.

## Resultado por producto

El número de fotografías incluye la portada. Todas las referencias de esta tabla recibieron la descripción íntegra de su entrada correspondiente en `BloomiBunny_descripciones_y_mapeo.md`, con sus párrafos y acentos. No se incluyeron como descripción las instrucciones de archivos ni las notas de inventario del documento.

| ID | Registro existente | Portada | Fotografías |
| ---: | --- | --- | ---: |
| 1 | Sombra Toy Story | Nueva | 3 |
| 2 | Iluminador Morado Buzz | Nueva | 4 |
| 4 | Cartuchera Metálica Toy Story | Nueva | 3 |
| 6 | Tintas Vaqueras Toy Story · Tono 6 | Nueva | 1 |
| 7 | Kit Accesorios Pelo Lotso | Nueva | 2 |
| 8 | Tinta De Labios Princesas Bella | Nueva | 2 |
| 9 | Monedero Lotso | Nueva | 6 |
| 10 | Lip Balm Glitter Ham #03 | Nueva | 2 |
| 11 | Labial Doble Vaquero Toy Story · Tono 2 | Nueva | 3 |
| 12 | Labial Doble Vaquero Toy Story · Tono 3 | Nueva | 3 |
| 13 | Tintas Vaqueras Toy Story · Tono 4 | Nueva | 1 |
| 14 | Hidratante De Labios Bob Esponja | Conservada | 5 |
| 15 | Sobre Mascarilla Stickers Para Acné Happy Clean | Conservada | 2 |
| 16 | Kit X 3 Gloss Mood Pills | Conservada | 3 |
| 17 | Serum Hidratante Morado Trendy | Conservada | 3 |
| 18 | Tinta Gloss Corazon Trendy | Conservada | 3 |
| 19 | Kit X 5 Garden Gloss | Nueva | 2 |
| 20 | Sombra Perfixt Beauty Glazed | Conservada | 3 |
| 21 | Sombra Glitter Circus | Conservada | 3 |
| 22 | Rubor Doble Mia | Conservada | 4 |
| 23 | Pompon Cotton | Conservada | 2 |
| 24 | Mousse Labial Black Kitty | Conservada | 3 |
| 25 | Tinta para Labios y Mejillas Crimson Skull | Conservada | 3 |

Se conservan las once portadas anteriores. Las doce nuevas utilizan la fotografía número 1 de su producto y tono en la carpeta `Productos/`, verificada visualmente. Sus rutas publicadas están en `BloomiFront/public/products/gallery/`, con nombres normalizados; por ejemplo, `labial-doble-vaquero-toy-story-2-01.webp` y `tintas-vaqueras-toy-story-6-01.avif`.

Hay 66 fotografías publicadas entre portadas y secundarias: once anteriores y 55 archivos nuevos. `Stickers Para Acné Happy Clean 1.png` y `2.png` tienen contenido idéntico y se muestran una sola vez. Los 56 originales adicionales y las once imágenes originales se conservan intactos. No apareció un ZIP en la raíz: se utilizó la carpeta `Productos/` ya extraída. La carpeta de originales existente se llama realmente `Blommiimages/`.

De las 56 fotografías adicionales, 46 contienen WebP, siete JPEG, dos PNG y una AVIF, aunque varias tenían extensiones PNG/JPG. Las copias públicas usan su formato real, sin conversión, compresión ni alteración de píxeles. Los SHA256 de las copias coinciden con los originales.

## Galería y administración

La ficha existente `/producto/:id` muestra portada, miniaturas ordenadas en una columna vertical a la izquierda, selección de foto, flechas y ampliación en diálogo. La columna mantiene su orientación también en móvil y permite desplazarse verticalmente cuando hay muchas imágenes. Las flechas del teclado, Inicio/Fin y Escape permiten navegar; el foco vuelve al botón que abrió la ampliación. Hay transición suave, estado de carga, salto a otra imagen cuando una falla y reintento cuando fallan todas. Con una sola foto no se muestran flechas ni miniaturas innecesarias. Las fotografías mantienen sus proporciones con `object-fit: contain`.

La identidad lavanda se conserva mediante los tokens existentes. Las miniaturas son circulares y mantienen su columna vertical; la foto principal conserva su formato. El catálogo y el carrito siguen usando `imageUrl` como portada; la galería no cambia el producto que se compra. Las descripciones se leen desde PostgreSQL y se presentan en párrafos, sin resumir el texto oficial.

Inventario → Editar permite consultar y cambiar las fotografías secundarias en un campo de una URL por línea, con vista previa y hasta 20 imágenes. El orden de las líneas determina el orden de la galería. La portada sigue siendo un campo independiente. El backend conserva las imágenes si un cliente anterior omite `galleryImages`; una lista vacía explícita elimina las secundarias, sin borrar la portada. Se mantienen el control de versiones y los movimientos de stock existentes.

## Tonos y pendientes

- Labial Doble Vaquero conserva los SKU 11 y 12, tonos 2 y 3. Cada uno tiene tres fotografías propias. Seleccionar otro tono reinicia la galería en su portada y cambia el SKU del carrito.
- Tintas Vaqueras conserva los SKU 13 y 6, tonos 4 y 6. Cada uno tiene su propia fotografía. No se mezclan sus imágenes.
- Tinta Gloss Corazón tiene un único registro de inventario, ID 18. Sus fotografías incluyen el producto y la carta general de tonos. El documento confirma 03, 05 y 06, pero no reparte las existencias entre ellos: no se crean SKU, precios, selectores de compra ni existencias ficticias por tono. Para venderlos separadamente falta confirmar ese reparto y las fotos identificables de cada tono.
- Kit Pizza Planet, ID 3, y Rubor En Crema Lotso, ID 5, no tienen descripción oficial ni fotografías propias en el material entregado. Se conservan sin asignarles contenido de otros productos.
- Durante la importación se conservaron ocultos los ocho registros que entonces lo estaban (1, 2, 4, 7, 8, 9, 10 y 19). En una comprobación posterior del mismo día la API ya entregaba 23 SKU publicados, equivalentes a 21 tarjetas agrupadas; las tareas de recomendaciones y estilos preservaron ese estado. No utilizar la lista anterior como estado actual ni cambiar publicación automáticamente al repetir una verificación.

## Persistencia y actualización reproducible

La migración aditiva `009_product_gallery.sql` crea `product_image(product_id, sort_order, image_url)`. La portada permanece en `product.image_url`; no se necesita duplicar su indicador en cada fila. JPA utiliza una colección ordenada por producto, con carga por lotes. Las API pública y administrativa añaden `galleryImages` sin exponer costos ni proveedores públicos.

`tools/product_media_map.json` documenta las asociaciones revisadas por código existente, nombre normalizado y tono. `tools/update_product_media.py` lee el Markdown oficial completo, valida esas correspondencias y publica las copias de las fotos. La aplicación persiste las descripciones y fotos en una transacción, con respaldo completo previo, bloqueo y comprobación de los registros originales. No crea productos ni registra movimientos comerciales. Repetir la operación no cambia versiones ni fechas y no duplica fotos.

**En el equipo actual ya se aplicaron la migración y el contenido; no hay comandos manuales pendientes.** Para repetir la instalación en otra base que contenga los mismos códigos, después de revisar su configuración local y conservar el Markdown y los originales:

```powershell
python tools/update_product_media.py --prepare
python tools/update_product_media.py --apply
python tools/update_product_media.py --verify
```

`--prepare` publica copias y prepara un plan, sin escrituras en la base. `--apply` crea un respaldo PostgreSQL completo, incorpora la tabla si falta y aplica únicamente el contenido confirmado. `--verify` comprueba el resultado de la aplicación. No ejecutar el importador histórico de Excel para esta tarea. Se necesitan las herramientas PostgreSQL y la configuración backend local; no hay dependencias Python adicionales. Los respaldos, planes con datos reales, sesiones y evidencias permanecen en `.local/product-media-2026-10-28/`, fuera de Git. Hibernate no ejecuta los SQL de migraciones por sí solo.

## Archivos de implementación

- Backend: `entity/Product.java`, `dto/ProductDtos.java`, `service/ProductMedia.java`, `service/impl/ProductServiceImpl.java`, `controller/StoreController.java`, `service/FavoriteService.java` y migración `009_product_gallery.sql`.
- Frontend: nuevo `components/store/ProductGallery.tsx`; integración en `ProductDetails.tsx`, `services/catalog.ts` y `types/index.ts`; edición en `components/inventory/InventoryDialogs.tsx`; estilos en `styles/product-detail.css` y `styles/inventory.css`. En `styles/index.css` se corrige el selector de tema de Sonner para que los avisos del carrito respeten los tokens lavanda existentes.
- Recursos: 55 archivos nuevos en `public/products/gallery/`. Las imágenes públicas anteriores no se reemplazan.
- Herramientas: `tools/update_product_media.py` y `tools/product_media_map.json`.
- Pruebas: nuevos `ProductMediaTest` y `ProductMediaIntegrationTest`; adaptación de constructores en `AccessControlTest`, `ProductLifecycleTest` y `ReviewIntegrationTest` al campo opcional nuevo.
- Documentación: esta guía, README y memoria privada de continuidad. Los cambios previos de paleta lavanda se conservan.

## Validaciones

89 pruebas backend aprobadas, incluidas 41 de integración PostgreSQL en un esquema aislado. Las nuevas pruebas cubren persistencia del orden y párrafos, conservación de datos comerciales, clientes anteriores, actualización de versión, rollback de rutas inválidas y separación de galerías por tono. Build frontend con TypeScript aprobado, sin dependencias nuevas.

Se verificaron las 66 rutas con HTTP 200, MIME correcto, SHA256 y decodificación real en Chrome. Las 67 fotografías originales conservaron sus huellas. Las pruebas de navegador comprueban miniaturas, flechas, ampliación, Escape/foco, carga lenta, fotos fallidas, recuperación, separación de tonos, límite real de carrito y párrafos íntegros. Ficha y zoom sin desbordamientos a 320/390/768/1440 px. El guardado administrativo se interceptó y simuló; no creó escrituras comerciales de prueba.

La comparación de datos confirma que las 25 referencias, precios, stock, categorías, códigos, publicación, cuentas, favoritos, catálogos, ventas, compras, gastos y movimientos se conservaron. Solo cambiaron descripciones, portadas faltantes, galerías y sus metadatos de actualización/versionado. La segunda ejecución del importador produjo cero cambios.

## Ajustes posteriores y cierre del día

La galería quedó vertical también en móvil y sus miniaturas son circulares: 68 × 68 px en escritorio/tablet y 44 × 44 px en móvil pequeño. El último ajuste visual comparte el contenedor público de 1400 px, equilibra las dos columnas desde 960 px y centra verticalmente la galería; por debajo, apila foto e información. La descripción conserva todos sus párrafos, con ancho de lectura limitado y título lateral en escritorio. No cambia datos ni imágenes.

Las tarjetas compartidas muestran la primera secundaria ordenada del mismo SKU al pasar el mouse; mantienen la portada si no existe o falla. Ese efecto no mezcla tonos ni se activa por toque móvil. Las fichas incorporan el carrusel descrito en [productos relacionados](PRODUCTOS_RELACIONADOS.md).

Se comprobó la distribución final a 375/430/768/1024/1440/1920 px, sin desbordamiento horizontal, conservando miniaturas, flechas, zoom, foco, selector de tonos y carrito. Backend/frontend y auxiliares quedaron apagados por petición de la propietaria al finalizar; respaldos y evidencias privadas se conservan. Resumen y pendientes en [revisión técnica](REVISION_PROYECTO.md).
