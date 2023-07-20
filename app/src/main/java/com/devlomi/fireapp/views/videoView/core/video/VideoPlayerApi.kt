package com.devlomi.fireapp.views.videoView.core.video

import com.devlomi.fireapp.views.videoView.core.audio.AudioPlayerApi
import com.devlomi.fireapp.views.videoView.core.listener.CaptionListener

/**
 * The basic APIs expected in the backing video view
 * implementations to allow us to create an abstraction
 * between the Native (Android) VideoView and the VideoView
 * using the ExoPlayer.
 */
interface VideoPlayerApi: AudioPlayerApi {
  /**
   * Performs the functionality to stop the video in playback
   *
   * @param clearSurface `true` if the surface should be cleared
   */
  fun stop(clearSurface: Boolean)

  /**
   * Specifies the listener to inform of closed caption events
   *
   * @param listener The listener to inform of closed caption events
   */
  fun setCaptionListener(listener: CaptionListener?)
}