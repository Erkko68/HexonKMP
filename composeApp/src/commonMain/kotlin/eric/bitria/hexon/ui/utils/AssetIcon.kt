package eric.bitria.hexon.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.svg.SvgDecoder
import eric.bitria.hexon.data.repository.ApiResult
import eric.bitria.hexon.data.repository.AssetsRepository
import eric.bitria.hexon.dtos.assets.IconResponse
import io.ktor.utils.io.core.toByteArray
import org.koin.compose.koinInject

@Composable
fun rememberAssetData(
    assetId: String,
    repository: AssetsRepository = koinInject()
): State<IconResponse?> {
    // 1. Check cache SYNCHRONOUSLY before composition starts
    val cached = remember(assetId) { repository.getIconCached(assetId) }

    // 2. Pass 'cached' as the initialValue
    return produceState(initialValue = cached, key1 = assetId) {
        // Only fetch if we don't have it already
        if (value == null) {
            val result = repository.getIcon(assetId)
            if (result is ApiResult.Success) {
                value = result.data
            }
        }
    }
}


@Composable
fun AssetIconDisplay(
    data: IconResponse?,
    modifier: Modifier = Modifier,
    fallbackTint: Color = Color.Gray
) {
    Box(modifier = modifier) {
        if (data != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(data.svg.toByteArray())
                    .decoderFactory { result, options, _ ->
                        SvgDecoder(result.source,options)
                    }
                    .build(),
                contentDescription = null
            )
        } else {
            // Loading / Error Fallback
            Icon(
                imageVector = Icons.Default.LocalFlorist,
                contentDescription = null,
                tint = fallbackTint,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}