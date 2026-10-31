# Solicitudes de pedido por WhatsApp

Implementado el 30 de octubre de 2026. El carrito permite preparar una solicitud completa de compra sin iniciar sesión. No hay pasarela, cobro, reserva ni pedido persistido en esta etapa.

## Funcionamiento

1. El cliente elige productos y, cuando corresponda, el SKU del tono desde la ficha existente.
2. Añade artículos al carrito y ajusta sus cantidades con los controles habituales.
3. Al abrir el carrito se consulta nuevamente el catálogo público completo. El lector existente recorre todas las páginas; continúa la actualización periódica de 15 segundos.
4. Una vez comprobados los artículos, se habilita **Realizar pedido por WhatsApp**.
5. El clic abre un enlace oficial de Click to Chat con el mensaje preparado. El cliente lo revisa y lo envía manualmente.
6. La tienda confirma disponibilidad, costo de envío y medios de pago en la conversación. La venta que realmente se confirme se registra posteriormente desde Administración → Ventas.

La lista, las imágenes, los controles de cantidad, el subtotal y los botones para eliminar/vaciar voluntariamente el carrito se conservan. Abrir o copiar el mensaje no cambia el carrito local del visitante o de la cuenta, ni fusiona sus selecciones. Tampoco registra una venta, pago o ingreso administrativo.

## Número comercial

El número público confirmado de Bloomi Bunny es `573242213841` (+57 324 221 3841). El destino del carrito depende exclusivamente de `VITE_WHATSAPP_NUMBER`; no tiene un número alternativo implícito.

En `BloomiFront/.env.local`, o en la configuración pública del frontend al compilar:

```dotenv
VITE_WHATSAPP_NUMBER=573242213841
```

`BloomiFront/.env.example` incluye el número oficial como referencia. La configuración local ya quedó creada en `.env.local`, excluido de Git; no hace falta copiar ni sobrescribir la configuración del backend. Si ya tienes otros ajustes de entorno, conserva sus líneas al añadir esta variable.

Para cambiar el destino, sustituye el valor por el nuevo número comercial confirmado en formato internacional: `57` seguido del móvil colombiano de diez dígitos que comienza por `3`. Sin `+`, espacios ni guiones. Reinicia Vite si no recarga automáticamente; en producción recompila y despliega el frontend. Las variables `VITE_*` son públicas: no colocar contraseñas o tokens allí.

Si falta la variable o su formato es inválido, el botón queda deshabilitado y se muestra un aviso; no se genera un enlace a otro número. Esta validación comprueba el formato, no la titularidad ni que el número tenga una cuenta de WhatsApp activa. Los contactos generales de Nosotros/políticas conservan la configuración comercial anterior; esta variable controla específicamente el destino del pedido del carrito.

## Mensaje y cálculos

Se prepara un mensaje con esta estructura, usando exclusivamente datos reales del catálogo y las cantidades del carrito:

```text
¡Hola, Bloomi Bunny! 💜🌸

Quisiera realizar un pedido de los siguientes productos:

🛍️ MI PEDIDO

1. Nombre comercial del producto
   Tono: tono del SKU, solo si tiene ese dato
   Cantidad: unidades seleccionadas
   Precio unitario: precio del catálogo en COP
   Subtotal: precio por cantidad en COP

--------------------

TOTAL DE PRODUCTOS: suma de unidades

SUBTOTAL: suma de los subtotales en COP

Envío: Por confirmar.

¿Me podrían confirmar la disponibilidad, el costo de envío y los medios de pago disponibles?

¡Muchas gracias! 💜
```

El bloque de producto se repite para todos los SKU. No se incluyen costos de adquisición, proveedores, márgenes ni información administrativa. No se inventan promociones, envío o garantía de disponibilidad. Los importes se validan y calculan en centavos enteros; se formatean en español de Colombia con símbolo `$` y sufijo `COP`, conservando hasta dos decimales.

## Variantes

Cada entrada usa `productId`, nombre, `shade` y precio persistidos del mismo SKU. Dos tonos del mismo grupo siguen siendo dos líneas y conservan cantidades y precios independientes. No se deducen tonos por coincidencias de nombre, descripciones o fotografías.

Los datos actuales mantienen los labiales Toy Story en SKU 11/12 y las tintas Toy Story en SKU 13/6. Se conservan literalmente las etiquetas existentes, como `Tono 2`; no se renombran productos ni se inventa un sufijo «5» o una normalización a «02».

Gloss Corazón, SKU 18, continúa con un solo inventario conjunto y sin tono de compra separado. La memoria exige confirmar primero las existencias por tono; el mensaje no lo divide artificialmente en 03/05/06 ni interpreta su carta general como selección del cliente. Un producto sin `shade` no recibe una línea «Tono».

## Validación y errores

- La consulta debe haber terminado correctamente; durante una actualización o si falla, el botón queda deshabilitado. Se ofrece reintentar. En el carrito abierto, lista y mensaje usan la misma consulta pública, sin preferir una ficha más antigua.
- Se rechazan carrito vacío, identificadores/cantidades inválidos o repetidos, producto eliminado u oculto, agotados, cantidades superiores al stock conocido, datos incompletos, precios inválidos o aritmética fuera del rango seguro.
- El cálculo visual anterior puede limitar una cantidad a la disponibilidad u ocultar una referencia ausente, pero la solicitud valida **el carrito original completo**. Si hay un problema no genera un mensaje parcial ni modifica cantidades silenciosamente; muestra qué revisar y permite retirar el artículo explícitamente.
- Los pagos y las existencias definitivas se confirman manualmente. Consultar la API no reserva unidades ni garantiza que el stock no cambie después.

## Apertura, copia y mensajes largos

Se usa `https://wa.me/NUMERO?text=MENSAJE`, con `encodeURIComponent(message)`, conforme a la documentación oficial de [Click to Chat de WhatsApp](https://faq.whatsapp.com/5913398998672934). El mismo enlace sirve de entrada para móvil y escritorio; WhatsApp decide la ruta de app o web según el dispositivo y la sesión.

La apertura ocurre directamente en el clic, sin esperar consultas, portapapeles ni otra operación asíncrona. Se crea una ventana vacía, se retira su `opener` y se navega al enlace. Si se bloquea o falla, se muestra la alternativa de copiar el pedido completo y abrir el contacto manualmente. El enlace alternativo usa `noopener noreferrer`.

Siempre que el pedido es válido también se puede copiar completo, consultar el mensaje en un campo de solo lectura y abrir el contacto sin mensaje. Si falla el portapapeles, se ofrece seleccionar todo para copiar manualmente. Nunca se informa «pedido enviado» por el solo hecho de abrir una ventana.

El límite preventivo del enlace codificado es **8000 caracteres** (`MAX_WHATSAPP_URL_LENGTH`). Es una decisión conservadora de interoperabilidad, no un máximo oficial ni una garantía universal del navegador/app. Por encima, se deshabilita la apertura con texto y se conserva el mensaje completo para copiarlo y pegarlo manualmente; no se recortan productos ni se divide la solicitud sin avisar.

El sitio no puede verificar que WhatsApp esté instalado, que el cliente haya iniciado sesión, que haya enviado el mensaje o que la tienda lo haya recibido. Puede utilizarse WhatsApp Web desde el enlace o contactar manualmente al número indicado. La disponibilidad del portapapeles también depende del navegador/contexto seguro; se conserva la selección manual como respaldo.

## Archivos

- `BloomiFront/src/services/whatsappCheckout.ts`: validación completa, cálculo en centavos, formato COP, mensaje, enlace, límite de longitud y apertura directa.
- `BloomiFront/src/components/store/WhatsAppCheckout.tsx`: botón, icono, mensajes de estado, errores, copia y contacto manual.
- `BloomiFront/src/pages/StorePage.tsx`: integración en el carrito y actualización al abrirlo; controles y persistencia existentes conservados.
- `BloomiFront/src/styles/store.css`: estilos lavanda, hover, foco, estados deshabilitados y alternativas responsive.
- `BloomiFront/src/vite-env.d.ts`, `.env.example` y configuración privada `.env.local`: variable pública del destino.
- `BloomiFront/tests/whatsapp-checkout.test.mjs`: pruebas de solicitud, importes, variantes, fallos y conservación del carrito.
- README y memoria local: estado y continuidad.

No se modificaron API, servicios de inventario, ventas administrativas ni esquema de PostgreSQL para esta funcionalidad. Se reutilizan `cart.ts`, el sistema de variantes y el lector público existente sin reemplazarlos.

## Pruebas

Desde `BloomiFront`:

```powershell
npm run build
node --test tests/whatsapp-checkout.test.mjs tests/recommendations.test.mjs
```

Resultado: build con TypeScript aprobado y **33 pruebas frontend** sin fallos (23 de carrito/WhatsApp y 10 de recomendaciones). Cubren producto simple, tono seleccionado, dos tonos, cuatro artículos, cantidades, carrito vacío, agotados/ocultos/eliminados, sobreexistencia, precios/IDs/cantidades inválidos, centavos, número incorrecto/ausente, emojis y caracteres especiales, privacidad comercial, stock conjunto de Gloss, mensaje largo y apertura bloqueada. También verifican lectura, límites por SKU, eliminación y disponibilidad del carrito existente.

Desde `BloomiBack`, `mvn test`: **52 pruebas aprobadas y 59 integraciones omitidas** por no configurar un esquema aislado. No se ejecutaron migraciones ni pruebas contra la base comercial. Las integraciones completas anteriores pertenecen a la funcionalidad de ventas, no a esta tarea.

La comprobación en Chrome aislado aprobó carrito y botón a 1440/768/390/320 px, teclado, actualización al abrir, pedido con cuatro SKU y dos tonos, subtotal correcto, conservación local, bloqueo de ventanas, copia y portapapeles denegado, recuperación de API, faltantes/agotados/sobreexistencia, 70 artículos y configuración ausente/incorrecta. Las aperturas y el portapapeles se simularon; no se contactó WhatsApp ni se enviaron mensajes. No se validó una sesión real en la app o WhatsApp Web. Cero escrituras API y cero solicitudes externas a WhatsApp durante estas pruebas. El catálogo público y las 16 tablas comerciales comprobadas permanecieron iguales.

Comprobación manual sugerida: preparar cada caso, abrir la vista previa y revisar líneas/precios. Para probar el enlace real, usar exclusivamente el WhatsApp del negocio y decidir manualmente si enviar el mensaje; no usar números de clientes. Para comprobar falta de configuración, retirar temporalmente solo la variable pública en un entorno de pruebas y reiniciar Vite; debe aparecer el aviso sin abrir conversación.

## Evolución futura

La solicitud de WhatsApp vive en una utilidad y un componente separados de la persistencia del carrito. Una futura pasarela podrá consumir el mismo carrito, validando precios, stock y totales en servidor, y creando pedidos/pagos con confirmación independiente. Esta etapa no añade tablas ni supone que abrir el enlace confirme una transacción. Pedidos persistidos, reservas, calendario de entrega y pagos electrónicos siguen pendientes.
