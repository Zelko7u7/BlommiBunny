# Ventas administrativas de Bloomi Bunny

El panel **Administración → Ventas**, ruta `/admin/ventas`, registra ventas de contado o con pago pendiente recibidas por WhatsApp, Instagram u otro canal. Desde el 30 de octubre de 2026 permite registrar abonos, consultar el saldo y autorizar expresamente la inclusión en cobros confirmados. No procesa pagos ni reembolsos.

## Abonos, saldo y autorización de cobros

El formulario de registro incluye **Estado del pago**: Pago pendiente (selección inicial), Pago parcial / abono inicial y Pago completo. Pendiente guarda la venta sin pagos; Parcial exige un importe mayor que cero y menor que el total; Completo registra el total después del descuento. Para pagos parciales/completos se elige el medio del pago recibido. Venta y pago inicial se guardan en una sola transacción, con un reintento común que evita duplicar pagos y stock. El pago inicial usa el día de registro en Colombia; los pagos posteriores permiten indicar su fecha real desde el detalle.

Toda venta comienza **sin autorización de cobro**, incluso con pago completo. Guardarla descuenta inventario una sola vez y conserva precios, tonos y costos históricos. Cada pago conserva su medio real.

1. Registrar la venta eligiendo su estado de pago e importe recibido, si corresponde, y abrir su detalle.
2. Para pagos posteriores, seleccionar **Registrar abono** e indicar importe, fecha del pago en Colombia y medio de pago; referencia y notas son opcionales. La fecha no puede ser futura ni anterior al día de la venta.
3. Consultar **Importe de la venta**, **Abonos registrados** y **Saldo pendiente**. Los pagos pueden ser parciales; nunca pueden superar el saldo, incluso con abonos simultáneos.
4. Al llegar a saldo cero, seleccionar **Autorizar cobro completo** y confirmar con motivo. Completar el pago no autoriza automáticamente el cobro. La autorización registra responsable, fecha y motivo; también puede retirarse expresamente.

El historial filtra Pendiente, Parcial, Pagada sin autorizar, Cobro autorizado y Pagos por revisar, conservando los filtros en la URL. Las ventas anteriores a la migración quedan con **Pagos por revisar**, sin suponer pagos ni deudas: iniciar su seguimiento con motivo y registrar después únicamente los pagos reales comprobados. La base local no tenía ventas al aplicar esta migración.

Un abono confirmado no se edita ni se borra: **Anular abono** exige motivo y conserva importe, fecha y auditoría. Recalcula el saldo y retira cualquier autorización de cobro. Registrar su reemplazo no vuelve a autorizar la venta. Los abonos y sus correcciones no alteran inventario.

Los reintentos usan el mismo UUID y contenido; el servidor recupera el abono sin duplicarlo. Si la respuesta es incierta, el formulario congela sus datos y ofrece reintentar. Si se cierra, consultar el historial antes de crear otro abono. Reutilizar el UUID con otro contenido se rechaza. La autorización exige la versión consultada para rechazar cambios simultáneos.

Anular una venta conserva sus abonos como historial, retira su autorización, la excluye de los indicadores de cobros/abonos/saldos y devuelve stock una sola vez. No devuelve dinero. Se bloquean nuevos abonos, correcciones y autorizaciones sobre la venta anulada; los reembolsos se gestionan por separado.

### Indicadores y fechas

- **Importe vendido, costo vendido, margen, gráficos y ranking:** ventas vigentes por fecha de venta, incluidas las que aún tienen deuda. No representan dinero cobrado. Los costos históricos y el cálculo de margen no cambian.
- **Cobros autorizados de hoy/del mes:** importe completo de ventas pagadas con autorización vigente, agrupado por fecha de autorización en Colombia. Sin autorización no se incluyen. Retirarla o anular la venta las excluye; reautorizar las sitúa en el nuevo periodo de autorización.
- **Abonos registrados del mes:** pagos no anulados de ventas vigentes por fecha del pago; incluye pagos parciales y completos, aunque falte autorizar la venta. Es un indicador separado: no se suma otra vez a cobros autorizados.
- **Saldo pendiente total:** saldos de todas las ventas vigentes cuyos pagos están revisados, sin limitarlo al mes. Las anteriores sin revisar tienen aviso y se excluyen hasta su revisión.

No se implementan calendario de vencimientos, intereses, cuotas programadas ni movimientos de caja. Cada abono corresponde a un pago real registrado.

## Registrar una venta

1. Seleccionar **Registrar venta**, el canal y el estado de pago. Para un pago parcial, indicar el abono inicial; para parcial/completo, elegir su medio. El cliente es opcional; por defecto se registra Consumidor final. Para ventas con saldo conviene seleccionar el cliente para identificarlas. Los contactos se gestionan en Clientes.
2. Buscar productos por nombre, código o tono y agregarlos. Cada SKU conserva su cantidad y precio. Los productos ocultos en la tienda pueden venderse desde administración; los eliminados, agotados o con datos provisionales no pueden añadirse a una venta nueva.
3. Revisar cantidades, precio por unidad, descuento total y notas opcionales. El descuento no puede superar el subtotal. Los importes admiten hasta dos decimales.
4. Confirmar. El servidor comprueba el stock vigente y guarda venta, líneas y movimientos en una transacción. Seleccionar productos no reserva unidades.

Cada línea conserva nombre, código, tono, precio y costo del momento. La venta conserva cliente, canal y administradora responsable. Cambiar después el producto o el cliente no reescribe estos datos históricos. Si falta confirmar un costo, su margen permanece sin confirmar; no se inventa ese valor.

Si se pierde la respuesta del servidor, **Reintentar la misma venta** utiliza el identificador original y los mismos datos. Si ya se registró, se recupera sin volver a descontar existencias. Antes de cerrar y registrar una venta nueva, revisar el historial para evitar duplicarla manualmente.

## Historial y anulación

El historial tiene búsqueda por número, cliente, producto, código o tono; filtros de fechas de Colombia, canal y estado; y páginas de 20 ventas. El detalle muestra importes, costo registrado y margen de productos, sin descontar gastos generales del negocio.

**Anular venta** exige un motivo y devuelve todas las unidades al inventario. Conserva la venta, quién la anuló y cuándo. Repetir la solicitud no vuelve a devolver stock. La venta anulada se excluye de ingresos confirmados, aunque sus importes originales se mantienen para consulta. La anulación no ejecuta una devolución de dinero.

## API y base de datos

Todos los endpoints requieren ADMIN:

- `GET /api/sales`: historial paginado. Parámetros `q`, `from`, `to`, `channel`, `cancelled`, `page` y `size` (1 a 100). Fechas inclusivas por día en America/Bogota.
- `GET /api/sales/{id}`: detalle con DTO, sin relaciones JPA recursivas.
- `POST /api/sales`: `requestId` UUID, cliente opcional, `paymentMethod`, `channel`, descuento, notas y líneas de producto/cantidad/precio. La identidad de quien registra procede de su sesión.
- `POST /api/sales/{id}/cancel`: motivo de 5 a 500 caracteres; identidad de quien anula derivada de su sesión.
- `POST /api/sales/{id}/payments`: UUID, importe, fecha, medio de pago y referencia/notas opcionales.
- `POST /api/sales/{id}/payments/{paymentId}/cancel`: motivo obligatorio; conserva el abono y revoca la autorización.
- `POST /api/sales/{id}/collection`: `expectedVersion`, `authorized` y motivo obligatorio.
- `POST /api/sales/{id}/payments/review`: versión y motivo para iniciar seguimiento de ventas anteriores.

`GET /api/sales` añade `paymentStatus` (`PENDING`, `PARTIAL`, `PAID`, `AUTHORIZED`, `UNREVIEWED`). Las respuestas incorporan abonos, saldo, versión, autorización e historial de revisiones/autorizaciones. La migración aditiva `010_sale_payments.sql` crea `sale_payment` y `sale_collection_event` y añade revisión/autorización a `sale`. No reconstruye pagos históricos. **Ya aplicada localmente con respaldo privado; no repetir migraciones anteriores.**

`POST /api/sales` admite `paymentStatus` (`PENDING`, `PARTIAL`, `PAID`) e `initialPayment`. Al omitir ambos mantiene el comportamiento anterior (sin pago inicial) y la huella de solicitud anterior para recuperar reintentos. El registro inicial no necesita otra migración.

El registro usa un bloqueo PostgreSQL por solicitud, unicidad del identificador y bloqueos de productos en orden de ID. La anulación bloquea la venta y los productos. Estas medidas permiten reintentos y evitan vender por encima del stock o devolver dos veces la misma venta.

La migración aditiva `BloomiBack/src/main/resources/db/migrations/007_admin_sales.sql` agrega snapshots, auditoría, canal, anulación y unicidad. No modifica productos ni reconstruye costos anteriores. En bases existentes, respaldar y aplicar el SQL antes de iniciar la nueva versión. El historial anterior conserva costos desconocidos y se señala cuando faltan snapshots.

## Comprobaciones

El 30 de octubre se comprobaron 105 pruebas distintas: una ejecución completa de 101 sin omisiones (incluidas 53 PostgreSQL aisladas) y una ejecución posterior de 52 sin base de datos que incorpora cuatro pruebas nuevas de acceso/validación. Las 12 integraciones nuevas cubren pagos parciales/completos, autorización explícita, importes/fechas inválidos, reintentos, concurrencia, anulaciones, revisión histórica, filtros y separación de fechas. Build con tipos aprobado y navegador a 1440/768/390/320 px: abonos, respuesta perdida/reintento, autorización con motivo, corrección/revocación, filtros persistentes y Dashboard sin desbordamiento tras adaptar el gráfico. Nueve escrituras simuladas/interceptadas, cero escrituras comerciales de prueba; datos anteriores intactos.

La mejora posterior del formulario inicial aprobó el conjunto completo de **111 pruebas sin omisiones**, incluidas 59 PostgreSQL aisladas. Añade seis integraciones de pago inicial pendiente/parcial/completo, validaciones con rollback, reintentos y concurrencia. Build con tipos aprobado. Las comprobaciones de navegador también cubren el selector inicial, el saldo, rechazo de un abono excesivo, total después de descuento y reintento de creación.

El 22 de octubre de 2026 pasaron 39 pruebas backend: 15 de acceso/validación, 14 de productos y 10 de ventas contra PostgreSQL en un esquema aislado. Cubren stock insuficiente y rollback, precios/costos históricos, descuentos inválidos, SKU repetidos, anulación y repetición, costos desconocidos, venta simultánea de la última unidad, reintentos concurrentes, venta mientras otra se anula y filtros por fecha de Colombia.

Las pruebas de integración se habilitan con `BLOOMI_TEST_DB_URL`, `BLOOMI_TEST_DB_USERNAME` y `BLOOMI_TEST_DB_PASSWORD`. La URL debe terminar exactamente en `?currentSchema=bloomi_sales_test_...`, y el esquema de prueba debe existir previamente. Esas pruebas usan `create-drop` y limpian sus propias tablas: **usar exclusivamente un esquema nuevo de pruebas**, sin datos comerciales. No guardar credenciales en el repositorio. Con esas variables, ejecutar `mvn test`; sin ellas se omiten las 10 pruebas de integración.

Las comprobaciones de navegador usan respuestas simuladas para registrar, reintentar y anular, sin crear ventas reales. Validan tonos, cliente, canal, pago, descuento, error tras guardado, anulación con motivo, acceso de clientes denegado y tamaños 320/390/1440 px. La compilación frontend está aprobada con comprobación de tipos y carga por pantallas, sin aviso de tamaño. Después de la revisión general, el conjunto completo tiene 77 pruebas, incluidas 35 de integración que se omiten sin las variables de pruebas. Los filtros de ventas se conservan en la URL al recargar.

## Pendiente prioritario: ventas a cuotas y abonos

**Implementado el 30 de octubre de 2026** según las reglas de las secciones iniciales. El título se conserva para mantener enlaces existentes. La lista siguiente es el requerimiento histórico del 28 de octubre, ya atendido para abonos y autorización; costos por lote siguen pendientes.

- Incorporar ventas cuyo importe aún no se haya recibido completo, registrando importe de venta, abonos y saldo por separado.
- No incorporar una venta al total final de cobros/ingresos confirmados hasta que la propietaria autorice expresamente su inclusión mediante un selector de estado o control equivalente. Registrar una venta no la convierte automáticamente en cobrada.
- Registrar un abono tampoco autoriza sumar el importe completo. Antes de implementar, precisar qué indicadores esperan autorización, cómo se presentan ventas frente a cobros y cómo se contabilizan y corrigen los abonos.
- Conservar el historial, los tonos, los precios/costos históricos y las reglas de inventario existentes. No reconstruir supuestos pagos de operaciones anteriores.

Esta prioridad precede a la propuesta de pedidos desde carrito con confirmación por WhatsApp. El alcance posterior de costos por recepción se explica en [Compras y gastos](COMPRAS_GASTOS.md#pendiente-costos-variables-por-resurtido). Las cifras de 77 pruebas anteriores corresponden a la revisión general del 22 de octubre; tras integrar medios, el conjunto completo aprobó 89, incluidas 41 PostgreSQL aisladas. Ver [estado vigente](REVISION_PROYECTO.md).
