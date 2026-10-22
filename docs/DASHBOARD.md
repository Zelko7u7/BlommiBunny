# Dashboard administrativo

En `/admin`, el resumen muestra ventas e inventario consultados en la base de datos. No contiene porcentajes ni series de ejemplo. Requiere una sesión ADMIN.

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

Las alertas muestran hasta ocho referencias, primero las de menor stock. Sus enlaces abren inventario buscando el código; los contadores abren `?stock=low` o `?stock=out`. El inventario permite combinar búsqueda y existencias, conserva los filtros en la URL y calcula sus totales sobre todos los resultados coincidentes. El listado administrativo mantiene su límite previo de 100 filas visibles.

Los diez últimos movimientos muestran tipo, referencia, cantidad, stock resultante y fecha reales. Registrar venta abre el formulario existente; el historial y los clientes tienen accesos directos.

El resumen se consulta cada 15 segundos mientras está visible, al volver a la ventana y con **Actualizar**. Crear/anular ventas o modificar productos invalida sus datos. Un fallo de consulta muestra error y reintento, sin presentar ceros como si fueran resultados.

## Comprobaciones

Cinco pruebas unitarias verifican periodos continuos, límites de Colombia, costo histórico y costos pendientes. Cinco pruebas PostgreSQL adicionales verifican descuentos, exclusión de anulaciones, snapshots, ranking, límites diarios/mensuales, filtros de alertas y registro/anulación con devolución de inventario. Se ejecutan con las mismas variables y restricciones de esquema aislado descritas en [ventas administrativas](VENTAS_ADMIN.md). No ejecutar `create-drop` contra datos comerciales.

El backend no requiere migraciones nuevas para este cambio; utiliza las ventas con snapshots del módulo administrativo. Frontend y backend deben usar la versión actual para compartir el nuevo contrato de `GET /api/dashboard`.
