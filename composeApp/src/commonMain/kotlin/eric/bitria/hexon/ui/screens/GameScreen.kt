package eric.bitria.hexon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AddRoad
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Castle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eric.bitria.hexon.render.GameLayer
import eric.bitria.hexon.ui.components.game.ResourceCard
import eric.bitria.hexon.viewmodel.GameUIViewModel
import eric.bitria.hexon.viewmodel.GameViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GameScreen(
    gameViewModel: GameViewModel = koinViewModel(),
    gameUIViewModel: GameUIViewModel = koinViewModel(),
) {
    val gameEvents = gameViewModel.gameEvents.collectAsState(initial = "Waiting for events...")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        GameLayer(
            modifier = Modifier.fillMaxSize(),
            jsonCollector = gameViewModel.sendJson,
            onJsonReceived = gameViewModel::onJsonReceived
        )

        gameViewModel.testCommand()

        // UI Layer
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            GameHeader(Color.Black)
            GameFooter(Color.Black)
        }
    }
}

@Composable
fun GameHeader(semiBlack: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween, // justify-between
        verticalAlignment = Alignment.Top // items-start
    ) {
        // Left side
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp) // gap-4
        ) {
            PlayerBadge(semiBlack = semiBlack)
            OtherPlayers()
        }

        // Right side
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
        ) {
            // More options button
            Box(
                modifier = Modifier
                    .clip(CircleShape) // rounded-full
                    .background(semiBlack)
                    .padding(8.dp) // p-2
            ) {
                Icon(
                    Icons.Filled.MoreHoriz,
                    contentDescription = "More options",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp) // text-3xl
                )
            }
            // Rank badge
            Row(
                modifier = Modifier
                    .clip(CircleShape) // rounded-full
                    .background(semiBlack)
                    .padding(horizontal = 12.dp, vertical = 4.dp), // px-3 py-1
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp) // gap-1
            ) {
                Icon(
                    Icons.Filled.MilitaryTech,
                    contentDescription = "Rank",
                    tint = Color(0xFFFFEB3B), // text-yellow-400
                    modifier = Modifier.size(18.dp) // text-lg
                )
                Text(
                    text = "8",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp // text-sm
                )
            }
        }
    }
}

@Composable
fun PlayerBadge(semiBlack: Color) {
    Row(
        modifier = Modifier
            .clip(CircleShape) // rounded-full
            .background(semiBlack)
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp), // p-2 pr-4
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp) // gap-2
    ) {
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Player 1 Avatar",
            tint = Color(0xFF81D4FA), // text-blue-300
            modifier = Modifier.size(36.dp) // text-4xl
        )
        Text(
            text = "Player 1",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp // text-lg
        )
    }
}

@Composable
fun OtherPlayers() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp), // space-x-2
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier
            .size(36.dp) // text-4xl
            .alpha(0.5f) // opacity-50
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Other player",
            tint = Color.Gray, // text-gray-600
            modifier = iconModifier
        )
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Other player",
            tint = Color.Gray,
            modifier = iconModifier
        )
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Other player",
            tint = Color.Gray,
            modifier = iconModifier
        )
    }
}

// --- Footer Composables ---

@Composable
fun GameFooter(semiBlack: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            verticalAlignment = Alignment.Bottom // items-end
        ) {
            // Left Column (Build actions & resources)
            Column(
                modifier = Modifier.weight(1f), // 1fr
                verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
            ) {
                BuildActions(semiBlack = semiBlack)
                ResourceItems(semiBlack = semiBlack)
            }
            // Right Column (Turn controls)
            FooterControls()
        }
    }
}

@Composable
fun BuildActions(semiBlack: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .background(semiBlack)
            .padding(8.dp), // p-2
        horizontalArrangement = Arrangement.spacedBy(8.dp) // gap-2
    ) {
        ResourceCard(icon = Icons.Filled.AddRoad, description = "Build Road", count = "0", bgColor = Color.Gray, borderColor = Color.White)
        ResourceCard(icon = Icons.Filled.Home, description = "Build Settlement", count = "0", bgColor = Color.Gray, borderColor = Color.White)
        ResourceCard(icon = Icons.Filled.Castle, description = "Build City", count = "0", bgColor = Color.Gray, borderColor = Color.White)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceItems(semiBlack: Color) {
    FlowRow( // flex-wrap
        modifier = Modifier
            .fillMaxWidth() // To allow justify-center
            .clip(RoundedCornerShape(12.dp)) // rounded-xl
            .background(semiBlack)
            .padding(8.dp), // p-2
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start), // gap-2, justify-center
        verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
    ) {
        ResourceCard("4", Icons.Filled.LocalFlorist, "Wool", Color(0xFFBA68C8), Color(0xFFCE93D8))
        ResourceCard("2", Icons.Filled.Public, "Ore", Color(0xFFFFF59D), Color(0xFFFFF59D))
        ResourceCard("5", Icons.Filled.Park, "Lumber", Color(0xFFFFB74D), Color(0xFFFFCC80))
        ResourceCard("1", Icons.Filled.Terrain, "Brick", Color(0xFFB0BEC5), Color(0xFFCFD8DC))
        ResourceCard("3", Icons.Filled.Agriculture, "Grain", Color(0xFFFFEB3B), Color(0xFFFFEB3B))
    }
}

@Composable
fun FooterControls() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom), // gap-2, justify-end
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ControlIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowForward,
            description = "End Turn",
            color = Color(0xFF2196F3).copy(alpha = 0.8f), // bg-blue-500/80
            iconSize = 30.dp // text-4xl is large, 30.dp fits better
        )
        ControlIconButton(
            icon = Icons.Filled.SwapHoriz,
            description = "Trade",
            color = Color(0xFF4CAF50).copy(alpha = 0.8f), // bg-green-500/80
            iconSize = 30.dp // text-3xl
        )
        Spacer(Modifier.size(48.dp)) // w-12 h-12 spacer
    }
}

@Composable
fun ControlIconButton(
    icon: ImageVector,
    description: String,
    color: Color,
    iconSize: androidx.compose.ui.unit.Dp
) {
    Button(
        onClick = { /*TODO*/ },
        modifier = Modifier.size(48.dp), // w-12 h-12
        shape = RoundedCornerShape(8.dp), // rounded-lg
        colors = ButtonDefaults.buttonColors(containerColor = color),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(iconSize)
        )
    }
}