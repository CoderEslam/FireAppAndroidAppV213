package com.devlomi.fireapp.views.videoView.core.source.builder

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import com.devlomi.fireapp.views.videoView.core.source.builder.MediaSourceBuilder
import io.grpc.netty.shaded.io.netty.util.internal.UnstableApi

@OptIn(UnstableApi::class)
class SsMediaSourceBuilder : MediaSourceBuilder() {
  override fun build(attributes: MediaSourceAttributes): MediaSource {
    val factoryAttributes = attributes.copy(
      transferListener = null
    )

    val dataSourceFactory = buildDataSourceFactory(factoryAttributes)
    val meteredDataSourceFactory = buildDataSourceFactory(attributes)
    val mediaItem = MediaItem.Builder().setUri(attributes.uri).build()

    return SsMediaSource.Factory(DefaultSsChunkSource.Factory(meteredDataSourceFactory), dataSourceFactory)
      .setDrmSessionManagerProvider(attributes.drmSessionManagerProvider)
      .createMediaSource(mediaItem)
  }
}
