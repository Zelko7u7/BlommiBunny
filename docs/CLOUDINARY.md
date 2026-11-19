# Fotografías desde administración

## Catálogo existente en Cloudinary

Las fotografías de productos y las opciones de colección utilizan enlaces HTTPS de Cloudinary. Los originales en `public/products/` se conservan como respaldo; los banners y logotipos mantienen sus recursos propios. Los productos sin fotografía siguen sin ella: no se asignan imágenes de otras referencias.

`tools/migrate_product_images_cloudinary.py` permite migrar las fotos existentes por etapas: `prepare`, `upload`, `apply` y `verify`. Lee las referencias actuales de PostgreSQL y la colección, además de los archivos de productos publicados. No reimporta Excel ni modifica precios, existencias, tonos o descripciones. Los planes, comprobantes de subida y respaldos permanecen en `.local/cloudinary-catalog-migration/`, fuera de Git.

Las subidas se firman desde el auxiliar privado con identificadores derivados del contenido y `overwrite=false`; permiten reanudar sin duplicar archivos. Se organizan en `bloomibunny/products/catalog`, conservando el nombre descriptivo y el formato original, incluido el AVIF ya existente. Antes de aplicar los enlaces se comprueban HTTP, tipo y SHA-256 del archivo descargado, y se crea un respaldo PostgreSQL verificado. Una transacción bloquea las filas/tablas necesarias y rechaza un plan si los productos cambiaron desde su preparación. Se conservan portada, orden de secundarias y separación por SKU; se incrementa la versión para impedir que un formulario anterior sobrescriba los enlaces nuevos. También se actualiza la colección del frontend, conservando una copia privada.

No ejecutar de nuevo los importadores históricos para restaurar rutas locales. Para continuar una migración interrumpida, conservar su plan y ejecutar la etapa pendiente. La verificación comprueba que solo cambiaron los enlaces y versiones esperados y que los originales y demás tablas comerciales siguen intactos. Las nuevas fotos subidas desde Inventario continúan utilizando el endpoint administrativo habitual.

## Configurar Cloudinary en local

En la consola de Cloudinary, abre Settings → API Keys. Consulta Cloud name, API Key y API Secret. No pegues el secreto en conversaciones ni lo añadas al frontend.

Completa el archivo privado `.local/cloudinary.properties` en la raíz del proyecto:

```properties
cloudinary.enabled=true
cloudinary.cloud-name=TU_CLOUD_NAME
cloudinary.api-key=TU_API_KEY
cloudinary.api-secret=TU_API_SECRET
```

Sustituye los valores de ejemplo, sin comillas. Este archivo está excluido de Git por estar dentro de `.local/`. Reinicia el backend después de cambiarlo. Su ruta predeterminada supone iniciar Java/Maven desde `BloomiBack`; `BLOOMI_MEDIA_CONFIG` permite indicar otra ubicación privada.

No se necesita un upload preset: el servidor firma cada subida. El navegador envía el archivo al backend autenticado; las claves permanecen en el servidor. Documentación oficial: [firma de subidas](https://cloudinary.com/documentation/authentication_signatures).

## Añadir fotos

1. Entra como administradora y abre Inventario → Nuevo producto, o edita uno existente.
2. En Fotografías del producto pulsa Seleccionar fotografías. Puedes seleccionar varias.
3. Espera a que termine la subida. La primera fotografía es la portada; puedes cambiarla, ordenar las secundarias o retirar fotos.
4. Guarda el producto para aplicar su portada y galería.

Se aceptan JPG, PNG y WebP sin animación, hasta 5 MB por foto, 20 megapíxeles y 10000 píxeles por lado. Cada producto admite una portada y hasta 20 secundarias. Las fotos existentes y las opciones de colección/enlaces siguen disponibles en el desplegable del formulario.

Si una subida falla, las fotos correctas se conservan. Reintenta las pendientes o retíralas antes de guardar. El formulario bloquea el guardado y cierre durante una subida activa. Subir una foto no crea ni actualiza productos, existencias, ventas o precios.

Los archivos quedan en Cloudinary con identificadores únicos bajo `bloomibunny/products/`. Retirar una foto del formulario o cancelar el producto no borra su archivo remoto: podría estar utilizado en otro producto. La limpieza de archivos sin uso se realiza por separado; no hay eliminación automática.

## Entornos

Con `cloudinary.enabled=false` hay almacenamiento de desarrollo en `.local/product-images`, fuera del compilado. Debe conservarse esa carpeta para mantener disponibles sus enlaces. No se deben publicar enlaces locales al desplegar.

Para un alojamiento futuro, configura en el backend `CLOUDINARY_ENABLED=true`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET`. Nunca uses prefijos `VITE_` para estos secretos. Si Cloudinary está activado pero falla, se informa del error y no se cambia silenciosamente a almacenamiento local.

En Heroku se rechaza la subida local sin Cloudinary: [su sistema de archivos es efímero](https://help.heroku.com/K1PPS2WM/why-are-my-file-uploads-missing-deleted-from-the-application). Los créditos ya fueron confirmados por la propietaria; el despliegue se realizará en una etapa posterior. Migrar fotografías no despliega la aplicación.

## API

- `POST /api/media/images`: multipart con campo `file`, solo ADMIN. Responde 201 con `imageUrl`, `contentType` y `size`.
- `GET /api/store/media/{nombre}`: lectura pública de fotos locales, nombres UUID y extensiones permitidas. Las fotos de Cloudinary se leen directamente desde su URL HTTPS.

El servidor comprueba tamaño, formato y dimensiones antes de guardar. Las respuestas de error no incluyen credenciales ni mensajes internos del proveedor. Las referencias a portada/galería utilizan el contrato de producto existente, sin cambios de esquema de base de datos ni dependencias nuevas.
