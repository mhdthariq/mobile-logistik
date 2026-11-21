package com.kaminova.novalogistikmobile.ui.components

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun evidenceImageRequest(context: Context, source: String): ImageRequest {
    val data = if (source.startsWith("data:", ignoreCase = true)) {
        withContext(Dispatchers.Default) {
            val comma = source.indexOf(',')
            require(comma > 0 && source.startsWith("data:image/", ignoreCase = true) &&
                source.substring(0, comma).endsWith(";base64", ignoreCase = true))
            Base64.decode(source.substring(comma + 1), Base64.DEFAULT)
        }
    } else {
        source
    }
    return ImageRequest.Builder(context).data(data)
        .diskCachePolicy(CachePolicy.DISABLED)
        .build()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EvidenceImage(
    source: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    showStatus: Boolean = true
) {
    val context = LocalContext.current
    key(source) {
        val request by produceState<Result<ImageRequest>?>(null, context, source) {
            value = try {
                Result.success(evidenceImageRequest(context, source))
            } catch (e: IllegalArgumentException) {
                Result.failure(e)
            }
        }
        val model = request?.getOrNull()
        when {
            model != null -> SubcomposeAsyncImage(
                model = model, contentDescription = contentDescription,
                modifier = modifier, contentScale = contentScale,
                loading = {
                    if (showStatus) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                },
                error = {
                    if (showStatus) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Foto gagal dibuka.")
                    }
                }
            )
            showStatus -> Box(modifier, contentAlignment = Alignment.Center) {
                if (request == null) LoadingIndicator() else Text("Foto gagal dibuka.")
            }
        }
    }
}
