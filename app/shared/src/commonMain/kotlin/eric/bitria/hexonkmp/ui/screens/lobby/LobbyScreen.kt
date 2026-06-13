package eric.bitria.hexonkmp.ui.screens.lobby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import eric.bitria.hexonkmp.core.protocol.LobbyMember
import eric.bitria.hexonkmp.core.protocol.PartyRules
import eric.bitria.hexonkmp.ui.components.ScrollPicker
import eric.bitria.hexonkmp.ui.components.hud.PlayerToken
import eric.bitria.hexonkmp.ui.theme.Spacing
import eric.bitria.hexonkmp.ui.theme.Tokens
import hexonkmp.app.shared.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

// The waiting room (shared with MenuScreen via one LobbyViewModel). Shows the roster
// for both matchmaking and private lobbies; the host gets a Start button, everyone a
// Leave. On GameStarted it signals [onGameStarted]; [onExit] returns to the menu.
@Composable
fun LobbyScreen(
    onGameStarted: () -> Unit,
    onExit: () -> Unit,
    viewModel: LobbyViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.gameStarted.collect { onGameStarted() }
    }

    val leave = { viewModel.leave(); onExit() }

    when (val s = state) {
        // Matchmaking (no host): the classic waiting view. Private lobbies: the room.
        is LobbyUiState.InLobby ->
            if (s.hostId == null) MatchmakingView(state = s, onLeave = leave)
            else LobbyRoom(
                state = s,
                onStart = viewModel::startGame,
                onLeave = leave,
            )
        is LobbyUiState.Error -> Centered {
            Text(stringResource(Res.string.something_went_wrong), style = MaterialTheme.typography.titleMedium)
            Text(s.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Button(onClick = leave) { Text(stringResource(Res.string.action_back)) }
        }
        // Connecting (and the transient Idle right after leaving) show a spinner.
        else -> Centered {
            CircularProgressIndicator()
            Text(stringResource(Res.string.connecting), style = MaterialTheme.typography.bodyLarge)
            Button(
                onClick = leave,
                colors = leaveColors(),
            ) { Text(stringResource(Res.string.action_cancel)) }
        }
    }
}

@Composable
private fun LobbyRoom(
    state: LobbyUiState.InLobby,
    onStart: (PartyRules) -> Unit,
    onLeave: () -> Unit,
) {
    // Tick the auto-start countdown down locally; re-seed on each server roster.
    var remaining by remember(state.countdownSeconds) { mutableStateOf(state.countdownSeconds) }
    LaunchedEffect(state.countdownSeconds) {
        var r = state.countdownSeconds
        while (r != null && r > 0) {
            delay(1000); r -= 1; remaining = r
        }
    }

    // The host's rules are edited locally and only leave the device on Start — no
    // per-change traffic, and only the host sees them. Default: 10 VP, no timer.
    var rules by remember { mutableStateOf(PartyRules(victoryPoints = 10, turnTimerSeconds = null)) }

    Centered {
        state.code?.let { code ->
            Text(stringResource(Res.string.lobby_code), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatCode(code), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }

        Text(stringResource(Res.string.players_count, state.members.size, state.maxPlayers), style = MaterialTheme.typography.titleSmall)

        // All seats: filled members first, then greyed placeholders for empty slots.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterHorizontally)) {
            repeat(state.maxPlayers) { index ->
                val member = state.members.getOrNull(index)
                if (member != null) MemberColumn(member, isHost = member.id == state.hostId)
                else EmptySlotColumn()
            }
        }

        remaining?.let {
            Text(stringResource(Res.string.starting_in, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Party rules: only the host configures them (sent with Start).
        if (state.isHost) RulesPanel(rules = rules, onRulesChange = { rules = it })

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(onClick = onLeave, colors = leaveColors()) { Text(stringResource(Res.string.action_leave)) }
            if (state.isHost) {
                Button(onClick = { onStart(rules) }, enabled = state.canStart) { Text(stringResource(Res.string.start_game)) }
            }
        }
    }
}

// Host-only party rules, picked with scroll selectors: victory points (5..12) and
// the per-turn timer (No timer, then +10s steps). Edited locally; applied at Start.
private val VP_VALUES: List<Int> = (5..12).toList()
private val TIMER_VALUES: List<Int?> = listOf<Int?>(null) + (10..120 step 10).toList()

@Composable
private fun RulesPanel(rules: PartyRules, onRulesChange: (PartyRules) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            stringResource(Res.string.lobby_rules),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(stringResource(Res.string.rule_victory_points), style = MaterialTheme.typography.labelSmall)
        ScrollPicker(
            items = VP_VALUES,
            selectedIndex = VP_VALUES.indexOf(rules.victoryPoints).coerceAtLeast(0),
            onSelectedIndex = { onRulesChange(rules.copy(victoryPoints = VP_VALUES[it])) },
            label = { it.toString() },
        )

        Text(stringResource(Res.string.rule_turn_timer), style = MaterialTheme.typography.labelSmall)
        ScrollPicker(
            items = TIMER_VALUES,
            selectedIndex = TIMER_VALUES.indexOf(rules.turnTimerSeconds).coerceAtLeast(0),
            onSelectedIndex = { onRulesChange(rules.copy(turnTimerSeconds = TIMER_VALUES[it])) },
            label = { if (it == null) "∞" else stringResource(Res.string.rule_seconds, it) },
        )
    }
}

// The classic matchmaking waiting view: spinner + "Waiting for players…" + count.
@Composable
private fun MatchmakingView(state: LobbyUiState.InLobby, onLeave: () -> Unit) {
    var remaining by remember(state.countdownSeconds) { mutableStateOf(state.countdownSeconds) }
    LaunchedEffect(state.countdownSeconds) {
        var r = state.countdownSeconds
        while (r != null && r > 0) {
            delay(1000); r -= 1; remaining = r
        }
    }
    Centered {
        CircularProgressIndicator()
        Text(stringResource(Res.string.waiting_for_players), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(Res.string.count_fraction, state.members.size, state.maxPlayers),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        remaining?.let {
            Text(stringResource(Res.string.starting_in, it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = onLeave, colors = leaveColors()) { Text(stringResource(Res.string.action_cancel)) }
    }
}

// One player in the roster: icon, name below, and "host" below the name for the host.
@Composable
private fun MemberColumn(member: LobbyMember, isHost: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        PlayerToken(MaterialTheme.colorScheme.primary, member.name, size = Tokens.tokenMd)
        Text(member.name, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        if (isHost) {
            Text(stringResource(Res.string.lobby_host), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// An empty seat: a dimmed, greyed-out icon (as if disconnected) with "..." for a name.
@Composable
private fun EmptySlotColumn() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        PlayerToken(
            color = MaterialTheme.colorScheme.surfaceVariant,
            label = stringResource(Res.string.open_slot),
            size = Tokens.tokenMd,
            modifier = Modifier.alpha(0.5f),
        )
        Text("…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Centered(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

// Inverted accent for Leave/Cancel: dark container with the amber primary as content.
@Composable
private fun leaveColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.onPrimary,
    contentColor = MaterialTheme.colorScheme.primary,
)

private fun formatCode(code: String): String =
    if (code.length == 6) "${code.substring(0, 3)} ${code.substring(3)}" else code
