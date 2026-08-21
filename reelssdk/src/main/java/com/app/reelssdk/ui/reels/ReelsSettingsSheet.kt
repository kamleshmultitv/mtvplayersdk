package com.app.reelssdk.ui.reels

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.exoplayer.ExoPlayer
import com.app.reelssdk.model.ReelsPlayerModel
import com.app.reelssdk.ui.SelectorHeader
import com.app.reelssdk.ui.SettingsLayoutMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsSettingsSheet(
    playerModel: ReelsPlayerModel?,
    exoPlayer: ExoPlayer?,
    selectedItemsState: MutableMap<Int, Int>,
    selectedOptionId: Int?,
    onSelectedOptionChange: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = {
            exoPlayer?.play()
            onDismiss()
        },
        modifier = Modifier.fillMaxWidth(),
        sheetState = sheetState,
        containerColor = Color(0xFF111111),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 420.dp)
                .navigationBarsPadding()
        ) {
            SelectorHeader(
                playerModel = playerModel,
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                layoutMode = SettingsLayoutMode.TwoPane,
                selectedItemsState = selectedItemsState,
                selectedOptionId = selectedOptionId,
                onSelectedOptionChange = onSelectedOptionChange,
                closeOptionCard = { isOpen ->
                    if (!isOpen) onDismiss()
                }
            )
        }
    }
}
