package com.devlomi.fireapp.adapters.messaging.holders

import android.content.Context
import android.view.View
import com.aghajari.emojiview.view.AXEmojiTextView
import com.devlomi.fireapp.R
import com.devlomi.fireapp.adapters.messaging.holders.base.BaseReceivedHolder
import com.devlomi.fireapp.model.realms.Message
import com.devlomi.fireapp.model.realms.User

// received message with type text
class ReceivedTextHolder(context: Context, itemView: View) : BaseReceivedHolder(context,itemView) {

    private var tvMessageContent: AXEmojiTextView = itemView.findViewById(R.id.tv_message_content)

    override fun bind(message: Message,user: User) {
        super.bind(message,user)
        tvMessageContent.text = message.content
    }


}