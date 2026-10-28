# Productos relacionados: Te puede interesar

La ficha `/producto/:id` incorpora un carrusel horizontal **TE PUEDE INTERESAR** después de la descripción. Conserva la galería vertical de miniaturas circulares, los tonos, las fotografías originales y el carrito. No requiere migraciones, dependencias nuevas ni cambios del backend.

## Datos y cálculo

Se utiliza el catálogo público completo que la tienda ya carga para búsqueda y carrito, junto con el producto seleccionado. No se consulta cada recomendación por separado ni se accede a datos administrativos. La API continúa siendo responsable de excluir registros ocultos, eliminados y provisionales. Al comprobar esta actualización entregaba 23 SKU publicados; ese número no está fijado en el código.

`services/recommendations.ts` concentra las reglas ampliables. El tipo se reconoce mediante términos completos en nombre/presentación: gloss, tinta, labial, bálsamo, sombras, rubor, iluminador, sérum, parches y accesorios. Se contempla el alias confirmado Pompón Cotton para iluminador. Las categorías proceden de la API; sus familias agrupan denominaciones equivalentes, como Labios/Labiales y Cuidado de piel/Skincare. Estas clasificaciones se calculan en memoria y no crean etiquetas ni campos en la base.

| Relación | Puntos |
| --- | ---: |
| Mismo tipo reconocido | +50 |
| Misma categoría o familia equivalente | +30 |
| Complemento definido en las reglas | +25 |
| Colección explícita Toy Story o Lotso | +20 |
| Categoría cercana de maquillaje, si no hay otra relación | 5 |

Los puntos se acumulan. Los complementos de labios relacionan labiales, glosses, tintas y bálsamos; sombras se relacionan con rubores/iluminadores; rubores e iluminadores con sombras; sérum con parches; y accesorios de pelo, cartucheras y monederos entre sí. Las temáticas se detectan con términos completos y reconocidos en nombres/grupos de variantes, sin coincidencias parciales de palabras genéricas.

Los respaldos de cinco puntos se limitan a categorías cercanas de maquillaje: labios/rostro y ojos/rostro. No se rellenan artificialmente las recomendaciones de skincare o accesorios con productos ajenos. Una coincidencia de temática reconocida también puede relacionar artículos de distintas categorías. Para ampliar las reglas se modifican las tablas de tipos, complementos, categorías cercanas y temáticas del mismo archivo.

Los resultados se ordenan por puntuación descendente y, ante empate, ID ascendente. Se muestran hasta seis familias distintas. Se excluyen el SKU abierto y todos los tonos de su propio `variantGroup`. Los tonos de otras familias se agrupan mediante `buildCatalog`, igual que en el catálogo. Solo se muestran familias con al menos un SKU publicado con stock; se elige un representante disponible y se conserva la información de otros tonos publicados para abrir su selector. Los SKU duplicados se eliminan por ID.

Si hay menos de cuatro candidatos adecuados se muestran únicamente los que existen. Sin coincidencias la sección se oculta. Cambiar de producto o tono recalcula el resultado, y el carrusel comienza de nuevo; los re-renderizados del carrito no cambian su orden ni reinician su posición.

## Tarjetas, navegación y carrito

`ProductCard.tsx` extrae la tarjeta que ya utilizaban catálogo y favoritos; el carrusel usa ese mismo componente. Muestra portada, nombre, categoría, precio real en COP y acción. Imágenes y nombres enlazan a la ficha, y el fondo de la tarjeta también permite abrirla. Se conserva el enlace de regreso al catálogo y sus filtros.

Los productos con variantes muestran **Elegir tonalidad**, que abre el detalle sin agregar un SKU automáticamente. Los demás muestran **Agregar** y usan la función existente `changeQuantity`, con el stock y precio del catálogo. El evento del botón se detiene para evitar navegación accidental. Al alcanzar el stock disponible el botón se deshabilita. El carrito sigue siendo local y no reserva ni descuenta existencias comerciales.

## Carrusel responsive

`ProductRecommendations.tsx` calcula el resultado con `useMemo` y gestiona el desplazamiento nativo horizontal con ajuste a tarjetas. Hay cuatro tarjetas visibles en escritorio amplio, tres en escritorio intermedio, dos en tablet y aproximadamente 1,3 en móvil. Las flechas avanzan por grupos visibles y se deshabilitan en los extremos. Se ocultan si el contenido cabe sin desplazarse.

Se puede deslizar con el dedo, desplazar con trackpad o usar el teclado al enfocar la región (izquierda/derecha, Inicio/Fin). Se respeta la preferencia de movimiento reducido. `ResizeObserver` actualiza los límites tras cambios de tamaño. La paleta, fotografías proporcionadas, sombras y tipografía se reutilizan mediante los tokens lavanda existentes; no hay reproducción automática.

## Archivos

- Nuevos: `src/services/recommendations.ts`, `src/components/store/ProductRecommendations.tsx`, `src/components/store/ProductCard.tsx` y `src/styles/recommendations.css`.
- Integración: `src/pages/StorePage.tsx` conecta datos y carrito; `src/components/store/ProductDetails.tsx` añade un espacio para la sección después de la descripción.
- Pruebas: `BloomiFront/tests/recommendations.test.mjs`, con Node y el compilador ya instalado con Vite.
- Documentación: esta guía, README y memoria privada de continuidad.

## Verificación

```powershell
# Desde BloomiFront
node --test tests/recommendations.test.mjs
npm run build
```

Diez pruebas de recomendaciones cubren prioridades, complementos, Toy Story/Lotso, exclusión del producto/grupo actual, duplicados, familias agotadas/inactivas, pocos resultados, orden estable y conservación de datos. Build con TypeScript aprobado. No se modificaron paquetes ni dependencias.

Pruebas de navegador con el catálogo real: recomendaciones de labios, ojos y Toy Story; exclusión del actual y duplicados; precios/portadas; flechas y límites a 1440/1024/768/390/320 px; arrastre táctil con eventos de pantalla; agregado sin navegación ni salto del carrusel; límite de stock; selección de tono; cambio de producto con regreso al inicio y nuevo resultado; tarjetas compartidas del catálogo. Escenarios de pocos productos, inactivos y agotados se simularon interceptando exclusivamente respuestas de lectura. Sin coincidencias la sección se oculta y con un solo resultado no hay flechas. No hubo errores JavaScript ni peticiones de escritura desde las pruebas.

Evidencias privadas en `.local/related-products-2026-10-28/`, con capturas y `browser-results.json`. La comparación comercial incluye productos, imágenes ordenadas, operaciones, movimientos, cuentas, favoritos y catálogos. La actualización no modifica sus datos.
