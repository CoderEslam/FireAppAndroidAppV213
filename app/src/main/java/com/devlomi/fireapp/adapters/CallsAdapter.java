package com.devlomi.fireapp.adapters;

import static com.devlomi.fireapp.Advertisement.api.Constants.BASE_URL_VIDEO;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.Ads.Ads.AdsModel;
import com.devlomi.fireapp.model.constants.FireCallDirection;
import com.devlomi.fireapp.model.realms.FireCall;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.TimeHelper;
import com.devlomi.hidely.hidelyviews.HidelyImageView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

public class CallsAdapter extends RealmRecyclerViewAdapter<FireCall, CallsAdapter.PhoneCallHolder> {

    private List<FireCall> fireCallList;
    private List<FireCall> originalList;
    private List<FireCall> selectedItemForActionMode;
    private Context context;
    private OnClickListener onPhoneCallClick;
    private List<AdsModel> adsModelList;

    private static final String TAG = "CallsAdapter";

    public CallsAdapter(@Nullable OrderedRealmCollection<FireCall> data,
                        List<FireCall> selectedItemForActionMode, Context context, OnClickListener onPhoneCallClick) {
        super(data, true);
        this.fireCallList = data;
        originalList = fireCallList;
        this.selectedItemForActionMode = selectedItemForActionMode;
        this.context = context;
        this.onPhoneCallClick = onPhoneCallClick;
    }

    public void setAds(List<AdsModel> adsModelList) {
        this.adsModelList = adsModelList;
    }

    @NonNull
    @Override
    public PhoneCallHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View row = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_call, parent, false);
        return new PhoneCallHolder(row);
    }


    @Override
    public void onBindViewHolder(@NonNull PhoneCallHolder holder, int position) {
        holder.bind(fireCallList.get(position));
        if (position == 2) {
            holder.videoView.setOnPreparedListener(mp -> {
//                mp.setVolume(0, 0);
                mp.setLooping(false);
//                Log.e(TAG, "onBindViewHolder: " + mp.getDuration() + " -> " + mHolder.videoView.getCurrentPosition());
//                mp.getDuration();
//                mHolder.videoView.getCurrentPosition();
            });
            holder.videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    Log.e(TAG, "onBindViewHolder: " + mediaPlayer.getDuration() + " -> " + holder.videoView.getCurrentPosition());
                }
            });
            holder._ads_.setVisibility(View.VISIBLE);
            if (adsModelList != null) {
                try {
                    if (adsModelList.get(0).getMedia().contains(".mp4")) {
                        holder.videoView.setVisibility(View.VISIBLE);
                        holder.image_adv.setVisibility(View.GONE);
                        holder.videoView.setVideoURI(Uri.parse(BASE_URL_VIDEO + adsModelList.get(0).getMedia()));
                    } else {
                        holder.videoView.setVisibility(View.GONE);
                        holder.image_adv.setVisibility(View.VISIBLE);
                        Glide.with(holder.itemView.getContext()).load(BASE_URL_VIDEO + adsModelList.get(0).getMedia()).into(holder.image_adv);
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onBindViewHolder: " + e.getMessage());
                    holder._ads_.setVisibility(View.GONE);
                }

            } else {
                holder._ads_.setVisibility(View.GONE);
            }
            holder.ic_option_.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(holder.itemView.getContext(), view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_adv_option, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            // Toast message on menu item clicked
                            return true;
                        }
                    });
                    // Showing the popup menu
                    popupMenu.show();
                }
            });
//            AudioManager audioManager = (AudioManager) holder.itemView.getContext().getSystemService(Context.AUDIO_SERVICE);
            /*mHolder.ic_volume_.setOnClickListener(new View.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @SuppressLint("UseCompatLoadingForDrawables")
                @Override
                public void onClick(View view) {
                    mHolder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mp.setVolume(0, 0);
                        }
                    });
                    if (!isMute) {
//                        mHolder.ic_volume_.setImageDrawable(mHolder.ic_volume_.getContext().getDrawable(R.drawable.ic_volume_off));
//                        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
//                        isMute = true;
                    } else {
//                        mHolder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                            @Override
//                            public void onPrepared(MediaPlayer mp) {
//                                mp.setVolume(0, 100);
//                            }
//                        });
//                        mHolder.ic_volume_.setImageDrawable(mHolder.ic_volume_.getContext().getDrawable(R.drawable.ic_volume_up));
//                        isMute = false;
//                        audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);

                    }
                }
            });*/

            holder.image_adv.setImageDrawable(holder.itemView.getContext().getDrawable(R.drawable.logo));
//            mHolder.videoView.setVideoURI(Uri.parse("android.resource://" + mHolder.itemView.getContext().getPackageName() + "/" + R.drawable.logo));

            holder.videoView.start();

            // create an object of media controller class
//            MediaController mediaControls = new MediaController(mHolder.itemView.getContext());
//            mediaControls.setAnchorView(mHolder.simpleVideoView);

            // set the media controller for video view
//            mHolder.simpleVideoView.setMediaController(mediaControls);
        } else {
            holder._ads_.setVisibility(View.GONE);
            holder.videoView.setVisibility(View.GONE);
            holder.image_adv.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return fireCallList.size();
    }

    class PhoneCallHolder extends RecyclerView.ViewHolder {
        private CircleImageView profileImage;
        private TextView tvUsername;
        private TextView tvCallTime;
        private ImageView callType;
        private ImageButton btnCall;
        private HidelyImageView imgSelected;
        private View _ads_;
        private VideoView videoView;
        private ImageView ic_option_;
        private ImageView image_adv;

        public PhoneCallHolder(View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profile_image);
            tvUsername = itemView.findViewById(R.id.tv_username);
            tvCallTime = itemView.findViewById(R.id.tv_call_time);
            callType = itemView.findViewById(R.id.call_type);
            btnCall = itemView.findViewById(R.id.btn_call);
            imgSelected = itemView.findViewById(R.id.img_selected);
            _ads_ = itemView.findViewById(R.id._ads_);
            videoView = _ads_.findViewById(R.id.video_adv);
            ic_option_ = _ads_.findViewById(R.id.ic_option_);
            image_adv = _ads_.findViewById(R.id.image_adv);
        }

        public void bind(final FireCall fireCall) {
            User user = fireCall.getUser();
            if (user != null) {
                tvUsername.setText(user.getProperUserName());
                Glide.with(context).load(user.getThumbImg()).into(profileImage);
            } else
                tvUsername.setText(fireCall.getPhoneNumber());

            callType.setImageDrawable(getPhoneCallType(fireCall.getDirection()));
            btnCall.setImageResource(fireCall.isVideo() ? R.drawable.ic_videocam : R.drawable.ic_phone_white);

            tvCallTime.setText(TimeHelper.getCallTime(fireCall.getTimestamp()));

            if (selectedItemForActionMode.contains(fireCall)) {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.item_selected_background_color));
                imgSelected.setVisibility(View.VISIBLE);

            } else {
                itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.bgColor));
                imgSelected.setVisibility(View.INVISIBLE);
            }


            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onPhoneCallClick != null)
                        onPhoneCallClick.onItemClick(imgSelected, view, fireCall);
                }
            });

            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    if (onPhoneCallClick != null)
                        onPhoneCallClick.onLongClick(imgSelected, view, fireCall);
                    return true;
                }
            });

            btnCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onPhoneCallClick != null)
                        onPhoneCallClick.onIconButtonClick(view, fireCall);
                }
            });




        }


        private Drawable getPhoneCallType(int type) {
            Drawable incomingDrawable = ContextCompat.getDrawable(context, R.drawable.ic_call_received);
            Drawable outgoingDrawable = ContextCompat.getDrawable(context, R.drawable.ic_call_made);

            switch (type) {
                case FireCallDirection.OUTGOING:
                    assert outgoingDrawable != null;
                    outgoingDrawable.mutate();
                    DrawableCompat.setTintMode(outgoingDrawable, PorterDuff.Mode.SRC_IN);
                    DrawableCompat.setTint(outgoingDrawable, ContextCompat.getColor(context, R.color.colorGreen));
                    return outgoingDrawable;

                case FireCallDirection.ANSWERED:
                    assert incomingDrawable != null;
                    incomingDrawable.mutate();
                    DrawableCompat.setTintMode(incomingDrawable, PorterDuff.Mode.SRC_IN);
                    DrawableCompat.setTint(incomingDrawable, ContextCompat.getColor(context, R.color.colorGreen));
                    return incomingDrawable;

                default:
                    assert incomingDrawable != null;
                    incomingDrawable.mutate();
                    DrawableCompat.setTintMode(incomingDrawable, PorterDuff.Mode.SRC_IN);
                    DrawableCompat.setTint(incomingDrawable, ContextCompat.getColor(context, R.color.red));
                    return incomingDrawable;


            }
        }
    }


    public interface OnClickListener {
        void onItemClick(HidelyImageView hidelyImageView, View itemView, FireCall fireCall);

        void onIconButtonClick(View itemView, FireCall fireCall);

        void onLongClick(HidelyImageView hidelyImageView, View itemView, FireCall fireCall);
    }

    public void filter(String query) {
        if (query.trim().isEmpty()) {
            fireCallList = originalList;
        } else {
            RealmResults<FireCall> fireCalls = RealmHelper.getInstance().searchForCall(query);
            fireCallList = fireCalls;
        }

        notifyDataSetChanged();
    }
}
