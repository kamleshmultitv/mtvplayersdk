package com.app.sample.viewModel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.app.sample.dataSource.ContentPagingSource
import com.app.sample.extra.ApiConstant
import com.app.sample.extra.ApiConstant.SUCCESS
import com.app.sample.model.GetContentModel
import com.app.sample.extra.ApiConstant.TOKEN
import com.app.sample.extra.ApiPathKeys
import com.app.sample.retrofit.RetrofitFactory
import com.app.sample.utils.ApiEncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import com.app.sample.model.ContentDetailsResponse
import com.app.sample.model.ContentItem
import com.app.sample.model.Json

/**
 * Created by kamle on 03,September,2024,DownloadSdk
 */

open class ContentViewModel : ViewModel() {
    private var getContentModel by mutableStateOf<GetContentModel?>(null)

    val contentListData = Pager(PagingConfig(pageSize = 10, enablePlaceholders = false)) {
        ContentPagingSource(getContentModel)
    }.flow.cachedIn(viewModelScope)

    var contentDetailsState by mutableStateOf<ContentItem?>(null)
        private set



    fun setContent() {
        getContentModel = GetContentModel(
            url = "https://api.artofliving.app/artoflivingapi/v10/content/list",

          //  contentId = "114080",
            token = TOKEN,
            // seasonId = "2171",
            seasonId = "2225",
            offset = "0",
            limit = "5",
            isGroup = "1"

            /* url = "https://api.artofliving.app/artoflivingapi/v10/content/list",
             contentId = "113742",
             token = TOKEN,
             seasonId = "2159",
             offset = "0",
             limit = "5",
             isGroup = "1"*/
        )
    }

    fun getContentDetails() {
        val completeUrl = "https://api.artofliving.app/artoflivingapi/v10/content/detail".toUri().buildUpon().apply {
            appendPath(ApiPathKeys.DEVICE.value)
            appendPath(ApiConstant.CURRENT_PLATFORM.value)
            appendPath(ApiPathKeys.CONTENT_ID.value)
            appendPath("115198")
            appendPath(ApiPathKeys.LOCATION.value)
            appendPath("IN")
        }.toString()
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitFactory.getRetrofit()
                        .getContentDetails(completeUrl, TOKEN)
                }
                if (response?.code != null && response.code == SUCCESS) {
                    val data = Json.parse(
                        ApiEncryptionHelper().decryptionResponse(response.result.toString())
                            .trim { it <= ' ' }, ContentDetailsResponse::class.java
                    )
                    if (data?.content != null) {
                        contentDetailsState = data.content
                    }
                } else if (response?.code != null) {
                  //  message.postValue(response.error.toString())
                } else {
                 //   message.postValue(context.resources.getString(R.string.something_went_wrong))
                }
            } catch (throwable: Throwable) {
               // message.postValue(context.resources.getString(R.string.something_went_wrong))
                throwable.printStackTrace()
            }
        }
    }
}


