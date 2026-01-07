package eric.bitria.hexon.ui.components.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import eric.bitria.hexon.viewmodel.social.UserStats

@Composable
fun UserInfoSection(
    username: String,
    avatarUrl: String?,
    stats: UserStats
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        AsyncImage(
            model = avatarUrl,
            contentDescription = "User Avatar",
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(
                    width = 4.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = CircleShape
                )
        )

        // Username
        Text(
            text = username,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            ),
            modifier = Modifier.padding(top = 16.dp) // mt-4
        )

        Spacer(modifier = Modifier.height(24.dp)) // mt-6

        // Stats Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // Simulates max-w-md
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatBox(
                label = "Wins",
                value = stats.wins,
                color = Color(0xFF60A5FA),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Streak",
                value = stats.streak,
                color = Color(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            )
            StatBox(
                label = "Win Rate",
                value = stats.winRate,
                color = Color(0xFFFACC15),
                modifier = Modifier.weight(1f)
            )
        }
    }
}