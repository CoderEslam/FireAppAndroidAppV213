package com.devlomi.fireapp.views.videoView.core.listener

import androidx.media3.common.VideoSize

interface VideoSizeListener {
  fun onVideoSizeChanged(videoSize: VideoSize)
}