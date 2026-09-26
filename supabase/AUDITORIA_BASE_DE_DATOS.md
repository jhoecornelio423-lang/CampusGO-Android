# Auditoría de Base de Datos y Notas de Migración - CampusGO

**Fecha:** 25 de Septiembre de 2026  
**Ubicación:** `supabase/AUDITORIA_BASE_DE_DATOS.md`  
**Estado:** Documento de referencia técnica para futuros cambios y migración a VPS.

---

## 1. Inventario y Diagnóstico de Tablas (20 Tablas Públicas)

Se analizaron las 20 tablas de la base de datos PostgreSQL en Supabase contrastadas contra el código Kotlin de la aplicación Android (`app/src/main/java/com/example/vallego`).

| Tabla | Columnas | Registros | Uso en App Móvil | Observaciones / Estado |
| :--- | :---: | :---: | :--- | :--- |
| `profiles` | 23 | 5 | **Activa (Central)** | Monolito: mezcla usuario y datos comerciales de la tienda. |
| `products` | 15 | 15 | **Activa (Catálogo)** | Almacena `image_url` directamente. |
| `orders` | 14 | 55 | **Activa (Pedidos)** | Conserva `seller_id` y otros campos redundantes de la V1. |
| `sub_orders` | 18 | 55 | **Activa (Subpedidos)** | Eje multi-tienda. Estados en español vs ENUM en inglés. |
| `order_items` | 7 | 58 | **Activa (Detalle)** | Le falta índice en `product_id`. |
| `order_messages` | 7 | 12 | **Activa (Chat)** | Mensajería efímera vinculada a subpedidos. |
| `campus_meeting_points` | 8 | 5 | **Activa (Puntos)** | Gestionada por admin y seleccionada por vendedores. |
| `categories` | 5 | 7 | **Activa (Categorías)** | Gestión de categorías de productos. |
| `reviews` | 7 | 7 | **Activa (Reseñas)** | Tiene triggers duplicados de recálculo de rating. |
| `seller_applications` | 19 | 2 | **Activa (Postulaciones)**| Flujo de solicitud y aprobación de vendedores. |
| `profile_warnings` | 6 | 2 | **Activa (Moderación)** | Clave foránea apunta a tabla obsoleta de soporte. |
| `order_incidents` | 12 | 2 | **Activa (Reportes)** | `sub_order_id` es `text` sin FK ni índices secundarios. |
| `stock_logs` | 6 | 142 | **Activa (Auditoría)** | Usada por 6 RPCs PL/pgSQL atómicos para control de stock. |
| `favorites` | 4 | 7 | **Sin uso actual** | Registros antiguos de prueba. No hay UI de favoritos aún. |
| `product_images` | 5 | 9 | **Obsoleta** | Reemplazada por `products.image_url`. |
| `product_reports` | 8 | 0 | **Obsoleta** | Absorbida por `order_incidents`. |
| `push_tokens` | 6 | 3 | **Sin uso actual** | La app usa servicio foreground con polling PostgREST. |
| `support_tickets` | 12 | 4 | **Obsoleta** | Soporte migró a correo institucional (`soporte@kodexti.com`). |
| `support_events` | 6 | 12 | **Obsoleta** | Vinculada a tickets de soporte. |
| `support_messages` | 8 | 16 | **Obsoleta** | Vinculada a tickets de soporte. |

---

## 2. Tablas en Pausa o Candidatas a Deprecación

Si en los próximos cambios se implementan nuevas funciones, tener en cuenta:

1. **`favorites`:**
   * La tabla está bien construida (`id`, `user_id`, `product_id`, `created_at` con constraint único `(user_id, product_id)` y `CASCADE`).
   * Si se decide reactivar la función de "Favoritos" en la app móvil, esta tabla está lista para usarse.
2. **`product_images`:**
   * Si en el futuro se desea soportar galería múltiple de fotos por producto, esta tabla puede reactivarse. Si la app continuará usando una sola foto por producto, conviene eliminarla al migrar.
3. **`support_tickets`, `support_events`, `support_messages`:**
   * No se consumen desde la app. Si en el futuro se quiere un centro de soporte dentro de la aplicación móvil (en vez de soporte por correo), se puede rehacer o limpiar. Por ahora añade sobrecarga de triggers innecesarios.
4. **`product_reports`:**
   * Totalmente sustituida por `order_incidents`. Se recomienda eliminar.
5. **`push_tokens`:**
   * Útil únicamente si se implementa Firebase Cloud Messaging (FCM) con notificaciones push estándar. Actualmente la app usa `ValleGoPushService` en primer plano.

---

## 3. Discrepancias Técnicas y Claves Foráneas a Corregir

### A. `order_incidents` (Prioridad Alta)
* **Problema:** La columna `sub_order_id` es de tipo `text` y **no tiene Foreign Key** hacia `sub_orders(id)`.
* **Consecuencias:**
  1. Si se borra un subpedido, el incidente queda huérfano.
  2. PostgREST no puede resolver relaciones anidadas tipo `select=*,sub_orders(*)`.
* **Corrección futura:**
  ```sql
  ALTER TABLE order_incidents 
    ALTER COLUMN sub_order_id TYPE uuid USING sub_order_id::uuid,
    ADD CONSTRAINT fk_order_incidents_sub_order 
    FOREIGN KEY (sub_order_id) REFERENCES sub_orders(id) ON DELETE SET NULL;
  ```

### B. `profile_warnings`
* **Problema:** La columna `ticket_id` tiene FK hacia `support_tickets(id)`.
* **Corrección futura:** Añadir `incident_id uuid REFERENCES order_incidents(id) ON DELETE SET NULL` para relacionar directamente las sanciones con los reportes de incidentes.

### C. Redundancias entre `orders` y `sub_orders`
* **`orders.seller_id`:** Residuo de la versión monotienda. En CampusGO una orden puede abarcar múltiples vendedores (`sub_orders`).
* **`meeting_point_id`:** En `orders` está como `varchar` y en `sub_orders` como `uuid`. Debe estandarizarse a `uuid REFERENCES campus_meeting_points(id)`.
* **Campos duplicados:** `meeting_point_name`, `scheduled_time`, `notes`, `payment_method` existen en ambas tablas.

### D. Inconsistencia de Estados
* `orders.status`: Usa enum PostgreSQL `order_status` (`pending`, `accepted`, `preparing`, `ready`, `completed`, `cancelled`).
* `sub_orders.status`: Usa `varchar` con valores en español (`PENDIENTE`, `ACEPTADO`, `EN_PREPARACION`, `LISTO`, `COMPLETADO`, `RECHAZADO`).
* Al unificar, considerar si se estandariza todo a un enum o se mantiene varchar para no quebrar serialización Kotlin.

---

## 4. Normalización de `profiles`

Actualmente `profiles` tiene 23 columnas que combinan:
* **Datos de Usuario Universitario:** `id`, `full_name`, `phone`, `role`, `campus`, `student_code`, `avatar_url`, `push_notifications_enabled`, `suspension_reason`.
* **Datos del Puesto / Tienda:** `business_name`, `business_category`, `business_description`, `business_location`, `open_time`, `close_time`, `banner_url`, `accepting_orders`, `show_in_explore`, `business_status`, `supported_meeting_points`, `rating_average`.

**Opciones de diseño:**
* **Opción 1 (Pragmática):** Mantener `profiles` unificada para no tocar los DTOs de Kotlin (`ProfileDto`, `UserProfile`, etc.).
* **Opción 2 (Normalizada):** Separar en `profiles` y `seller_stores` (`seller_id REFERENCES profiles(id) PRIMARY KEY`). En PostgreSQL se puede crear una vista `profiles` para mantener compatibilidad hacia atrás.

---

## 5. Triggers e Índices a Optimizar

1. **Triggers Duplicados en `reviews`:**
   * `on_review_changed` y `trg_recalculate_seller_rating` ejecutan ambas la misma función en cada reseña. Se debe conservar solo una.
2. **Triggers con Llamadas Externas (`pg_net`):**
   * `trg_order_created_push_fn` y similares invocan `https://dqjuifzsowwrrfppczsj.functions.supabase.co/send-push`.
   * En el VPS, si no está `pg_net` o la URL deja de existir, provocará timeouts de 5s o errores al crear pedidos. Deben desactivarse o apuntar al webhook del VPS.
3. **Índices Faltantes:**
   ```sql
   CREATE INDEX idx_order_incidents_status ON order_incidents(status);
   CREATE INDEX idx_order_incidents_reported ON order_incidents(reported_user_id);
   CREATE INDEX idx_order_incidents_suborder ON order_incidents(sub_order_id);
   CREATE INDEX idx_order_items_product_id ON order_items(product_id);
   ```
4. **Índice Duplicado en `orders`:**
   * Eliminar `idx_orders_buyer_id` (es idéntico a `idx_orders_buyer` sobre `orders(buyer_id)`).

---

## 6. Recomendaciones para el VPS Propio

1. **Despliegue Recomendado: Supabase Self-Hosted (Docker Compose)**
   * La app Android depende de `postgrest-kt`, `gotrue-kt` y `storage-kt`.
   * Montar Supabase Self-Hosted en el VPS permite mantener el 100% de la funcionalidad de la app sin tener que programar un backend REST intermedio ni modificar los repositorios en Android. Solo se cambia `supabaseUrl` y la anon key.
2. **Storage Buckets Necesarios:**
   * `avatars`
   * `banners`
   * `product-images`
