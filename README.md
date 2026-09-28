# CampusGO 🚀

<div align="center">
  <img src="app/src/main/res/drawable/mascota_campusgo.png" alt="CampusGO Mascota" width="160" />
  
  <p><strong>Plataforma móvil nativa de comercio estudiantil y puntos de encuentro para campus universitarios.</strong></p>
  <p>Desarrollada y respaldada por <strong>Kodex</strong></p>

  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
  [![Android](https://img.shields.io/badge/Android-API%2024%2B%20%7C%20Target%2036-green.svg?style=for-the-badge&logo=android)](https://developer.android.com/)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09.00-blue.svg?style=for-the-badge&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
  [![Supabase](https://img.shields.io/badge/Supabase-Backend%20as%20a%20Service-3ECF8E.svg?style=for-the-badge&logo=supabase)](https://supabase.com/)
  [![Koin](https://img.shields.io/badge/Dependency%20Injection-Koin-orange.svg?style=for-the-badge)](https://insert-koin.io/)
</div>

---

## 📌 Descripción del Proyecto

**CampusGO** es una aplicación móvil nativa para Android diseñada para conectar a la comunidad universitaria mediante el comercio interno organizado, seguro y eficiente. 

A diferencia de un delivery tradicional, CampusGO opera mediante **puntos de encuentro estratégicos dentro del campus** y pagos contra entrega directos (efectivo, Yape, Plin o transferencias). La plataforma divide automáticamente una compra con múltiples puestos en subpedidos individuales y gestiona en tiempo real la preparación, entrega, chats efímeros y moderación comunitaria.

---

## 👥 Roles del Sistema

### 🛒 1. Comprador (Estudiante / Comunidad)
* **Exploración ágil:** Catálogo dinámico con carrusel de flyers promocionales, filtros por categorías (Comidas, Postres, Bebidas, Hamburguesas, Accesorios) y búsqueda en tiempo real.
* **Carrito multipuesto:** Capacidad de agregar productos de diferentes puestos en un solo carrito, calculando subtotales por vendedor y total general.
* **Puntos y horarios de encuentro:** Selección de horarios dinámicos y puntos físicos de entrega dentro de la sede universitaria.
* **Seguimiento en vivo:** Stepper visual del estado de la orden y control independiente de cada subpedido.
* **Favoritos y calificaciones:** Guardado de puestos/productos preferidos y sistema de reseñas con valoración en estrellas.
* **Chat en tiempo real:** Comunicación directa con el vendedor de cada subpedido.

### 🏪 2. Vendedor (Emprendedor Universitario)
* **Gestión de puesto:** Control de estado operativo del puesto (Abierto / Cerrado) y configuración de métodos de pago aceptados (Efectivo, Yape, Plin).
* **Catálogo de productos:** Creación, edición, precios, categorías y gestión de stock.
* **Procesamiento de subpedidos:** Flujo de aceptación, preparación, aviso de llegada al punto de encuentro y confirmación de entrega con código PIN.
* **Estadísticas y finanzas:** Balance de ingresos, pedidos completados y métricas clave del negocio.
* **Perfil de tienda:** Personalización de portada, logo y descripción comercial.

### 🛡️ 3. Administrador (Supervisión y Auditoría)
* **Auditoría de usuarios:** Consulta detallada de perfiles de compradores y vendedores.
* **Puntos de encuentro:** Creación, habilitación y administración de ubicaciones oficiales de entrega en el campus.
* **Aprobación de puestos:** Validación y autorización de solicitudes de nuevos emprendedores.
* **Sistema disciplinario de 5 Strikes:**
  - Emisión de advertencias y llamadas de atención estructuradas con motivo formal.
  - Indicador visual unificado en perfiles de usuario (`X/5 strikes`).
  - **Suspensión automática** al alcanzar el tope de 5 strikes con bloqueo de emisión adicional de faltas.
  - Opción administrativa de reactivación de cuenta (con reinicio de strikes a cero).
* **Métricas y telemetría:** Registro de volumen de ventas, tiempos de entrega y salud operativa del campus.

---

## 🛠️ Arquitectura y Tecnologías

El proyecto sigue los principios de **Clean Architecture** y arquitectura reactiva moderna en Android:

```text
app/src/main/java/com/example/campusgo/
├── core/                  # Inyección de dependencias (Koin), Notificaciones y Red
├── data/                  # Implementación de repositorios y fuentes de datos
├── domain/                # Modelos de negocio, Casos de uso (UseCases) y Contratos
├── features/              # Pantallas y ViewModels por módulo funcional
│   ├── admin/             # Panel de administración, auditoría de usuarios y métricas
│   ├── auth/              # Inicio de sesión, registro institucional, OTP y recuperación
│   ├── buyer/             # Catálogo, carrusel de flyers, perfil y favoritos
│   ├── cart/              # Carrito multipuesto y pantalla de checkout
│   ├── chat/              # Chat efímero entre comprador y vendedor
│   ├── seller/            # Dashboard del vendedor, pedidos y estadísticas
│   └── tracking/          # Seguimiento de subpedidos y confirmación de entrega
├── theme/                 # Diseño de colores, tipografía y formas Material 3
└── ui/                    # Componentes visuales reutilizables, CodeSlots y VideoSplashScreen
```

### Stack Tecnológico:
* **Lenguaje:** Kotlin 2.0+ con Coroutines y Kotlin Flows.
* **UI Toolkit:** Jetpack Compose con Material Design 3 y Navigation Compose.
* **Inyección de Dependencias:** Koin.
* **Backend & Base de Datos:** Supabase (PostgreSQL, Row Level Security, Realtime websockets y Auth).
* **Media & Video:** Media3 ExoPlayer (para el `VideoSplashScreen` de inicio rápido).
* **Carga de Imágenes:** Coil Compose.

---

## 🚀 Requisitos y Configuración Local

### Requisitos previos:
* Android Studio Iguana / Ladybug o superior.
* JDK 17 o superior.
* Dispositivo físico o Emulador Android con API 24 o superior (Recomendado API 34+).

### Configuración del entorno:
1. Clona el repositorio:
   ```bash
   git clone https://github.com/jhoecornelio423-lang/CampusGO-Android.git
   cd CampusGO-Android
   ```
2. Crea el archivo `local.properties` en la raíz del proyecto (si no existe) y define la ruta del SDK y las credenciales de Supabase:
   ```properties
   sdk.dir=C\:\\Users\\TuUsuario\\AppData\\Local\\Android\\Sdk
   SUPABASE_URL=https://tu-proyecto.supabase.co
   SUPABASE_KEY=tu-anon-key-de-supabase
   ```

### Compilar y Ejecutar Pruebas:
* **Compilación Debug:**
  ```bash
  ./gradlew assembleDebug
  ```
* **Ejecutar Suite de Pruebas Unitarias:**
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## 🔒 Privacidad, Moderación y Políticas

* **Contenido Generado por Usuarios (UGC):** Implementación integral de reportes de usuarios, sistema de strikes y suspensión de cuentas según los estándares exigidos por las políticas de Google Play Store.
* **Manejo de Datos Seguros:** Comunicación cifrada de extremo a extremo mediante HTTPS/WSS con Supabase RLS (Row Level Security) garantizando el aislamiento de datos por usuario y rol.
* **Cero Comisiones en Bienes Físicos:** Modelo de pago directo y contra entrega en el campus universitario sin comisiones abusivas de pasarelas digitales.

---

## 📄 Licencia y Créditos

Este software ha sido diseñado y desarrollado por el equipo de ingeniería de **Kodex**.  
Todos los derechos reservados © 2026 **Kodex**.
