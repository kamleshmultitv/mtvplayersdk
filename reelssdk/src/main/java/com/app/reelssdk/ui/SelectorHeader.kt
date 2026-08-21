package com.app.reelssdk.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.OptionItemModel
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.utils.PlayerUtils.calculatePitch
import com.app.reelssdk.utils.PlayerUtils.changeVideoResolution
import com.app.reelssdk.utils.PlayerUtils.getTextTrackOptions
import com.app.reelssdk.utils.PlayerUtils.getVideoFormats
import com.app.reelssdk.utils.PlayerUtils.selectAudioTrack
import com.app.reelssdk.utils.PlayerUtils.selectTextTrack
import com.app.reelssdk.utils.PlayerUtils.setAutoVideoResolution
import com.app.reelssdk.utils.PlayerUtils.showAudioTrack
import com.app.reelssdk.utils.PlayerUtils.TextTrackOption
import com.app.reelssdk.viewmodel.VideoViewModel

enum class SettingsLayoutMode {
    HorizontalTabs,
    TwoPane
}

@Composable
fun SelectorHeader(
    modifier: Modifier = Modifier,
    playerModel: ReelsPlayerModel? = null,
    exoPlayer: ExoPlayer?,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp),
    layoutMode: SettingsLayoutMode = SettingsLayoutMode.HorizontalTabs,
    selectedItemsState: MutableMap<Int, Int>? = null,
    selectedOptionId: Int? = null,
    onSelectedOptionChange: (Int?) -> Unit = {},
    closeOptionCard: (Boolean) -> Unit = {}
) {
    val viewModel: VideoViewModel = viewModel()
    val internalSelectedItems = remember { mutableStateMapOf<Int, Int>() }
    val selectedItems = selectedItemsState ?: internalSelectedItems
    val options by viewModel.options.collectAsState()
    var internalSelectedOption by remember { mutableStateOf(options.firstOrNull()?.id) }
    val selectedOption = selectedOptionId ?: internalSelectedOption
    val selectOption: (Int) -> Unit = { optionId ->
        internalSelectedOption = optionId
        onSelectedOptionChange(optionId)
    }
    var captionOptions by remember(exoPlayer) { mutableStateOf(getTextTrackOptions(exoPlayer)) }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer ?: return@DisposableEffect onDispose {}

        captionOptions = getTextTrackOptions(player)

        val listener = object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                captionOptions = getTextTrackOptions(player)
            }
        }

        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Intercept Back button to close the header and resume video
    BackHandler(enabled = true) {
        exoPlayer?.play()
        closeOptionCard(false)
    }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        if (layoutMode == SettingsLayoutMode.TwoPane) {
            TwoPaneSettingsContent(
                playerModel = playerModel,
                exoPlayer = exoPlayer,
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = selectOption,
                selectedItems = selectedItems,
                captionOptions = captionOptions,
                closeOptionCard = closeOptionCard
            )
            return@Column
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
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
                        isSelected = selectedOption == option.id
                    ) { clickedId -> selectOption(clickedId) }
                }
            }

            IconButton(
                onClick = {
                    exoPlayer?.play()
                    closeOptionCard(false)
                }
            ) {
                CustomIcon(
                    resId = playerModel?.customControls?.crossFadeIconRes,
                    defaultIcon = Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(24.dp),
                    tint = playerModel?.customControls?.iconTintRes
                )
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 1.dp,
            color = Color.Gray
        )

        Box(modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)) {
            SettingsSelectionList(
                exoPlayer = exoPlayer,
                selectedOption = selectedOption,
                selectedItems = selectedItems,
                captionOptions = captionOptions,
                horizontalPadding = 16.dp
            )
        }
    }
}

@Composable
private fun TwoPaneSettingsContent(
    playerModel: ReelsPlayerModel?,
    exoPlayer: ExoPlayer?,
    options: List<OptionItemModel>,
    selectedOption: Int?,
    onOptionSelected: (Int) -> Unit,
    selectedItems: MutableMap<Int, Int>,
    captionOptions: List<TextTrackOption>,
    closeOptionCard: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 12.dp, end = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Settings",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(
            onClick = {
                exoPlayer?.play()
                closeOptionCard(false)
            }
        ) {
            CustomIcon(
                resId = playerModel?.customControls?.crossFadeIconRes,
                defaultIcon = Icons.Default.Close,
                contentDescription = "Close",
                modifier = Modifier.size(22.dp),
                tint = playerModel?.customControls?.iconTintRes
            )
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp),
        thickness = 1.dp,
        color = Color.White.copy(alpha = 0.12f)
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp, bottom = 10.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .width(136.dp)
                .fillMaxHeight()
                .padding(start = 10.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(options.size) { index ->
                val option = options[index]
                SettingsSideOptionItem(
                    title = option.title,
                    isSelected = selectedOption == option.id,
                    onClick = { onOptionSelected(option.id) }
                )
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 10.dp, end = 8.dp, top = 2.dp)
        ) {
            SettingsSelectionList(
                exoPlayer = exoPlayer,
                selectedOption = selectedOption,
                selectedItems = selectedItems,
                captionOptions = captionOptions,
                horizontalPadding = 0.dp
            )
        }
    }
}

@Composable
private fun SettingsSideOptionItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.68f),
        animationSpec = tween(durationMillis = 180)
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(durationMillis = 180)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(22.dp)
                .background(
                    if (isSelected) Color.Red else Color.Transparent,
                    RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun SettingsSelectionList(
    exoPlayer: ExoPlayer?,
    selectedOption: Int?,
    selectedItems: MutableMap<Int, Int>,
    captionOptions: List<TextTrackOption>,
    horizontalPadding: Dp
) {
    val context = LocalContext.current
    val viewModel: VideoViewModel = viewModel()

    when (selectedOption) {
        1 -> {
            val audioTrackList =
                remember(context, exoPlayer) { showAudioTrack(context, exoPlayer) }
            SelectionList(
                items = audioTrackList.map { it.name.toString() },
                selectedIndex = selectedItems[selectedOption] ?: -1,
                horizontalPadding = horizontalPadding
            ) { index ->
                selectedItems[selectedOption] = index
                selectAudioTrack(audioTrackList[index].id.toString(), exoPlayer)
            }
        }

        2 -> {
            val selectedCaptionIndex = selectedItems[selectedOption]?.takeIf {
                it in captionOptions.indices
            }
                ?: captionOptions.indexOfFirst { it.isSelected }.takeIf { it >= 0 }
                ?: 0

            SelectionList(
                items = captionOptions.map { it.displayName },
                selectedIndex = selectedCaptionIndex,
                horizontalPadding = horizontalPadding
            ) { index ->
                selectedItems[selectedOption] = index
                selectTextTrack(captionOptions[index], exoPlayer)
            }
        }

        3 -> {
            viewModel.getSpeedData()
            val speedData by viewModel.speedControlData.observeAsState(emptyList())

            SelectionList(
                items = speedData.map { it.speedTitle },
                selectedIndex = selectedItems[selectedOption] ?: -1,
                horizontalPadding = horizontalPadding
            ) { index ->
                selectedItems[selectedOption] = index
                val param = PlaybackParameters(
                    speedData[index].speed,
                    calculatePitch(speedData[index].speed)
                )
                exoPlayer?.playbackParameters = param
            }
        }

        4 -> {
            val qualityList = remember(exoPlayer) { getVideoFormats(exoPlayer) }

            SelectionList(
                items = qualityList.map { if (it.id == "auto") it.title.toString() else "${it.title}p" },
                selectedIndex = selectedItems[selectedOption] ?: -1,
                horizontalPadding = horizontalPadding
            ) { index ->
                selectedItems[selectedOption] = index
                if (index == 0) {
                    setAutoVideoResolution(exoPlayer)
                } else {
                    changeVideoResolution(
                        exoPlayer,
                        qualityList[index].width,
                        qualityList[index].height
                    )
                }
            }
        }
    }
}

@Composable
fun SelectionList(
    items: List<String>,
    selectedIndex: Int,
    horizontalPadding: Dp = 16.dp,
    onItemClick: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
    ) {
        items(items.size) { index ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(index) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSelected = selectedIndex == index

                Text(
                    text = if (isSelected) "✔" else "",
                    color = Color.Green,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(28.dp)
                )

                Text(
                    text = items[index],
                    color = if (isSelected) Color.Green else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun OptionItem(
    option: OptionItemModel,
    isSelected: Boolean,
    onItemClick: (Int) -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.Yellow else Color.White,
        animationSpec = tween(durationMillis = 300)
    )

    Box(
        modifier = Modifier
            .padding(8.dp)
            .background(
                if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onItemClick(option.id) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = option.title,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = textColor
        )
    }
}
