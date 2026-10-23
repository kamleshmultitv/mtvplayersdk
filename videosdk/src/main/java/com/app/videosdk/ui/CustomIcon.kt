package com.app.videosdk.ui

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import coil.ImageLoader
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest

@Composable
fun CustomIcon(
    resId: Int?,
    defaultIcon: ImageVector,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Int? = null,
    iconUrl: String? = null,
    tintColorInt: Int? = null,
    tintHex: String? = null
) {
    val context = LocalContext.current
    val explicitTintColor = tintColorInt?.let { Color(it) }
        ?: tintHex.toComposeColorOrNull()
        ?: tint?.let { colorResource(id = it) }
    val fallbackTintColor = explicitTintColor ?: Color.White
    val safeIconUrl = iconUrl?.trim()?.takeIf { it.isNotBlank() }
    val urlDrawableResId = safeIconUrl?.resolveDrawableResourceId(context)
    val fallbackResId = resId?.takeIf { it != 0 }

    when {
        urlDrawableResId != null -> {
            LocalDrawableIcon(
                resId = urlDrawableResId,
                contentDescription = contentDescription,
                modifier = modifier,
                tintColor = fallbackTintColor
            )
        }

        safeIconUrl != null -> {
            RemoteIcon(
                context = context,
                iconUrl = safeIconUrl,
                fallbackResId = fallbackResId,
                defaultIcon = defaultIcon,
                contentDescription = contentDescription,
                modifier = modifier,
                explicitTintColor = explicitTintColor,
                fallbackTintColor = fallbackTintColor
            )
        }

        fallbackResId != null -> {
            LocalDrawableIcon(
                resId = fallbackResId,
                contentDescription = contentDescription,
                modifier = modifier,
                tintColor = fallbackTintColor
            )
        }

        else -> {
            DefaultVectorIcon(
                defaultIcon = defaultIcon,
                contentDescription = contentDescription,
                modifier = modifier,
                tintColor = fallbackTintColor
            )
        }
    }
}

@Composable
private fun RemoteIcon(
    context: Context,
    iconUrl: String,
    fallbackResId: Int?,
    defaultIcon: ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    explicitTintColor: Color?,
    fallbackTintColor: Color
) {
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()
    }
    val request = remember(context, iconUrl) {
        ImageRequest.Builder(context)
            .data(iconUrl)
            .crossfade(true)
            .build()
    }
    val painter = rememberAsyncImagePainter(
        model = request,
        imageLoader = imageLoader
    )

    if (painter.state is AsyncImagePainter.State.Error) {
        if (fallbackResId != null) {
            LocalDrawableIcon(
                resId = fallbackResId,
                contentDescription = contentDescription,
                modifier = modifier,
                tintColor = fallbackTintColor
            )
        } else {
            DefaultVectorIcon(
                defaultIcon = defaultIcon,
                contentDescription = contentDescription,
                modifier = modifier,
                tintColor = fallbackTintColor
            )
        }
    } else {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = explicitTintColor?.let { ColorFilter.tint(it) }
        )
    }
}

@Composable
private fun LocalDrawableIcon(
    resId: Int,
    contentDescription: String?,
    modifier: Modifier,
    tintColor: Color
) {
    Icon(
        painter = painterResource(resId),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tintColor
    )
}

@Composable
private fun DefaultVectorIcon(
    defaultIcon: ImageVector,
    contentDescription: String?,
    modifier: Modifier,
    tintColor: Color
) {
    Icon(
        imageVector = defaultIcon,
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tintColor
    )
}

private fun String?.toComposeColorOrNull(): Color? {
    val safeValue = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        val normalized = when {
            safeValue.startsWith("#") -> safeValue
            safeValue.length == 6 || safeValue.length == 8 -> "#$safeValue"
            else -> safeValue
        }
        Color(android.graphics.Color.parseColor(normalized))
    }.getOrNull()
}

private fun String.resolveDrawableResourceId(context: Context): Int? {
    val value = trim()
    if (value.isBlank()) return null
    if (value.contains("://") || value.startsWith("/") || value.startsWith("file:") || value.startsWith("content:")) {
        return null
    }

    val stripped = value
        .removePrefix("@drawable/")
        .removePrefix("drawable/")
        .removePrefix("R.drawable.")

    val resourceName = stripped.substringBeforeLast('.', stripped)
    if (resourceName.isBlank()) return null

    return listOf(context.packageName, "com.app.videosdk")
        .asSequence()
        .map { packageName ->
            context.resources.getIdentifier(resourceName, "drawable", packageName)
        }
        .firstOrNull { it != 0 }
}
