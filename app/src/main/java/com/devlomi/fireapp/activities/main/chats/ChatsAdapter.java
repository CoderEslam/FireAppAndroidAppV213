package com.devlomi.fireapp.activities.main.chats;

import static com.devlomi.fireapp.Advertisement.api.Constants.BASE_URL_VIDEO;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.devlomi.fireapp.R;
import com.devlomi.fireapp.model.Ads.Ads.AdsModel;
import com.devlomi.fireapp.model.constants.MessageType;
import com.devlomi.fireapp.model.constants.TypingStat;
import com.devlomi.fireapp.model.realms.Chat;
import com.devlomi.fireapp.model.realms.GroupEvent;
import com.devlomi.fireapp.model.realms.Message;
import com.devlomi.fireapp.model.realms.User;
import com.devlomi.fireapp.utils.AdapterHelper;
import com.devlomi.fireapp.utils.MessageTypeHelper;
import com.devlomi.fireapp.utils.RealmHelper;
import com.devlomi.fireapp.utils.glide.GlideApp;
import com.devlomi.fireapp.utils.network.FireManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import io.reactivex.disposables.CompositeDisposable;
import io.realm.OrderedRealmCollection;
import io.realm.RealmRecyclerViewAdapter;
import io.realm.RealmResults;

/**
 * Created by Devlomi on 03/08/2017.
 */

//the RealmRecyclerViewAdapter provides autoUpdate feature
//which will handle changes in list automatically with smooth animations
public class ChatsAdapter extends RealmRecyclerViewAdapter<Chat, RecyclerView.ViewHolder> {

    private static final String TAG = "ChatsAdapter";
    private Context context;
    //chats list
    List<Chat> originalList;
    List<Chat> chatList;
    List<AdsModel> adsModelList;

    //this list will contain the selected chats when user start selecting chats
    List<Chat> selectedChatForActionMode = new ArrayList<>();

    //this hashmap is to save typing state when user scrolls
    //because the recyclerView will not save it
    HashMap<String, Integer> typingStatHashmap = new HashMap<>();

    ChatsAdapterCallback callback;

    private boolean isMute = false;

    public interface ChatsAdapterCallback {
        void userProfileClicked(User user);

        void onClick(Chat chat, View itemView);

        void onLongClick(Chat chat, View itemView);

        void onBind(int pos, Chat chat);
    }

    private CompositeDisposable disposables = new CompositeDisposable();

    void onDestroy() {
        disposables.dispose();
    }

    public ChatsAdapter(@Nullable OrderedRealmCollection<Chat> data, boolean autoUpdate, Context context, ChatsAdapterCallback callback) {
        super(data, autoUpdate);
        this.originalList = data;
        this.context = context;
        this.callback = callback;
        chatList = data;
    }

    public void setAds(List<AdsModel> adsModelList) {
        this.adsModelList = adsModelList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ChatsHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.row_chats, parent, false));
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        final Chat chat = chatList.get(position);
        final User user = chat.getUser();
        final ChatsHolder mHolder = (ChatsHolder) holder;


        if (callback != null) {
            callback.onBind(holder.getAdapterPosition(), chat);
        }
        //if other user is typing then show typing layout
        //this will set the state over scrolling
        if (typingStatHashmap.containsValue(chat.getChatId())) {
            mHolder.tvTypingStat.setVisibility(View.VISIBLE);
            mHolder.tvLastMessage.setVisibility(View.GONE);
            mHolder.countUnreadBadge.setVisibility(View.GONE);

            int stat = typingStatHashmap.get(chat.getChatId());
            if (stat == TypingStat.TYPING)
                if (stat == TypingStat.TYPING)
                    mHolder.tvTypingStat.setText(context.getResources().getString(R.string.typing));
                else if (stat == TypingStat.RECORDING)
                    mHolder.tvTypingStat.setText(context.getResources().getString(R.string.recording));


            //otherwise set default state and show last message layout
        } else {
            mHolder.tvTypingStat.setVisibility(View.GONE);
            mHolder.tvLastMessage.setVisibility(View.VISIBLE);
            mHolder.countUnreadBadge.setVisibility(View.VISIBLE);
        }

        Random rand = new Random();
//        int n = rand.nextInt(adsModelList.size() - 1);
        if (position == 2) {
            mHolder._ads_.setVisibility(View.VISIBLE);
//            mHolder.image_adv.setVisibility(View.VISIBLE);
//            mHolder.videoView.setVisibility(View.GONE);
            if (adsModelList != null) {
                try {
                    if (adsModelList.get(0).getMedia().contains(".mp4")) {
                        mHolder.videoView.setVisibility(View.VISIBLE);
                        mHolder.image_adv.setVisibility(View.GONE);
                        mHolder.videoView.setVideoURI(Uri.parse(BASE_URL_VIDEO + adsModelList.get(0).getMedia()));
                    } else {
                        mHolder.videoView.setVisibility(View.GONE);
                        mHolder.image_adv.setVisibility(View.VISIBLE);
                    }
                } catch (NullPointerException e) {
                    Log.e(TAG, "onBindViewHolder: " + e.getMessage());
                    mHolder._ads_.setVisibility(View.GONE);
                }

            } else {
                mHolder._ads_.setVisibility(View.GONE);
            }
            mHolder.ic_option_.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(mHolder.itemView.getContext(), view);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_adv_option, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            // Toast message on menu item clicked
                            Toast.makeText(mHolder.itemView.getContext(), "You Clicked " + menuItem.getTitle(), Toast.LENGTH_SHORT).show();
                            return true;
                        }
                    });
                    // Showing the popup menu
                    popupMenu.show();
                }
            });
//            AudioManager audioManager = (AudioManager) holder.itemView.getContext().getSystemService(Context.AUDIO_SERVICE);
            mHolder.ic_volume_.setOnClickListener(new View.OnClickListener() {
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
                        mHolder.ic_volume_.setImageDrawable(mHolder.ic_volume_.getContext().getDrawable(R.drawable.ic_volume_off));
//                        audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI);
                        isMute = true;
                    } else {
//                        mHolder.videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                            @Override
//                            public void onPrepared(MediaPlayer mp) {
//                                mp.setVolume(0, 100);
//                            }
//                        });
                        mHolder.ic_volume_.setImageDrawable(mHolder.ic_volume_.getContext().getDrawable(R.drawable.ic_volume_up));
                        isMute = false;
//                        audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI);

                    }
                }
            });

            mHolder.image_adv.setImageDrawable(mHolder.itemView.getContext().getDrawable(R.drawable.logo));
//            mHolder.videoView.setVideoURI(Uri.parse("android.resource://" + mHolder.itemView.getContext().getPackageName() + "/" + R.drawable.logo));

            mHolder.videoView.start();

            // create an object of media controller class
//            MediaController mediaControls = new MediaController(mHolder.itemView.getContext());
//            mediaControls.setAnchorView(mHolder.simpleVideoView);

            // set the media controller for video view
//            mHolder.simpleVideoView.setMediaController(mediaControls);
        } else {
            mHolder._ads_.setVisibility(View.GONE);
        }

        keepActionModeItemsSelected(holder.itemView, chat);


        //set the user name from phonebook
        String name = "";
        if (user != null) {
            name = user.getProperUserName();
        }

        mHolder.tvTitle.setText(name);


        //get the lastmessage from chat
        final Message message = chat.getLastMessage();
        //set last message time
        mHolder.timeChats.setText(chat.getTime());

        if (message != null) {

            final String content = message.getContent();
            //if it's a TextMessage
            if (message.isTextMessage()
                    || message.getType() == MessageType.GROUP_EVENT
                    || MessageType.isDeletedMessage(message.getType())
                    || message.isStickerType()
            ) {
                //set group event text
                if (message.getType() == MessageType.GROUP_EVENT) {
                    String groupEvent = GroupEvent.extractString(message.getContent(), user.getGroup().getUsers());
                    mHolder.tvLastMessage.setText(groupEvent);
                    //set message deleted event text
                } else if (MessageType.isDeletedMessage(message.getType())) {
                    if (message.getType() == MessageType.SENT_DELETED_MESSAGE) {
                        mHolder.tvLastMessage.setText(context.getResources().getString(R.string.you_deleted_this_message));
                    } else {
                        mHolder.tvLastMessage.setText(context.getResources().getString(R.string.this_message_was_deleted));
                    }
                } else if (message.isStickerType()) {
                    mHolder.tvLastMessage.setText(R.string.sticker);
                } else
                    //set the message text
                    mHolder.tvLastMessage.setText(content);
                //remove the icon besides the text (camera,voice etc.. icon)
                mHolder.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);


                //Set Icon if it's not a Text Message
            } else {
                mHolder.tvLastMessage.setText(MessageTypeHelper.extractMessageTypeMetadataText(message));

                //set icon besides the type text
                Drawable drawable = getColoredDrawable(message);
                if (drawable != null) {
                    mHolder.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                    mHolder.tvLastMessage.setCompoundDrawablePadding(5);
                }
            }

            //Set Recipient Marks
            //if the Message was sent by user
            if (message.getType() == MessageType.GROUP_EVENT || MessageType.isDeletedMessage(message.getType())) {
                mHolder.imgReadTagChats.setVisibility(View.GONE);
            } else if (message.getFromId().equals(FireManager.getUid())) {
                mHolder.imgReadTagChats.setVisibility(View.VISIBLE);
                mHolder.imgReadTagChats.setImageDrawable(AdapterHelper.getColoredStatDrawable(context, message.getMessageStat()));
            } else {
                mHolder.imgReadTagChats.setVisibility(View.GONE);
            }
        } else {
            mHolder.tvLastMessage.setText("");
            mHolder.imgReadTagChats.setVisibility(View.GONE);
            mHolder.tvLastMessage.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }
        int unreadCount = chat.getUnReadCount();

        //if there are unread messages hide the unread count badge
        if (unreadCount == 0)
            mHolder.countUnreadBadge.setVisibility(View.GONE);
            //otherwise show it and set the unread count
        else {
            mHolder.countUnreadBadge.setVisibility(View.VISIBLE);
            mHolder.countUnreadBadge.setText(chat.getUnReadCount() + "");
        }


        //on chat click
        mHolder.rlltBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null) {
                    callback.onClick(chat, holder.itemView);
                }
            }
        });

        //start action mode and select this chat
        mHolder.rlltBody.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (callback != null) callback.onLongClick(chat, holder.itemView);

                return true;
            }
        });

        //show user profile in the dialog-like activity
        mHolder.userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null) callback.userProfileClicked(user);
            }
        });


        loadUserPhoto(user, mHolder.userProfile);


    }

    //change the icon color drawable depending on message state
    private Drawable getColoredDrawable(Message message) {
        int messageTypeResource = MessageTypeHelper.getMessageTypeDrawable(message.getType());
        if (messageTypeResource == -1) return null;
        Resources resources = context.getResources();

        Drawable drawable = resources.getDrawable(messageTypeResource);
        drawable.mutate();
        int color;


        //if it's a voice message
        if (message.isVoiceMessage()) {
            //if it was sent by the user
            if (message.getType() == MessageType.SENT_VOICE_MESSAGE) {
                //if the other user listened to it set the color to blue
                if (message.isVoiceMessageSeen()) {
                    color = resources.getColor(R.color.colorBlue);
                } else {
                    //if it's not listened set it to grey
                    color = resources.getColor(R.color.colorTextDesc);
                }
                //if this message is received from the other user
            } else {
                //if the user listened to it set it to blue
                if (message.isVoiceMessageSeen()) {
                    color = resources.getColor(R.color.colorBlue);
                    //otherwise set it to green
                } else {
                    color = resources.getColor(R.color.colorGreen);
                }
            }
            //if it's not a voice message change the icon color to grey
        } else {
            color = resources.getColor(R.color.colorTextDesc);
        }
        //change the icon color
        DrawableCompat.setTintMode(drawable, PorterDuff.Mode.SRC_IN);
        DrawableCompat.setTint(drawable, color);
        return drawable;
    }


    @Override
    public int getItemCount() {
        return chatList.size();
    }


    public class ChatsHolder extends RecyclerView.ViewHolder {
        private RelativeLayout rlltBody;
        private ImageView userProfile;

        public TextView tvTitle, tvLastMessage, timeChats, tvTypingStat, countUnreadBadge;

        public ImageView imgReadTagChats;
        private View _ads_;
        private VideoView videoView;
        private ImageView ic_volume_;
        private ImageView ic_option_;
        private ImageView image_adv;


        public ChatsHolder(View itemView) {
            super(itemView);
            rlltBody = itemView.findViewById(R.id.container_layout);
            userProfile = itemView.findViewById(R.id.user_photo);
            tvTitle = itemView.findViewById(R.id.tv_name);
            tvLastMessage = itemView.findViewById(R.id.tv_last_message);
            timeChats = itemView.findViewById(R.id.time_chats);
            imgReadTagChats = itemView.findViewById(R.id.img_read_tag_chats);
            countUnreadBadge = itemView.findViewById(R.id.count_unread_badge);
            _ads_ = itemView.findViewById(R.id._ads_);
            videoView = _ads_.findViewById(R.id.video_adv);
            ic_volume_ = _ads_.findViewById(R.id.ic_volume_);
            ic_option_ = _ads_.findViewById(R.id.ic_option_);
            image_adv = _ads_.findViewById(R.id.image_adv);
            tvTypingStat = itemView.findViewById(R.id.tv_typing_stat);

        }
    }


    private void loadUserPhoto(final User user, final ImageView imageView) {
        if (user == null)
            return;
        if (user.getUid() == null)
            return;

        if (user.isBroadcastBool())
            imageView.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_broadcast_with_bg));
        else if (user.getThumbImg() != null) {

            GlideApp.with(context).load(user.getThumbImg()).into(imageView);

        }

//        //if it's a broadcast there is no photo to load
//        if (!user.isBroadcastBool()) {
//            //check for a new thumb img
//            disposables.add(FireManager.checkAndDownloadUserThumbImg(user).subscribe(thumbImg -> {
//                try {
//                    GlideApp.with(context).load(thumbImg).into(imageView);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }));
//        }
    }


    void itemAdded(View view, Chat chat) {
        //add chat to list
        selectedChatForActionMode.add(chat);
        //change background color to blue
        setBackgroundColor(view, true);
    }

    //set the background color on scroll because of default behavior of recyclerView
    void keepActionModeItemsSelected(View itemView, Chat chat) {
        if (selectedChatForActionMode.contains(chat)) {
            setBackgroundColor(itemView, true);
        } else {
            setBackgroundColor(itemView, false);
        }
    }


    //remove chat from selected list
    void itemRemoved(View view, Chat chat) {
        //change the background color to default color
        setBackgroundColor(view, false);
        //remove item from list
        selectedChatForActionMode.remove(chat);
    }


    //exit action mode and notify the adapter to redraw the default items
    public void exitActionMode() {
        selectedChatForActionMode.clear();
        notifyDataSetChanged();
    }


    private void setBackgroundColor(View view, boolean isAdded) {
        if (isAdded)
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.item_selected_background_color));
        else
            //default background color
            view.setBackgroundColor(ContextCompat.getColor(context, R.color.chats_background));
    }

    public void filter(String query) {
        if (query.trim().isEmpty()) {
            chatList = originalList;
        } else {
            RealmResults<Chat> chats = RealmHelper.getInstance().searchForChat(query);
            chatList = chats;
        }

        notifyDataSetChanged();
    }

}
