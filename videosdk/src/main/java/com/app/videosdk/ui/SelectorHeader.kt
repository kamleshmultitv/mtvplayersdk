package com.app.videosdk.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import com.app.videosdk.model.OptionItemModel
import com.app.videosdk.utils.PlayerUtils.calculatePitch
import com.app.videosdk.utils.PlayerUtils.changeVideoResolution
import com.app.videosdk.utils.PlayerUtils.getAudioTrack
import com.app.videosdk.utils.PlayerUtils.getSubTitleFormats
import com.app.videosdk.utils.PlayerUtils.getVideoFormats
import com.app.videosdk.utils.PlayerUtils.selectAudioTrack
import com.app.videosdk.utils.PlayerUtils.setAutoVideoResolution
import com.app.videosdk.utils.PlayerUtils.showAudioTrack
import com.app.videosdk.viewmodel.VideoViewModel

@Composable
fun SelectorHeader(exoPlayer: ExoPlayer, closeOptionCard: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val viewModel: VideoViewModel = viewModel()
    val selectedItems = remember { mutableStateMapOf<Int, Int>() }
    val options by viewModel.options.collectAsState()
    var selectedOption by remember { mutableStateOf(options.firstOrNull()?.id) }

    val closeButtonFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    
    val optionFocusRequesters = remember(options) {
        options.associate { it.id to FocusRequester() }
    }

    LaunchedEffect(Unit) {
        if (options.isNotEmpty()) {
            optionFocusRequesters[options.first().id]?.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                    exoPlayer.play()
                    closeOptionCard(false)
                    true
                } else false
            }
            .pointerInput(Unit) {}
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(options.size) { index ->
                    val option = options[index]
                    OptionItem(
                        option = option,
                        isSelected = selectedOption == option.id,
                        modifier = Modifier
                            .focusRequester(optionFocusRequesters[option.id] ?: FocusRequester())
                            .onKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.DirectionRight -> {
                                            if (index == options.lastIndex) {
                                                closeButtonFocusRequester.requestFocus()
                                                true
                                            } else false
                                        }
                                        Key.DirectionLeft -> {
                                            if (index == 0) {
                                                // Restriction: Do not move left from the first item
                                                true
                                            } else false
                                        }
                                        else -> false
                                    }
                                } else false
                            },
                        onDown = { listFocusRequester.requestFocus() },
                        onUp = { closeButtonFocusRequester.requestFocus() },
                        onFocus = { focusedId ->
                            selectedOption = focusedId
                        }
                    ) { clickedId ->
                        if (selectedOption != clickedId) {
                            selectedOption = clickedId
                        }
                    }
                }
            }

            var isCloseFocused by remember { mutableStateOf(false) }
            IconButton(
                modifier = Modifier
                    .focusRequester(closeButtonFocusRequester)
                    .onFocusChanged { isCloseFocused = it.isFocused }
                    .background(
                        if (isCloseFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (isCloseFocused) 2.dp else 0.dp,
                        color = if (isCloseFocused) Color.White else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionDown -> {
                                    optionFocusRequesters[selectedOption ?: options.first().id]?.requestFocus()
                                    true
                                }
                                Key.DirectionLeft -> {
                                    if (options.isNotEmpty()) {
                                        optionFocusRequesters[options.last().id]?.requestFocus()
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    // Restriction: Do not move right from the close button
                                    true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    exoPlayer.play()
                                    closeOptionCard(false)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                onClick = {
                    exoPlayer.play()
                    closeOptionCard(false)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            thickness = 1.dp,
            color = Color.Gray
        )

        val handleNavigateLR: (Boolean) -> Unit = { isRight ->
            val currentIndex = options.indexOfFirst { it.id == selectedOption }
            if (isRight) {
                if (currentIndex < options.lastIndex) {
                    optionFocusRequesters[options[currentIndex + 1].id]?.requestFocus()
                } else {
                    // Logic for jumping from the last list category to Close button
                    closeButtonFocusRequester.requestFocus()
                }
            } else {
                if (currentIndex > 0) {
                    optionFocusRequesters[options[currentIndex - 1].id]?.requestFocus()
                }
            }
        }

        when (selectedOption) {
            1 -> {
                val audioTrackList = showAudioTrack(context, exoPlayer)
                SelectionList(
                    items = audioTrackList.map { it.name.toString() },
                    selectedIndex = selectedItems[selectedOption] ?: -1,
                    focusRequester = listFocusRequester,
                    onUp = { optionFocusRequesters[selectedOption!!]?.requestFocus() },
                    onNavigateLR = handleNavigateLR
                ) { index ->
                    selectedItems[selectedOption!!] = index
                    selectAudioTrack(audioTrackList[index].id.toString(), exoPlayer)
                }
            }

            2 -> {
                val subTitleList = getSubTitleFormats(exoPlayer)
                val availableCloseCaptionList = getAudioTrack(
                    context, listOf("off") + subTitleList.mapNotNull { it.language ?: it.label }
                )

                SelectionList(
                    items = availableCloseCaptionList.map { it.name.toString() },
                    selectedIndex = selectedItems[selectedOption] ?: -1,
                    focusRequester = listFocusRequester,
                    onUp = { optionFocusRequesters[selectedOption!!]?.requestFocus() },
                    onNavigateLR = handleNavigateLR
                ) { index ->
                    selectedItems[selectedOption!!] = index
                    exoPlayer.trackSelectionParameters =
                        exoPlayer.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(
                                C.TRACK_TYPE_TEXT,
                                availableCloseCaptionList[index].id == "off"
                            )
                            .apply {
                                if (availableCloseCaptionList[index].id != "off") {
                                    setPreferredTextLanguages(
                                        availableCloseCaptionList[index].id ?: "en"
                                    )
                                }
                            }.build()
                }
            }

            3 -> {
                viewModel.getSpeedData()
                val getSpeedControllerResponse by viewModel.speedControlData.observeAsState(
                    emptyList()
                )

                SelectionList(
                    items = getSpeedControllerResponse.map { it.speedTitle },
                    selectedIndex = selectedItems[selectedOption] ?: -1,
                    focusRequester = listFocusRequester,
                    onUp = { optionFocusRequesters[selectedOption!!]?.requestFocus() },
                    onNavigateLR = handleNavigateLR
                ) { index ->
                    selectedItems[selectedOption!!] = index
                    val param = PlaybackParameters(
                        getSpeedControllerResponse[index].speed,
                        calculatePitch(getSpeedControllerResponse[index].speed)
                    )
                    exoPlayer.playbackParameters = param
                }
            }

            4 -> {
                val videoQualityList = getVideoFormats(exoPlayer)

                SelectionList(
                    items = videoQualityList.map { if (it.id == "auto") it.title.toString() else "${it.title}p" },
                    selectedIndex = selectedItems[selectedOption] ?: -1,
                    focusRequester = listFocusRequester,
                    onUp = { optionFocusRequesters[selectedOption!!]?.requestFocus() },
                    onNavigateLR = handleNavigateLR
                ) { index ->
                    selectedItems[selectedOption!!] = index
                    if (index == 0) {
                        setAutoVideoResolution(exoPlayer)
                    } else {
                        changeVideoResolution(
                            exoPlayer,
                            videoQualityList[index].width,
                            videoQualityList[index].height
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectionList(
    items: List<String>,
    selectedIndex: Int,
    focusRequester: FocusRequester,
    onUp: () -> Unit,
    onNavigateLR: (Boolean) -> Unit,
    onItemClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, end = 64.dp)
    ) {
        items(items.size) { index ->
            var isItemFocused by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .wrapContentWidth()
                    .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier)
                    .onFocusChanged { isItemFocused = it.isFocused }
                    .background(
                        if (isItemFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = if (isItemFocused) 1.dp else 0.dp,
                        color = if (isItemFocused) Color.White.copy(alpha = 0.5f) else Color.Transparent,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onItemClick(index)
                    }
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key) {
                                Key.DirectionUp -> {
                                    if (index == 0) {
                                        onUp()
                                        true
                                    } else false
                                }
                                Key.DirectionLeft -> {
                                    onNavigateLR(false)
                                    true
                                }
                                Key.DirectionRight -> {
                                    onNavigateLR(true)
                                    true
                                }
                                Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                                    onItemClick(index)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .focusable()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = items[index],
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.wrapContentWidth()
                )

                if (selectedIndex == index) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✔",
                        color = Color.Green,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun OptionItem(
    option: OptionItemModel,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onDown: () -> Unit,
    onUp: () -> Unit,
    onFocus: (Int) -> Unit,
    onItemClick: (Int) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    val textColor by animateColorAsState(
        targetValue = if (isSelected || isFocused) Color.Yellow else Color.White,
        animationSpec = tween(durationMillis = 300)
    )

    val fontWeight by animateFloatAsState(
        targetValue = (if (isSelected || isFocused) FontWeight.Bold.weight else FontWeight.Medium.weight).toFloat(),
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = modifier
            .padding(8.dp)
            .onFocusChanged {
                isFocused = it.isFocused
                if (it.isFocused) {
                    onFocus(option.id)
                }
            }
            .background(
                if (isFocused) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (isFocused) 2.dp else 0.dp,
                color = if (isFocused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onItemClick(option.id) }
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            onDown()
                            true
                        }
                        Key.DirectionUp -> {
                            onUp()
                            true
                        }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onItemClick(option.id)
                            true
                        }
                        else -> false
                    }
                } else false
            }
            .focusable()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = option.title,
            fontSize = 16.sp,
            fontWeight = FontWeight(fontWeight.toInt()),
            color = textColor
        )
    }
}
