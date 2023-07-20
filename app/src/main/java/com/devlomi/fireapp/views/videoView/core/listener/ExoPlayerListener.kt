package com.devlomi.fireapp.views.videoView.core.listener

import com.devlomi.fireapp.views.videoView.listener.OnTimelineChangedListener
import com.devlomi.fireapp.views.videoView.core.state.PlaybackStateListener
import com.devlomi.fireapp.views.videoView.listener.OnSeekCompletionListener
import com.devlomi.fireapp.views.videoView.nmp.ExoMediaPlayer

/**
 * A listener for core [ExoMediaPlayer] events
 */
interface ExoPlayerListener : OnSeekCompletionListener, PlaybackStateListener,
    OnTimelineChangedListener {
  fun onError(player: ExoMediaPlayer, e: Exception?)

  fun onVideoSizeChanged(width: Int, height: Int, unAppliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
}
