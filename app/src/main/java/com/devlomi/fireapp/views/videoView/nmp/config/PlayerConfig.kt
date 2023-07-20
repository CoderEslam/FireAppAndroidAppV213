package com.devlomi.fireapp.views.videoView.nmp.config

import android.content.Context
import android.os.Handler
import com.devlomi.fireapp.views.videoView.nmp.manager.UserAgentProvider
import com.devlomi.fireapp.views.videoView.nmp.manager.WakeManager
import com.devlomi.fireapp.views.videoView.nmp.manager.track.TrackManager

import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.analytics.AnalyticsCollector
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.BandwidthMeter
import com.devlomi.fireapp.views.videoView.core.source.MediaSourceProvider
import com.devlomi.fireapp.views.videoView.core.source.data.DataSourceFactoryProvider
import com.devlomi.fireapp.views.videoView.util.FallbackManager

/**
 * Supplies the classes necessary for the [com.devbrackets.android.exomedia.nmp.ExoMediaPlayerImpl]
 * to play media. It is recommended to use the [PlayerConfigBuilder] to construct a new
 * instance as that will provide default implementations.
 */
data class PlayerConfig(
  val context: Context,
  val fallbackManager: FallbackManager,
  val analyticsCollector: AnalyticsCollector,
  val bandwidthMeter: BandwidthMeter,
  val handler: Handler,
  val rendererFactory: RenderersFactory,
  val trackManager: TrackManager,
  val wakeManager: WakeManager,
  val loadControl: LoadControl,
  val userAgentProvider: UserAgentProvider,
  val mediaSourceProvider: MediaSourceProvider,
  val mediaSourceFactory: MediaSource.Factory,
  val dataSourceFactoryProvider: DataSourceFactoryProvider
)