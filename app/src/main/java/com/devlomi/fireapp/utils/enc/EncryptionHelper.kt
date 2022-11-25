package com.devlomi.fireapp.utils.enc

import com.devlomi.fireapp.R
import com.devlomi.fireapp.model.constants.EncryptionType
import com.devlomi.fireapp.utils.MyApp
import com.devlomi.fireapp.utils.enc.aes.AESCrypto
import com.devlomi.fireapp.utils.enc.ethree.EthreeHelper
import kotlinx.coroutines.CoroutineScope

class EncryptionHelper {

    private val aesCrypto: AESCrypto by lazy {
        AESCrypto()
    }

    suspend fun encrypt(
        scope: CoroutineScope,
        singleUidOrMultiple: SingleUidOrMultiple,
        message: String,
        encryptionType: String,
    ): String {
        return when {
            encryptionType.equals(
                EncryptionType.AES,
                ignoreCase = true
            ) -> aesCrypto.encryptPlainTextWithRandomIV(message)
            encryptionType.equals(EncryptionType.E2E, ignoreCase = true) -> {
                if (singleUidOrMultiple.uids != null) {
                    EthreeHelper.encryptMessage(scope, singleUidOrMultiple.uids!!, message)
                } else {
                    EthreeHelper.encryptMessage(scope, singleUidOrMultiple.uid!!, message)
                }
            }
            else -> message
        }
    }


}