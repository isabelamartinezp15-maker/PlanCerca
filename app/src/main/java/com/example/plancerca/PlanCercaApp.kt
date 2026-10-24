package com.example.plancerca

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.plancerca.ui.detalle.DetalleScreen
import com.example.plancerca.ui.explorar.ExplorarScreen
import com.example.plancerca.ui.mapa.MapaScreen

// Destinos de la app
data object Explorar
data object Mapa
data object Favoritos
data class DetalleLugar(val lugarId: Int)

private data class Pestana(
    val destino: Any,
    val titulo: String,
    val icono: ImageVector
)

private val pestanas = listOf(
    Pestana(Explorar, "Explorar", Icons.Default.Explore),
    Pestana(Mapa, "Mapa", Icons.Default.Map),
    Pestana(Favoritos, "Favoritos", Icons.Default.Favorite)
)

@Composable
fun PlanCercaApp() {
    val backStack = remember { mutableStateListOf<Any>(Explorar) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                pestanas.forEach { pestana ->
                    NavigationBarItem(
                        // Usa la primera pantalla de la pila, así la pestaña
                        // sigue marcada aunque estés dentro de un detalle
                        selected = backStack.firstOrNull() == pestana.destino,
                        onClick = {
                            backStack.clear()
                            backStack.add(pestana.destino)
                        },
                        icon = { Icon(pestana.icono, contentDescription = pestana.titulo) },
                        label = { Text(pestana.titulo) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavDisplay(
            modifier = Modifier.padding(padding),
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Explorar> {
                    ExplorarScreen(onLugarClick = { id -> backStack.add(DetalleLugar(id)) })
                }
                entry<DetalleLugar> { clave ->
                    DetalleScreen(
                        lugarId = clave.lugarId,
                        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() }
                    )
                }
                entry<Mapa> {
                    MapaScreen(onLugarClick = { id -> backStack.add(DetalleLugar(id)) })
                }
                entry<Favoritos> { PantallaVacia("Favoritos") }
            }
        )
    }
}

@Composable
private fun PantallaVacia(titulo: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}