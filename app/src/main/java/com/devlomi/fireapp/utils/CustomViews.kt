package com.devlomi.fireapp.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.fireapp.R
import com.devlomi.fireapp.adapters.PixAdapter
import com.devlomi.fireapp.api.Constants
import com.devlomi.fireapp.model.Ads.Ads.AdsModel
import com.devlomi.fireapp.views.videoView.ui.widget.VideoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.ak1.pix.helpers.toPx


fun fragmentBody(
    context: Context,
    adapter: RecyclerView.Adapter<PixAdapter.ViewHolder>,
    clickCallback: View.OnClickListener
): View {
    val layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    ).apply {
        this.gravity = Gravity.RIGHT or Gravity.BOTTOM
    }
    return FrameLayout(context).apply {
        this.layoutParams = layoutParams
        addView(RecyclerView(context).apply {
            layoutManager = GridLayoutManager(context, 3)
            setPadding(0, 100, 0, 0)
            this.layoutParams = layoutParams
            this.adapter = adapter
        })
        addView(FloatingActionButton(context).apply {
            this.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(32, 32, 32, 32)
                this.gravity = Gravity.RIGHT or Gravity.BOTTOM
            }
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            setImageResource(R.drawable.attach_camera)
            setOnClickListener(clickCallback)
        })
    }
}

fun fragmentAds(
    context: Context,
//    onCLickDots: (menuItem: MenuItem) -> View.OnClickListener,
//    onCLickVolume: () -> View.OnClickListener,
    adsModel: AdsModel
): View {
    val layoutParams = LinearLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT
    ).apply {
        this.gravity = Gravity.RIGHT or Gravity.BOTTOM
    }
    return FrameLayout(context).apply {
        this.layoutParams = layoutParams
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.vert_dots))
                setOnClickListener {
                    val popupMenu =
                        PopupMenu(context, it)
                    popupMenu.menuInflater.inflate(R.menu.menu_adv_option, popupMenu.menu)
                    popupMenu.setOnMenuItemClickListener { menuItem -> // Toast message on menu item clicked
//                        onCLickDots(menuItem)
                        return@setOnMenuItemClickListener true
                    }
                    // Showing the popup menu
                    popupMenu.show()

                }
            })
            addView(ImageView(context).apply {
                setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_volume_up))
                setOnClickListener {

                }
            })
        })
        if (!adsModel.media.contains(".mp4") || !adsModel.media.contains(".mov") || !adsModel.media.contains(
                ".wmv"
            ) || !adsModel.media.contains(".avi") || !adsModel.media.contains(".mkv") || !adsModel.media.contains(
                ".webm"
            ) || !adsModel.media.contains(".avchd")
        ) {
            addView(ImageView(context).apply {
                Glide.with(context).load(Constants.BASE_URL_VIDEO + adsModel.media.toString())
                    .into(this)
            })
        } else {
            addView(VideoView(context).apply {
                this.setMedia(Uri.parse(Constants.BASE_URL_VIDEO + adsModel.media))
            })
        }
    }
}
