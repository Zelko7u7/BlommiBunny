# Dashboard administrativo

En `/admin`, el resumen muestra ventas e inventario consultados en la base de datos. No contiene porcentajes ni series de ejemplo. Requiere una sesión ADMIN.

Actualización del 30 de octubre de 2026: [abonos y autorización de cobros](VENTAS_ADMIN.md#abonos-saldo-y-autorización-de-cobros) implementados. El Dashboard separa cobros autorizados, abonos y saldo del importe vendido. Siguen pendientes los [costos por lote de recepción](COMPRAS_GASTOS.md#pendiente-costos-variables-por-resurtido); el margen de productos sigue sin equivaler a utilidad neta.

## Cobros, abonos y saldo

- **Cobros autorizados de hoy/del mes:** total de ventas vigentes completamente pagadas y con autorización explícita vigente, por fecha de autorización en Colombia. Registrar una venta o completar sus abonos no las incluye automáticamente.
- **Abonos registrados del mes:** pagos no anulados de ventas vigentes por fecha real del pago, aunque la venta aún no esté autorizada. Incluye parciales y pagos completos. No sumar este indicador a los cobros autorizados: pueden representar el mismo dinero.
- **Saldo pendiente total:** suma del saldo de todas las ventas vigentes con pagos revisados, independiente del mes. Un aviso identifica las ventas anteriores cuyos pagos siguen sin revisar; estas no se incluyen inventando una deuda.
- Anular un abono recalcula el saldo y revoca la autorización. Anular una venta la excluye de los indicadores; sus abonos se conservan como historial, sin efectuar reembolsos. Retirar/reautorizar un cobro cambia su inclusión y periodo vigente; los eventos auditados conservan el historial.

Los siguientes indicadores conservan su cálculo por fecha de venta y ahora se rotulan **Importe vendido** para distinguirlos del dinero recibido. Incluyen ventas con pagos pendientes; sus costos históricos, margen, gráficos y ranking no dependen de la autorización del cobro. El endpoint incorpora `authorizedCollectionsToday`, `authorizedCollectionsMonth`, `recordedPaymentsMonth`, `outstandingBalance` y `unreviewedPayments`.

## Indicadores

- **Ventas de hoy y del mes:** suma de los totales después de descuentos, con número de ventas confirmadas. Las anuladas quedan excluidas.
- **Costo vendido del mes:** suma de cantidad por costo unitario histórico guardado en cada línea de venta. Cambiar el costo actual de un producto no altera este cálculo.
- **Margen de ventas del mes:** total vendido menos costo vendido. No descuenta gastos generales y no representa utilidad neta.
- Si alguna línea del mes carece de costo histórico, el costo completo y el margen muestran **Por confirmar**. Se indica el costo conocido parcial y cuántas ventas/unidades tienen datos pendientes. Un costo confirmado de cero sí es válido.
- **Stock bajo:** referencias no eliminadas con existencias positivas menores o iguales a su mínimo. **Agotadas:** stock cero. Incluyen referencias ocultas en tienda para poder reponerlas. Las tonalidades se cuentan individualmente.

Los periodos se calculan en `America/Bogota`, desde medianoche local hasta el inicio exclusivo del día siguiente. El mes termina en el día actual. El reloj del servidor determina la fecha; la zona del navegador no cambia los periodos.

## Gráficos y navegación

El gráfico diario cubre 30 días; el mensual, siete meses hasta hoy. Los periodos sin ventas aparecen en cero. Ambos incluyen una tabla con las cifras y un enlace al historial filtrado por fechas y ventas confirmadas.

El ranking muestra hasta cinco referencias/tonalidades por unidades vendidas este mes, usando nombres y códigos guardados al vender. Sus enlaces abren el historial del mes buscando ese código. Las anulaciones también se excluyen del ranking.

Las alertas muestran hasta ocho referencias, primero las de menor stock. Sus enlaces abren inventario buscando el código; los contadores abren `?stock=low` o `?stock=out`. El inventario permite combinar búsqueda y existencias, conserva los filtros en la URL y calcula sus totales sobre todos los resultados coincidentes. La revisión general corrigió el límite anterior de 100 filas: ahora el frontend consulta todas las páginas del listado.

Los diez últimos movimientos muestran tipo, referencia, cantidad, stock resultante y fecha reales. Registrar venta abre el formulario existente; el historial y los clientes tienen accesos directos.

El resumen se consulta cada 15 segundos mientras está visible, al volver a la ventana y con **Actualizar**. Crear/anular ventas o modificar productos invalida sus datos. Un fallo de consulta muestra error y reintento, sin presentar ceros como si fueran resultados.

## Comprobaciones

Cinco pruebas unitarias verifican periodos continuos, límites de Colombia, costo histórico y costos pendientes. Cinco pruebas PostgreSQL adicionales verifican descuentos, exclusión de anulaciones, snapshots, ranking, límites diarios/mensuales, filtros de alertas y registro/anulación con devolución de inventario. Se ejecutan con las mismas variables y restricciones de esquema aislado descritas en [ventas administrativas](VENTAS_ADMIN.md). No ejecutar `create-drop` contra datos comerciales.

El backend no requiere migraciones nuevas para este cambio; utiliza las ventas con snapshots del módulo administrativo. Frontend y backend deben usar la versión actual para compartir el nuevo contrato de `GET /api/dashboard`.
