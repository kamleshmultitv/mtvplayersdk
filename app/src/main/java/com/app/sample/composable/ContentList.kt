package com.app.sample.composable

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.rememberTvLazyListState
import com.app.sample.model.ContentItem

@Composable
fun ContentList(
    pagingItems: LazyPagingItems<ContentItem>,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberTvLazyListState()

    // Request focus once items are loaded to ensure visibility
    LaunchedEffect(pagingItems.itemCount) {
        if (pagingItems.itemCount > 0) {
            focusRequester.requestFocus()
        }
    }

    TvLazyRow(
        state = listState,
        modifier = modifier
            .fillMaxSize() // Use fillMaxSize to occupy the weight-assigned Box
            .focusGroup(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            count = pagingItems.itemCount,
            key = { index -> pagingItems[index]?.id ?: index }
        ) { index ->
            val item = pagingItems[index]
            item?.let {
                val cardModifier = if (index == 0) {
                    Modifier.focusRequester(focusRequester)
                } else {
                    Modifier
                }
                ContentCard(
                    content = it,
                    modifier = cardModifier
                ) {
                    onItemClick(index)
                }
            }
        }
    }
}
