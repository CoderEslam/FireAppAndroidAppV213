package com.devlomi.fireapp.views.videoView.core.source.data

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import com.devlomi.fireapp.views.videoView.core.source.data.DataSourceFactoryProvider

@OptIn(UnstableApi::class)
class DefaultDataSourceFactoryProvider: DataSourceFactoryProvider {
  override fun provide(userAgent: String, listener: TransferListener?): DataSource.Factory {
    return DefaultHttpDataSource.Factory().apply {
      setUserAgent(userAgent)
      setTransferListener(listener)
    }
  }
}