package com.app.videosdk.ui.cut

import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CutBottomSheet(
    videoUri: Uri,
    duration: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        ClipEditorContent(
            videoUri = videoUri,
            duration = duration
        )
    }
}
