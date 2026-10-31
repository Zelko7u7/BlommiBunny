# Compras y gastos administrativos

Compras registra mercancía recibida y Gastos registra gastos realizados del negocio. Las dos secciones requieren ADMIN y permiten consultar historial, detalle, fechas, referencias, notas y anulación con motivo. Movimientos de dinero y saldos quedan pendientes.

## Registrar una compra

En `/admin/compras`, seleccionar un proveedor activo, fecha de recepción, referencia de factura opcional y los productos/tonalidades recibidos. Cada línea exige cantidad positiva y costo unitario de esa compra, con hasta dos decimales. Se permite costo cero si ese es el valor real. Una referencia aparece una sola vez y se admiten hasta 100 líneas.

Confirmar suma las unidades al inventario y genera movimientos vinculados al número de compra y a quien registra. Los productos ocultos o agotados pueden recibirse; los eliminados o con datos provisionales no. Para una referencia nueva, crear primero el producto en Inventario con stock cero. No registrar como nueva recepción mercancía que ya se sumó al stock inicial o por un ajuste manual.

La compra conserva nombre del proveedor y nombre/código/tono de cada producto tal como estaban al registrar. Sus costos y totales no cambian al editar el catálogo después. **El costo de referencia del producto no se sobrescribe automáticamente.** Esta primera versión no implementa valoración por lotes ni costo promedio: el costo de referencia puede revisarse desde Inventario, y las ventas siguen guardando ese costo como snapshot cuando se registran. La compra tampoco implica un pago registrado ni una cuenta por pagar.

## Registrar un gasto

En `/admin/gastos`, indicar fecha, categoría, concepto, importe positivo y medio de pago. La referencia del comprobante y las notas son opcionales. Las categorías disponibles son Publicidad, Empaques, Transporte, Servicios, Comisiones y Otros.

La referencia es texto: no hay carga de archivos adjuntos en esta versión. Los gastos no alteran stock. La mercancía para vender se registra en Compras, evitando duplicar su importe como gasto. Los totales de esta sección son gastos confirmados; el margen de productos del dashboard mantiene su definición anterior y no descuenta estos gastos ni representa utilidad neta.

## Historial y correcciones

Ambos historiales permiten buscar, filtrar fechas inclusivas de Colombia y estado, con páginas de 20 registros. Los filtros se conservan en la URL. El importe confirmado y los contadores se calculan sobre todas las páginas coincidentes; las anulaciones se excluyen del importe. Se actualizan cada 15 segundos y al recuperar el foco.

Los registros confirmados no se editan ni borran físicamente. Para corregirlos, anular con un motivo de 5 a 500 caracteres y registrar la operación correcta. Se conserva quién registró/anuló, cuándo y por qué.

- Anular compra retira todas sus cantidades bajo bloqueo de inventario. Si una referencia no tiene stock suficiente, se rechaza toda la anulación sin cambios parciales. No se rastrea de qué lote provienen las unidades disponibles.
- Anular gasto conserva el detalle y lo excluye de totales. No modifica inventario.
- Repetir una anulación no vuelve a aplicar sus efectos. Ninguna anulación registra devoluciones de dinero.

El registro utiliza un UUID de solicitud y una huella de sus datos. Si la respuesta se pierde, el formulario congela los datos y permite reintentar la misma operación. El backend recupera el registro previo sin duplicarlo; reutilizar el UUID con otro contenido se rechaza. También se puede cerrar y consultar el historial antes de crear otro registro.

## API y migración

Para `purchases` y `expenses`:

- `GET /api/{tipo}`: historial con `q`, `from`, `to`, `cancelled`, `page` y `size` (1–100). Devuelve `content`, paginación y `confirmedTotal`, `confirmedCount`, `cancelledCount`.
- `GET /api/{tipo}/{id}`: detalle mediante DTO.
- `POST /api/{tipo}`: creación validada, UUID obligatorio; identidad derivada de la sesión.
- `POST /api/{tipo}/{id}/cancel`: anulación con motivo.

El antiguo DELETE de gastos se retiró para preservar historial. Los contratos anteriores de compras/gastos se reemplazan por los nuevos DTO y paginación; usar frontend y backend actualizados juntos.

En bases existentes, respaldar y aplicar manualmente `BloomiBack/src/main/resources/db/migrations/008_admin_purchases_expenses.sql` antes de iniciar la nueva versión. Agrega columnas e índices de forma aditiva y deriva la fecha local de compras anteriores; no modifica stock ni costos de productos. No inventa nombres históricos ni medios de pago anteriores. El detalle señala cuando una compra previa no tiene snapshots. Hibernate continúa con `ddl-auto=update` para desarrollo; no ejecuta los SQL automáticamente.

## Validación

El conjunto backend tiene 77 pruebas aprobadas después de la revisión general. Las 14 pruebas PostgreSQL de este módulo cubren snapshots, preservación del costo de referencia, recepciones/reintentos/anulaciones concurrentes, competencia con ventas por las últimas unidades, rollback completo, límites de inventario e importes, gastos sin efecto en stock, filtros y totales entre páginas. Se suman cuatro pruebas de acceso/validación, incluida la retirada del borrado de gastos.

Las 35 pruebas de integración requieren `BLOOMI_TEST_DB_URL`, `BLOOMI_TEST_DB_USERNAME` y `BLOOMI_TEST_DB_PASSWORD`, con un esquema nuevo cuyo nombre empiece por `bloomi_sales_test_`. Usan `create-drop`: no ejecutarlas contra datos comerciales. El procedimiento de configuración es el de [ventas administrativas](VENTAS_ADMIN.md).

Build frontend con tipos aprobado. Las pruebas del navegador usan respuestas simuladas para recibir/anular mercancía, registrar/anular gastos y recuperar respuestas inciertas; verifican formularios, detalles e historiales a 320/390/768/1440 px. Los datos comerciales se compararon antes y después de la migración y pruebas, sin registrar operaciones reales.

## Pendiente: costos variables por resurtido

Decisión de continuidad vigente al cierre del 28 de octubre de 2026. **Todavía no implementada:** la compra actual guarda su costo histórico, pero el inventario no conserva unidades restantes por lote y las ventas utilizan el costo de referencia del producto.

- Permitir que una recepción tenga un costo distinto para el mismo SKU/tono, conservando costo unitario y unidades restantes por recepción/lote.
- Identificar cada recepción por fecha e identificador de compra para mantener la trazabilidad.
- Calcular «Valor del stock a costo de compra» sumando las unidades restantes por su costo de origen. La mercancía nueva aumenta el total por su propio costo y cantidad, sin revalorizar las unidades anteriores.
- Conservar los costos y márgenes de ventas históricas; no sobrescribirlos al recibir mercancía o cambiar un precio de referencia.
- Acordar cómo se asignan unidades de cada lote a una venta. FIFO es una posibilidad, todavía no una decisión. La fecha de compra por sí sola no demuestra qué costo se vendió.
- Antes de migrar, verificar existencias actuales, respaldar y definir el tratamiento del stock previo y de anulaciones, sin inventar procedencias ni costos desconocidos.

Actualización del 30 de octubre: [abonos y autorización de cobros](VENTAS_ADMIN.md#pendiente-prioritario-ventas-a-cuotas-y-abonos) implementados. Los costos por recepción/lote pasan a ser el siguiente pendiente funcional; aún falta acordar el método de asignación. Caja y saldos de dinero continúan aplazados. Las 77 pruebas descritas antes corresponden al cierre de la revisión general; el conjunto posterior a galerías aprobó 89, incluidas 41 PostgreSQL aisladas. Resumen vigente en [revisión técnica](REVISION_PROYECTO.md).
