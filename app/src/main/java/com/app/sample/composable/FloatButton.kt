package com.app.sample.composable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Preview
@Composable
fun PreviewFloatButton() {
    FloatButton(playContent = {
            _, _, _ ->
    })
}

@Composable
fun FloatButton(playContent: (String, String, Boolean) -> Unit = { _, _, _ -> }) {
    var expanded by remember { mutableStateOf(false) }

    // Now trigger the file picker when needed
    val modifier = Modifier.padding(16.dp)

    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(durationMillis = 500), label = "button rotation animation"
    )


    Box(modifier = Modifier.fillMaxSize()
        .padding(end = 16.dp, bottom = 60.dp)) {
        Surface(
            modifier = modifier.align(Alignment.BottomEnd),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primary,
            onClick = { expanded = !expanded }
        ) {
            AnimatedContent(
                targetState = expanded,
                transitionSpec = {
                    fadeIn(animationSpec = tween(150, 150)) togetherWith
                            fadeOut(animationSpec = tween(150)) using
                            SizeTransform { initialSize, targetSize ->
                                if (targetState) {
                                    keyframes {
                                        IntSize(targetSize.width, initialSize.height) at 150
                                        durationMillis = 300
                                    }
                                } else {
                                    keyframes {
                                        IntSize(initialSize.width, targetSize.height) at 150
                                        durationMillis = 300
                                    }
                                }
                            }
                }, label = "floating"
            ) { targetExpanded ->
                if (targetExpanded) {
                    CardWithTextFieldsAndButton(expanded = { expanded = it }, playContent = playContent)
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .size(56.dp)
                        .rotate(rotationAngle)
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Expand",
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}
