package com.app.sample.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.app.sample.dataSource.ContentPagingSource
import com.app.sample.model.GetContentModel
import com.app.sample.extra.ApiConstant.TOKEN
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

/**
 * Created by kamle on 03,September,2024,DownloadSdk
 */

open class ContentViewModel : ViewModel() {
    private val seasonRequest = MutableStateFlow<GetContentModel?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val contentListData = seasonRequest
        .filterNotNull()
        .flatMapLatest { request ->
            Pager(PagingConfig(pageSize = 10, enablePlaceholders = false)) {
                ContentPagingSource(request)
            }.flow
        }
        .cachedIn(viewModelScope)

    /** Calls content/list using the season_id path rather than content/detail. */
    fun getSeason(seasonId: String = "2225") {
        require(seasonId.isNotBlank()) { "seasonId must not be blank" }
        seasonRequest.value = GetContentModel(
            url = "https://api.artofliving.app/artoflivingapi/v10/content/list",
            token = TOKEN,
            seasonId = seasonId,
            offset = "0",
            limit = "5",
            isGroup = "1"
        )
    }

    fun setContent() = getSeason()
}

