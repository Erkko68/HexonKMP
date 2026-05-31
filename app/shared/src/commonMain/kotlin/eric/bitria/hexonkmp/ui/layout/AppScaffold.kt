package eric.bitria.hexonkmp.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eric.bitria.hexonkmp.ui.navigation.AppDestination

private val WIDE_BREAKPOINT = 600.dp

@Composable
fun AppScaffold(content: @Composable (destination: AppDestination) -> Unit) {
    var current by remember { mutableStateOf<AppDestination>(AppDestination.Game) }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_BREAKPOINT) {
            WideLayout(current, onSelect = { current = it }, content)
        } else {
            NarrowLayout(current, onSelect = { current = it }, content)
        }
    }
}

@Composable
private fun WideLayout(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit,
    content: @Composable (AppDestination) -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        NavigationRail {
            Spacer(Modifier.weight(1f))
            AppDestination.all.forEach { dest ->
                NavigationRailItem(
                    selected = dest == current,
                    onClick = { onSelect(dest) },
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) },
                )
            }
            Spacer(Modifier.weight(1f))
        }
        Box(Modifier.weight(1f).fillMaxSize()) {
            content(current)
        }
    }
}

@Composable
private fun NarrowLayout(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit,
    content: @Composable (AppDestination) -> Unit,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                AppDestination.all.forEach { dest ->
                    NavigationBarItem(
                        selected = dest == current,
                        onClick = { onSelect(dest) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            content(current)
        }
    }
}
