package eric.bitria.hexonkmp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eric.bitria.hexonkmp.ui.theme.Spacing

@Composable
fun LobbyScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Hexon", style = MaterialTheme.typography.displayMedium)
        Text("Find a game to get started", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { /* TODO: call GameClient.joinGame() */ }) {
            Text("Find Game")
        }
    }
}
