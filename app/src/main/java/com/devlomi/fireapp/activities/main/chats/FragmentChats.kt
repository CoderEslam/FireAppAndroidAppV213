package com.devlomi.fireapp.activities.main.chats

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.fireapp.R
import com.devlomi.fireapp.activities.NewChatActivity
import com.devlomi.fireapp.activities.NewGroupActivity
import com.devlomi.fireapp.activities.ProfilePhotoDialog
import com.devlomi.fireapp.activities.main.MainViewModel
import com.devlomi.fireapp.activities.main.chats.ChatsAdapter.ChatsHolder
import com.devlomi.fireapp.activities.main.messaging.ChatActivity
import com.devlomi.fireapp.api.Constants.BASE_URL_VIDEO
import com.devlomi.fireapp.api.Constants.TOKEN
import com.devlomi.fireapp.api.RetrofitInstance
import com.devlomi.fireapp.fragments.BaseFragment
import com.devlomi.fireapp.interfaces.FragmentCallback
import com.devlomi.fireapp.model.Ads.Ads.AdsList
import com.devlomi.fireapp.model.Ads.Ads.AdsModel
import com.devlomi.fireapp.model.constants.GroupEventTypes
import com.devlomi.fireapp.model.constants.MessageStat
import com.devlomi.fireapp.model.constants.MessageType
import com.devlomi.fireapp.model.constants.TypingStat
import com.devlomi.fireapp.model.realms.Chat
import com.devlomi.fireapp.model.realms.GroupEvent
import com.devlomi.fireapp.model.realms.Message
import com.devlomi.fireapp.model.realms.User
import com.devlomi.fireapp.utils.*
import com.devlomi.fireapp.utils.GroupTyping.GroupTypingListener
import com.devlomi.fireapp.utils.network.FireManager
import com.devlomi.fireapp.utils.network.GroupManager
import com.devlomi.fireapp.views.lavafab.Child
import com.devlomi.fireapp.views.lavafab.LavaFab
import com.devlomi.fireapp.views.videoView.ui.widget.VideoView
import com.google.android.gms.ads.AdView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.reactivex.disposables.CompositeDisposable
import io.realm.OrderedRealmCollectionChangeListener
import io.realm.RealmResults
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Random

class FragmentChats : BaseFragment(), GroupTypingListener, ActionMode.Callback,
    ChatsAdapter.ChatsAdapterCallback {
    private var rvChats: RecyclerView? = null
    private lateinit var lava_fab_bottom_right: LavaFab
    var adapter: ChatsAdapter? = null
    var linearLayoutManager: LinearLayoutManager? = null
    var chatList: RealmResults<Chat>? = null
    var changeListener: OrderedRealmCollectionChangeListener<RealmResults<Chat>>? = null
    var typingEventListener: ValueEventListener? = null
    var voiceMessageListener: ValueEventListener? = null
    var lastMessageStatListener: ValueEventListener? = null
    var groupTypingList: MutableList<GroupTyping>? = null
    var fireListener: FireListener? = null
    override var adView: AdView? = null
    private var callback: FragmentCallback? = null
    private var actionMenu: Menu? = null
    private val TAG = "FragmentChats"
    private val mainViewModel: MainViewModel by activityViewModels()
    private val viewModel: ChatsFragmentViewModel by activityViewModels()
    var actionMode: ActionMode? = null
    private var selectedChats = ArrayList<Chat>()

    private val groupManager = GroupManager()
    override val disposables = CompositeDisposable()

    private lateinit var cld: ConnectionLiveData

    private val isHasMutedItem: Boolean
        get() {
            val selectedItems = selectedChats

            for (chat in selectedItems) {
                if (chat.isMuted)
                    return true
            }
            return false
        }

    private val isHasGroupItem: Boolean
        get() {
            val selectedItems = selectedChats

            for (chat in selectedItems) {
                val user = chat.user
                if (user.isGroupBool && user.group.isActive)
                    return true
            }
            return false
        }

    private fun updateMutedIcon(menuItem: MenuItem?, isMuted: Boolean) {
        menuItem?.setIcon(if (isMuted) R.drawable.ic_volume_up else R.drawable.ic_volume_off)
    }

    private fun setMenuItemVisibility(b: Boolean) {

        actionMenu?.findItem(R.id.menu_item_mute)?.isVisible = b

    }

    private fun updateGroupItems() {
        actionMenu?.findItem(R.id.menu_item_delete)?.isVisible = !isHasGroupItem
        actionMenu?.findItem(R.id.exit_group_item)?.isVisible = areAllOfChatsGroups()
    }

    private fun areAllOfChatsGroups(): Boolean {
        var b = false
        val selectedItems = selectedChats
        for (chat in selectedItems) {
            val user = chat.user
            if (user.isGroupBool && user.group.isActive)
                b = true
            else {
                return false
            }
        }
        return b

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        init(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        callback = context as? FragmentCallback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fireListener = FireListener()
        chatList = RealmHelper.getInstance().allChats
        setTheAdapter()
        listenForTypingStat()
        listenForVoiceMessageStat()
        listenForLastMessageStat()
        listenForMessagesChanges()
        adViewInitialized(adView)
        checkNetworkConnection()

        mainViewModel.queryTextChange.observe(
            viewLifecycleOwner,
            androidx.lifecycle.Observer { text ->
                onQueryTextChange(text)
            })

        with(lava_fab_bottom_right) {
            setParentOnClickListener {
                lava_fab_bottom_right.trigger()
            }
            setChildOnClickListener(Child.TOP) {
                startActivity(Intent(requireActivity(), NewChatActivity::class.java))
            }
            setChildOnClickListener(Child.LEFT) {
                val intent = Intent(requireActivity(), NewGroupActivity::class.java)
                intent.putExtra(IntentUtils.IS_BROADCAST, true)
                startActivity(intent)
            }
            setChildOnClickListener(Child.LEFT_TOP) {
                createGroupClicked()
            }
        }

    }

    override fun showAds(): Boolean {
        return resources.getBoolean(R.bool.is_calls_ad_enabled)
    }

    private fun init(view: View) {
        rvChats = view.findViewById(R.id.rv_chats)
        adView = view.findViewById(R.id.ad_view)
        lava_fab_bottom_right = view.findViewById(R.id.lava_fab_bottom_right)

    }

    private fun createGroupClicked() {
        startActivity(Intent(requireActivity(), NewGroupActivity::class.java))
    }

    //add a listener for the last message if the user has replied from the notification
    private fun listenForMessagesChanges() {
        changeListener =
            OrderedRealmCollectionChangeListener<RealmResults<Chat>> { chats, changeSet ->
                val modifications = changeSet.changeRanges
                if (modifications.size != 0) {
                    val chat = chats[modifications[0].startIndex]
                    val lastMessage = chat!!.lastMessage
                    if (lastMessage != null && lastMessage.messageStat == MessageStat.PENDING
                        || lastMessage != null && lastMessage.messageStat == MessageStat.SENT
                    ) {
                        addMessageStatListener(chat.chatId, lastMessage)
                    }
                }
            }
    }

    //listen for lastMessage stat if it's received or read by the other user
    private fun listenForLastMessageStat() {
        lastMessageStatListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) return
                val `val` = dataSnapshot.getValue(Int::class.java)!!
                val key = dataSnapshot.key
                val chatId = dataSnapshot.ref.parent!!.key
                RealmHelper.getInstance().updateMessageStatLocally(key, chatId, `val`)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
    }

    private fun addVoiceMessageStatListener() {
        for (chat in chatList!!) {
            val lastMessage = chat.lastMessage
            val user = chat.user ?: continue
            if (!user.isBroadcastBool && lastMessage != null && lastMessage.type != MessageType.GROUP_EVENT && lastMessage.isVoiceMessage
                && lastMessage.fromId == FireManager.uid && !lastMessage.isVoiceMessageSeen
            ) {
                val reference = FireConstants.voiceMessageStat.child(lastMessage.chatId)
                    .child(lastMessage.messageId)
                fireListener!!.addListener(reference, voiceMessageListener)
            }
        }
    }

    private fun addMessageStatListener() {
        for (chat in chatList ?: emptyList<Chat>()) {
            val lastMessage = chat.lastMessage
            val user = chat.user ?: continue
            if (user.isBroadcastBool && lastMessage != null && lastMessage.type != MessageType.GROUP_EVENT && lastMessage.messageStat != MessageStat.READ) {
                val reference =
                    FireConstants.messageStat.child(chat.chatId).child(lastMessage.messageId)
                fireListener!!.addListener(reference, lastMessageStatListener)
            }
        }
    }

    private fun addMessageStatListener(chatId: String, lastMessage: Message?) {
        if (lastMessage != null && lastMessage.type != MessageType.GROUP_EVENT && lastMessage.messageStat != MessageStat.READ) {
            val reference = FireConstants.messageStat.child(chatId).child(lastMessage.messageId)
            fireListener!!.addListener(reference, lastMessageStatListener)
        }
    }

    //if the lastMessage is a Voice message then we want to
    //listen if it's listened by the other user
    private fun listenForVoiceMessageStat() {
        voiceMessageListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) {
                    return
                }
                val key = dataSnapshot.key
                val chatId = dataSnapshot.ref.parent!!.key
                RealmHelper.getInstance().updateVoiceMessageStatLocally(key, chatId)
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
    }

    //listen if other user is typing to this user
    private fun listenForTypingStat() {
        typingEventListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value == null) return
                val stat = dataSnapshot.getValue(Int::class.java)!!
                val uid = dataSnapshot.ref.parent!!.key

                //create temp chat object to get the index of the uid
                val chat = Chat()
                chat.chatId = uid
                val i = chatList?.indexOf(chat) ?: -1
                //if chat is not exists in the list return
                if (i == -1) return
                val vh = rvChats!!.findViewHolderForAdapterPosition(i) as ChatsHolder? ?: return
                adapter!!.typingStatHashmap[chat.chatId] = stat
                val typingTv = vh.tvTypingStat
                val lastMessageTv = vh.tvLastMessage
                val lastMessageReadIcon = vh.imgReadTagChats


                //if other user is typing or recording to this user
                //then hide last message textView with all its contents
                if (stat == TypingStat.TYPING || stat == TypingStat.RECORDING) {
                    lastMessageTv.visibility = View.GONE
                    lastMessageReadIcon.visibility = View.GONE
                    typingTv.visibility = View.VISIBLE
                    if (stat == TypingStat.TYPING) typingTv.text =
                        resources.getString(R.string.typing) else if (stat == TypingStat.RECORDING) typingTv.text =
                        resources.getString(R.string.recording)

                    //in case there is no typing or recording event
                    //revert back to normal mode and show last message
                } else {
                    adapter!!.typingStatHashmap.remove(chat.chatId)
                    typingTv.visibility = View.GONE
                    lastMessageTv.visibility = View.VISIBLE
                    val lastMessage = chatList!![i]!!.lastMessage
                    if (lastMessage != null && lastMessage.type != MessageType.GROUP_EVENT && !MessageType.isDeletedMessage(
                            lastMessage.type
                        )
                        && lastMessage.fromId == FireManager.uid
                    ) {
                        lastMessageReadIcon.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        }
    }

    //adding typing listeners for all chats
    private fun addTypingStatListener() {
        if (!FireManager.isLoggedIn()) return
        for (chat in chatList!!) {
            val user = chat.user ?: continue
            if (user.isGroupBool && user.group.isActive) {
                if (groupTypingList == null) groupTypingList = ArrayList()
                val groupTyping = GroupTyping(user.group.users, user.uid, this)
                groupTypingList!!.add(groupTyping)
            } else {
                val receiverUid = user.uid
                val typingStat = FireConstants.mainRef.child("typingStat").child(receiverUid)
                    .child(FireManager.uid)
                fireListener!!.addListener(typingStat, typingEventListener)
            }
        }
    }


    private fun setTheAdapter() {
        adapter = ChatsAdapter(
            chatList,
            true,
            requireActivity(),
            this@FragmentChats
        )
        linearLayoutManager = LinearLayoutManager(activity)
        rvChats!!.layoutManager = linearLayoutManager
        rvChats!!.adapter = adapter

    }

    override fun onTyping(state: Int, groupId: String, user: User?) {
        val tempChat = Chat()
        tempChat.chatId = groupId
        val i = chatList!!.indexOf(tempChat)
        if (i == -1) return
        if (user == null) return
        val chat = chatList!![i]
        val vh = rvChats!!.findViewHolderForAdapterPosition(i) as ChatsHolder? ?: return
        adapter!!.typingStatHashmap[chat!!.chatId] = state
        val typingTv = vh.tvTypingStat
        val lastMessageTv = vh.tvLastMessage
        val lastMessageReadIcon = vh.imgReadTagChats


        //if other user is typing or recording to this user
        //then hide last message textView with all its contents
        if (state == TypingStat.TYPING || state == TypingStat.RECORDING) {
            lastMessageTv.visibility = View.GONE
            lastMessageReadIcon.visibility = View.GONE
            typingTv.visibility = View.VISIBLE
            typingTv.text = user.userName + " is " + TypingStat.getStatString(activity, state)
        }
    }

    override fun onAllNotTyping(groupId: String) {
        val tempChat = Chat()
        tempChat.chatId = groupId
        val i = chatList!!.indexOf(tempChat)
        if (i == -1) return
        val chat = chatList!![i]
        val vh = rvChats!!.findViewHolderForAdapterPosition(i) as ChatsHolder? ?: return
        val typingTv = vh.tvTypingStat
        val lastMessageTv = vh.tvLastMessage
        val lastMessageReadIcon = vh.imgReadTagChats
        adapter!!.typingStatHashmap.remove(chat!!.chatId)
        typingTv.visibility = View.GONE
        lastMessageTv.visibility = View.VISIBLE
        val lastMessage = chatList!![i]!!.lastMessage
        if (lastMessage != null && lastMessage.type != MessageType.GROUP_EVENT && !MessageType.isDeletedMessage(
                lastMessage.type
            )
            && lastMessage.fromId == FireManager.uid
        ) {
            lastMessageReadIcon.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        addTypingStatListener()
        addVoiceMessageStatListener()
        addMessageStatListener()
        chatList?.addChangeListener(changeListener)
    }

    override fun onPause() {
        super.onPause()
        fireListener!!.cleanup()
        if (groupTypingList != null) {
            for (groupTyping in groupTypingList!!) {
                groupTyping.cleanUp()
            }
        }
        chatList?.removeChangeListener(changeListener)
        adapter?.exitActionMode()
        actionMode?.finish()

    }


    override fun onQueryTextChange(newText: String?) {
        super.onQueryTextChange(newText)
        adapter?.filter(newText)
    }

    override fun onSearchClose() {
        super.onSearchClose()

    }


    private fun checkNetworkConnection() {
        cld = ConnectionLiveData(requireActivity().application)
        cld.observe(viewLifecycleOwner) { isConnected ->
            if (isConnected) {
                RetrofitInstance.api.getAds(token = TOKEN).clone()
                    .enqueue(object : Callback<AdsList> {
                        override fun onResponse(call: Call<AdsList>, response: Response<AdsList>) {
                            adapter?.let {
                                it.setAds(
                                    response.body()?.data?.get(0) ?: null
                                )
                                chatList?.let { list -> it.notifyItemRangeChanged(0, list.size) }
                                if (Random().nextInt(100) == 50) {
                                    response.body()?.data?.get(0)?.let {
                                        runAds(requireView(), it)
                                    }
                                }
                            }
                        }

                        override fun onFailure(call: Call<AdsList>, t: Throwable) {

                        }

                    })
            } else {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.no_internt),
                    Toast.LENGTH_SHORT
                ).show()
                chatList?.let { list -> adapter?.notifyItemRangeChanged(0, list.size) }
            }

        }
    }


    override fun onClick(chat: Chat, view: View) {
//        if isInAction mode then select or remove the clicked chat from selectedActionList
        if (isInActionMode()) {
            //if it's selected ,remove it
            if (selectedChats.contains(chat))
                itemRemoved(view, chat);

            //otherwise add it to list
            else
                itemAdded(view, chat);
            //if it's not in actionMode start the chatActivity
        } else {
            chat.let {
                if (it.user != null) {
                    val user = it.user
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra(IntentUtils.UID, user.uid)
                    startActivity(intent);
                }
            }

        }
    }

    private fun isInActionMode() = actionMode != null

    private fun itemAdded(itemView: View, chat: Chat) {

        selectedChats.add(chat)
        adapter?.itemAdded(itemView, chat)

        val itemsCount = selectedChats.size

        actionMode?.title = itemsCount.toString() + ""

        updateActionMenuItems(itemsCount)
    }

    private fun updateActionMenuItems(itemsCount: Int) {
        if (itemsCount > 1) {
            if (isHasMutedItem)
                setMenuItemVisibility(false)
            else
                updateMutedIcon(
                    actionMenu?.findItem(R.id.menu_item_mute),
                    false
                )//if there is no muted item then the user may select multiple chats and mute them all in once


        } else if (itemsCount == 1 && selectedChats.size == 1) {

            val isMuted = selectedChats[0].isMuted
            //in case if it's hidden before
            setMenuItemVisibility(true)
            updateMutedIcon(actionMenu?.findItem(R.id.menu_item_mute), isMuted)

        }

        updateGroupItems()
    }

    private fun itemRemoved(itemView: View, chat: Chat) {
        selectedChats.remove(chat)
        adapter?.itemRemoved(itemView, chat)
        actionMode?.title = selectedChats.size.toString() + ""
        if (selectedChats.isEmpty())
            exitActionMode()
        else
            updateActionMenuItems(selectedChats.size)
    }

    private fun exitActionMode() {
        actionMode?.finish()
    }

    override fun onLongClick(chat: Chat, view: View) {
        if (!isInActionMode()) {
            callback!!.startTheActionMode(this)
            itemAdded(view, chat)
        }


    }


    override fun userProfileClicked(user: User) {
        //start user profile (Dialog-Like Activity)
        val intent = Intent(requireContext(), ProfilePhotoDialog::class.java)
        intent.putExtra(IntentUtils.UID, user.uid)
        startActivity(intent)

    }

    override fun onBind(pos: Int, chat: Chat?) {
        chat?.let { chat ->
            chat.user?.let { user ->
                viewModel.fetchUserImage(pos, user)

            }
        }
    }

    override fun feedback(title: String?) {
        Toast.makeText(requireActivity(), title, Toast.LENGTH_LONG).show()
    }

    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        this.actionMode = actionMode

        actionMode.menuInflater.inflate(R.menu.menu_action_chat_list, menu)
        this.actionMenu = menu
        actionMode.title = "1"
        return true
    }

    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return false
    }

    override fun onDestroyActionMode(actionMode: ActionMode) {
        this.actionMode = null
        selectedChats.clear()
        adapter?.exitActionMode()
    }

    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
        if (actionMode != null && menuItem != null) {
            when (menuItem.itemId) {
                R.id.menu_item_delete -> deleteItemClicked()

                R.id.menu_item_mute -> muteItemClicked()

                R.id.exit_group_item -> exitGroupClicked()

            }

            return true
        }
        return false
    }

    private fun muteItemClicked() {
        val selectedItems = selectedChats
        for (chat in selectedItems) {
            if (chat.isMuted) {
                RealmHelper.getInstance().setMuted(chat.chatId, false)
            } else {
                RealmHelper.getInstance().setMuted(chat.chatId, true)
            }
        }

        exitActionMode()
    }

    private fun exitGroupClicked() {
        if (!NetworkHelper.isConnected(MyApp.context()))
            return

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(R.string.confirmation)
            .setMessage(R.string.exit_group)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { _, _ ->
                val selectedItems = selectedChats
                for (chat in selectedItems) {
                    disposables.add(
                        groupManager.exitGroup(chat.chatId, FireManager.uid).subscribe({
                            RealmHelper.getInstance().exitGroup(chat.chatId)
                            val groupEvent = GroupEvent(
                                SharedPreferencesManager.getPhoneNumber(),
                                GroupEventTypes.USER_LEFT_GROUP,
                                null
                            )
                            groupEvent.createGroupEvent(chat.user, null)
                        }, { throwable ->
                            Toast.makeText(requireContext(), R.string.error, Toast.LENGTH_SHORT)
                                .show();
                        })
                    )
                }
                exitActionMode()
            })
            .show()


    }

    private fun deleteItemClicked() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle(R.string.confirmation)
            .setMessage(R.string.delete_conversation_confirmation)
            .setNegativeButton(R.string.no, null)
            .setPositiveButton(R.string.yes, DialogInterface.OnClickListener { _, _ ->
                val selectedItems = selectedChats
                for (chat in selectedItems) {
                    RealmHelper.getInstance().deleteChat(chat.chatId)
                }
                exitActionMode()
            })
            .show()

    }

    override fun onDestroy() {
        super.onDestroy()
        adapter?.onDestroy()
    }

    private fun runAds(view: View, adsModel: AdsModel?) {
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        val _ads_: View? = view.findViewById(R.id.layout_adv_full)
        val videoView: VideoView? = _ads_?.findViewById(R.id.video_adv)
        val ic_volume_: ImageView? = _ads_?.findViewById(R.id.ic_volume_)
        val ic_option_: ImageView? = _ads_?.findViewById(R.id.ic_option_)
        val image_adv: ImageView? = _ads_?.findViewById(R.id.image_adv)
        var isMute = false
        _ads_?.visibility = View.VISIBLE
        //        Log.e(TAG, "runAds: " + adsModel.toString());
        if (adsModel != null) {
            try {
                if (adsModel.media.contains(".mp4") || adsModel.media.contains(".mov") || adsModel.media.contains(
                        ".wmv"
                    ) || adsModel.media.contains(".avi") || adsModel.media.contains(".mkv") || adsModel.media.contains(
                        ".webm"
                    ) || adsModel.media.contains(".avchd")
                ) {
                    videoView?.visibility = View.VISIBLE
                    videoView?.setMedia(Uri.parse(BASE_URL_VIDEO + adsModel.media))
                } else {
                    videoView?.visibility = View.GONE
                    image_adv?.visibility = View.VISIBLE
                    ic_volume_?.visibility = View.GONE
                    if (image_adv != null) {
                        Glide.with(requireActivity()).load(BASE_URL_VIDEO + adsModel.media)
                            .into(image_adv)
                    }
                }
            } catch (e: NullPointerException) {
                Log.e(TAG, "onBindViewHolder: " + e.message)
                _ads_?.visibility = View.GONE
            }
        } else {
            _ads_?.visibility = View.GONE
        }
        ic_option_?.setOnClickListener { view: View? ->
            val popupMenu =
                PopupMenu(requireActivity(), view)
            popupMenu.menuInflater.inflate(R.menu.menu_adv_option, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { menuItem -> // Toast message on menu item clicked

                return@setOnMenuItemClickListener true
            }
            // Showing the popup menu
            popupMenu.show()
        }
        ic_volume_?.setOnClickListener { view: View? ->
            if (!isMute) {
                videoView?.volume = 0f
                ic_volume_.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_volume_off
                    )
                )
                isMute = true
            } else {
                videoView?.volume = 100f
                ic_volume_.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireActivity(),
                        R.drawable.ic_volume_up
                    )
                )
                isMute = false
            }
        }
        image_adv?.setImageDrawable(
            ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.logo
            )
        )
        videoView?.start()
    }
}