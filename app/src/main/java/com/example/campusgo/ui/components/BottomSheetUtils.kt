package com.example.campusgo.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * Interceptor para eliminar de forma definitiva el bug de rebote / oscilación infinita
 * (flickering / jumping / bouncing) en ModalBottomSheet de Jetpack Compose (Material 3).
 *
 * CAUSAS DEL BUG EN ANDROID:
 * 1. Conflicto de física de scroll anidado (Nested Scrolling): Cuando una lista (LazyColumn o
 *    Column con verticalScroll) alcanza sus límites superior o inferior, cualquier velocidad
 *    residual (fling) no consumida se propaga a través de onPostFling hacia el resorte
 *    (spring physics) del ModalBottomSheet. El resorte sobrepasa el ancla, rebota, y reinicia
 *    el ciclo indefinidamente.
 * 2. Recálculo dinámico de insets: Aplicar `navigationBarsPadding()` dentro del contenido de un
 *    ModalBottomSheet provoca que los márgenes se recalculen continuamente a medida que la hoja
 *    se desplaza sobre la barra de navegación del sistema, cambiando la altura del layout y
 *    forzando un bucle de recomposición y re-anclaje.
 *
 * SOLUCIÓN:
 * - onPostFling absorbe el 100% de la energía residual en los extremos (retornando 'available').
 * - onPreFling neutraliza la inercia ascendente cuando el contenido ya se encuentra en el tope.
 * - Centraliza insets estables a nivel de ventana con [campusBottomSheetWindowInsets].
 */
class BottomSheetFlingInterceptor : NestedScrollConnection {
    override suspend fun onPreFling(available: Velocity): Velocity {
        // Si el usuario lanza hacia arriba con alta velocidad cuando ya no hay más espacio,
        // neutralizamos el exceso para evitar que la hoja sobrepase su límite y vibre.
        return if (available.y < 0) {
            available
        } else {
            Velocity.Zero
        }
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        // Al consumir toda la velocidad residual en los límites de la lista,
        // evitamos que el resorte del ModalBottomSheet reciba energía cinética y empiece a rebotar.
        return available
    }
}

/**
 * Retorna una instancia recordada de [BottomSheetFlingInterceptor].
 */
@Composable
fun rememberBottomSheetFlingInterceptor(): NestedScrollConnection {
    return remember { BottomSheetFlingInterceptor() }
}

/**
 * Modifier de utilidad para aplicar el interceptor de rebotes directamente a cualquier
 * contenedor con scroll (LazyColumn o Column con verticalScroll).
 */
@Composable
fun Modifier.preventBottomSheetBounce(): Modifier {
    val interceptor = rememberBottomSheetFlingInterceptor()
    return this.nestedScroll(interceptor)
}

/**
 * Insets estables recomendados para [androidx.compose.material3.ModalBottomSheet].
 * Evita la oscilación por recálculo dinámico de la barra de navegación.
 */
@OptIn(ExperimentalMaterial3Api::class)
val campusBottomSheetWindowInsets: @Composable () -> WindowInsets = {
    BottomSheetDefaults.windowInsets
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun campusBottomSheetWindowInsets(): WindowInsets {
    return BottomSheetDefaults.windowInsets
}
