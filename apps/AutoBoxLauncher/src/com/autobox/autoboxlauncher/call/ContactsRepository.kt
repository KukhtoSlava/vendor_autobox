package com.autobox.autoboxlauncher.call

import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class Contact(
    val id: Long,
    val name: String,
    val numbers: List<String>,
    val photoUri: Uri?,
)

data class RecentCall(
    val name: String?,
    val number: String,
    val type: Int,
    val dateMs: Long,
)

object ContactsRepository {

    suspend fun loadContacts(context: Context): List<Contact> = withContext(Dispatchers.IO) {
        val map = linkedMapOf<Long, Contact>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        ) ?: return@withContext emptyList()

        cursor.use {
            val idCol     = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameCol   = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol    = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoCol  = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

            while (it.moveToNext()) {
                val id    = it.getLong(idCol)
                val name  = it.getString(nameCol) ?: continue
                val num   = it.getString(numCol)?.trim() ?: continue
                val photo = it.getString(photoCol)?.let { u -> Uri.parse(u) }

                val existing = map[id]
                map[id] = if (existing != null)
                    existing.copy(numbers = existing.numbers + num)
                else
                    Contact(id = id, name = name, numbers = listOf(num), photoUri = photo)
            }
        }
        map.values.toList()
    }

    suspend fun loadRecentCalls(context: Context, limit: Int = 50): List<RecentCall> =
        withContext(Dispatchers.IO) {
            val uri = CallLog.Calls.CONTENT_URI.buildUpon()
                .appendQueryParameter("limit", limit.toString())
                .build()
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                ),
                null, null,
                "${CallLog.Calls.DATE} DESC"
            ) ?: return@withContext emptyList()

            buildList {
                cursor.use {
                    val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                    val numCol  = it.getColumnIndex(CallLog.Calls.NUMBER)
                    val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
                    val dateCol = it.getColumnIndex(CallLog.Calls.DATE)

                    while (it.moveToNext()) {
                        val num = it.getString(numCol)?.trim() ?: continue
                        add(
                            RecentCall(
                                name   = it.getString(nameCol)?.takeIf { n -> n.isNotBlank() },
                                number = num,
                                type   = it.getInt(typeCol),
                                dateMs = it.getLong(dateCol),
                            )
                        )
                    }
                }
            }
        }
}
