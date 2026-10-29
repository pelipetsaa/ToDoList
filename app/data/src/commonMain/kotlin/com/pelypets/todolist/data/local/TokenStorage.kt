package com.pelypets.todolist.data.local

import com.liftric.kvault.KVault

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String,
)

interface TokenStorage {
    fun read(): StoredTokens?
    fun save(tokens: StoredTokens)
    fun clear()
}

/** Keychain on iOS, EncryptedSharedPreferences on Android. */
class KVaultTokenStorage(private val vault: KVault) : TokenStorage {

    override fun read(): StoredTokens? {
        val access = vault.string(KEY_ACCESS) ?: return null
        val refresh = vault.string(KEY_REFRESH) ?: return null
        return StoredTokens(access, refresh)
    }

    override fun save(tokens: StoredTokens) {
        vault.set(KEY_ACCESS, tokens.accessToken)
        vault.set(KEY_REFRESH, tokens.refreshToken)
    }

    override fun clear() {
        vault.deleteObject(KEY_ACCESS)
        vault.deleteObject(KEY_REFRESH)
    }

    private companion object {
        const val KEY_ACCESS = "auth.access_token"
        const val KEY_REFRESH = "auth.refresh_token"
    }
}
