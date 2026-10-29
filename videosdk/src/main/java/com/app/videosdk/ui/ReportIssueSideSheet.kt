package com.app.videosdk.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun ReportIssueSideSheet(
    onDismiss: () -> Unit,
    onReportSubmitted: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val predefinedIssues = listOf(
        "Buffering / Freezing",
        "Audio & Video out of sync",
        "Subtitle issue / missing",
        "Video quality / resolution issue",
        "Playback error / crash",
        "Other"
    )

    val selectedIssues = remember { mutableStateMapOf<String, Boolean>() }
    var otherText by remember { mutableStateOf("") }
    var isDismissing by remember { mutableStateOf(false) }

    val transitionState = remember {
        MutableTransitionState(false)
    }.apply {
        targetState = !isDismissing
    }

    val dismissAction = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                delay(350.milliseconds) // matches exit animation duration
                onDismiss()
            }
        }
    }

    BackHandler(enabled = true) {
        dismissAction()
    }

    val backgroundAlpha by animateFloatAsState(
        targetValue = if (!isDismissing) 0.85f else 0f,
        animationSpec = tween(350),
        label = "backgroundAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = backgroundAlpha))
            .zIndex(20f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                dismissAction()
            },
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(
                    durationMillis = 400,
                    easing = FastOutSlowInEasing
                )
            ) + fadeIn(
                animationSpec = tween(300)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(
                    durationMillis = 350,
                    easing = FastOutSlowInEasing
                )
            ) + fadeOut(
                animationSpec = tween(250)
            ),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.55f)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .background(Color.Black)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { },
                color = Color.Black,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    // Header with Title & Cross Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Report Playback Issue",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = dismissAction) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        thickness = 1.dp,
                        color = Color.DarkGray
                    )

                    Text(
                        text = "Select the issue(s) you are experiencing:",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    // Predefined List with Checkboxes
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        items(predefinedIssues) { issue ->
                            val isChecked = selectedIssues[issue] == true
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedIssues[issue] = !isChecked
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { checked ->
                                            selectedIssues[issue] = checked
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color.Yellow,
                                            uncheckedColor = Color.Gray,
                                            checkmarkColor = Color.Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = issue,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                // If "Other" is checked, show text field right after it
                                if (issue == "Other" && isChecked) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    OutlinedTextField(
                                        value = otherText,
                                        onValueChange = { otherText = it },
                                        placeholder = { Text("Please describe other issue...", color = Color.Gray) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 32.dp, bottom = 8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Yellow,
                                            unfocusedBorderColor = Color.Gray,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White
                                        ),
                                        maxLines = 3
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Submit Button (Wrapped width)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = {
                                val activeIssues = selectedIssues.filter { it.value }.keys.toList()
                                if (activeIssues.isEmpty()) {
                                    Toast.makeText(context, "Please select at least one issue", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                val reportSummary = buildString {
                                    append("Reported Issues: ")
                                    append(activeIssues.joinToString(", "))
                                    if (selectedIssues["Other"] == true && otherText.isNotBlank()) {
                                        append(" | Other details: $otherText")
                                    }
                                }

                                onReportSubmitted(reportSummary)
                                dismissAction()
                            },
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow)
                        ) {
                            Text(
                                text = "Submit Report",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun ReportIssueSideSheetPreview() {
    ReportIssueSideSheet(
        onDismiss = {},
        onReportSubmitted = {}
    )
}
