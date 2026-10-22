# Ventas administrativas de Bloomi Bunny

El panel **Administración → Ventas**, ruta `/admin/ventas`, registra ventas confirmadas recibidas por WhatsApp, Instagram u otro canal. Guarda cómo pagó el cliente; no procesa cobros ni reembolsos.

## Registrar una venta

1. Seleccionar **Registrar venta**, el canal y el pago recibido. El cliente es opcional; por defecto se registra Consumidor final. Los contactos se gestionan en Clientes.
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

El registro usa un bloqueo PostgreSQL por solicitud, unicidad del identificador y bloqueos de productos en orden de ID. La anulación bloquea la venta y los productos. Estas medidas permiten reintentos y evitan vender por encima del stock o devolver dos veces la misma venta.

La migración aditiva `BloomiBack/src/main/resources/db/migrations/007_admin_sales.sql` agrega snapshots, auditoría, canal, anulación y unicidad. No modifica productos ni reconstruye costos anteriores. En bases existentes, respaldar y aplicar el SQL antes de iniciar la nueva versión. El historial anterior conserva costos desconocidos y se señala cuando faltan snapshots.

## Comprobaciones

El 22 de octubre de 2026 pasaron 39 pruebas backend: 15 de acceso/validación, 14 de productos y 10 de ventas contra PostgreSQL en un esquema aislado. Cubren stock insuficiente y rollback, precios/costos históricos, descuentos inválidos, SKU repetidos, anulación y repetición, costos desconocidos, venta simultánea de la última unidad, reintentos concurrentes, venta mientras otra se anula y filtros por fecha de Colombia.

Las pruebas de integración se habilitan con `BLOOMI_TEST_DB_URL`, `BLOOMI_TEST_DB_USERNAME` y `BLOOMI_TEST_DB_PASSWORD`. La URL debe terminar exactamente en `?currentSchema=bloomi_sales_test_...`, y el esquema de prueba debe existir previamente. Esas pruebas usan `create-drop` y limpian sus propias tablas: **usar exclusivamente un esquema nuevo de pruebas**, sin datos comerciales. No guardar credenciales en el repositorio. Con esas variables, ejecutar `mvn test`; sin ellas se omiten las 10 pruebas de integración.

Las comprobaciones de navegador usan respuestas simuladas para registrar, reintentar y anular, sin crear ventas reales. Validan tonos, cliente, canal, pago, descuento, error tras guardado, anulación con motivo, acceso de clientes denegado y tamaños 320/390/1440 px. La compilación frontend está aprobada. La actualización posterior del [dashboard](DASHBOARD.md) resolvió los errores anteriores de TypeScript y eliminó `noCheck=true`; se mantiene el aviso por tamaño del bundle. El conjunto completo ahora tiene 49 pruebas, incluidas 15 de integración que se omiten sin las variables de pruebas.
