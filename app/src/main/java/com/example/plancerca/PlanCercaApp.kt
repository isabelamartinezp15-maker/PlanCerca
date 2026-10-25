package com.example.plancerca

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.plancerca.data.FavoritosRepositorio
import com.example.plancerca.data.ResenasRepositorio
import com.example.plancerca.ui.detalle.DetalleScreen
import com.example.plancerca.ui.explorar.ExplorarScreen
import com.example.plancerca.ui.favoritos.FavoritosScreen
import com.example.plancerca.ui.mapa.MapaScreen

// Destinos de la app
data object Explorar
data object Mapa
data object Favoritos

data class DetalleLugar(val lugarId: String)

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
    // Carga los favoritos y las reseñas guardados en el celular
    val context = LocalContext.current
    LaunchedEffect(context) {
        FavoritosRepositorio.iniciar(context)
        ResenasRepositorio.iniciar(context)
    }

    val backStack = remember { mutableStateListOf<Any>(Explorar) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                pestanas.forEach { pestana ->
                    NavigationBarItem(
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
                entry<Favoritos> {
                    FavoritosScreen(onLugarClick = { id -> backStack.add(DetalleLugar(id)) })
                }
            }
        )
    }
}