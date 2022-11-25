package com.devlomi.fireapp.utils.enc

import com.devlomi.fireapp.R
import com.devlomi.fireapp.model.constants.EncryptionType
import com.devlomi.fireapp.model.realms.Message
import com.devlomi.fireapp.utils.MyApp.Companion.context

object EncryptionTypeUseCase {
     fun getEncryptionType(message: Message): String? {
        val encryptionTypeSetting =
            context().getString(R.string.encryption_type)
        return if (message.isGroup && !encryptionTypeSetting.equals(
                EncryptionType.NONE,
                ignoreCase = true
            )
        ) {
            EncryptionType.AES
        } else encryptionTypeSetting
    }
}