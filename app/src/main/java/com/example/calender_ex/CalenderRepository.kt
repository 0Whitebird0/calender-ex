package com.example.calender_ex

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import java.util.TimeZone

class CalendarRepository(private val context: Context) {

    fun getCalendars(): LiveData<List<CalendarInfo>> = liveData {
        val calendars = mutableListOf<CalendarInfo>()

        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE
        )

        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            "${CalendarContract.Calendars.VISIBLE} = 1",
            null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val accountNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val accountTypeIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)

            while (cursor.moveToNext()) {
                calendars.add(
                    CalendarInfo(
                        id = cursor.getLong(idIndex),
                        name = cursor.getString(nameIndex) ?: "",
                        accountName = cursor.getString(accountNameIndex) ?: "",
                        accountType = cursor.getString(accountTypeIndex) ?: ""
                    )
                )
            }
        }
        emit(calendars)
    }

    // EventUiModel 제거하고 Event만 사용
    fun getEventsForDateRange(startMillis: Long, endMillis: Long): LiveData<List<Event>> = liveData {
        val events = mutableListOf<Event>()  // EventUiModel → Event

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND
            // calendarName 제거 (필요시 나중에 추가)
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)

        context.contentResolver.query(
            builder.build(),
            projection,
            null, null, null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events._ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val descIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val calIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.CALENDAR_ID)
            val calNameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                events.add(
                    Event(
                        id = cursor.getLong(idIndex),
                        title = cursor.getString(titleIndex) ?: "",
                        startTime = cursor.getLong(startIndex),
                        endTime = cursor.getLong(endIndex),
                    )
                )
            }
        }
        emit(events)
    }

    suspend fun insertEvent(
        calendarId: Long,
        title: String,
        startMillis: Long,
        endMillis: Long
    ): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?.lastPathSegment?.toLong() ?: -1L
    }

    suspend fun updateEvent(eventId: Long, title: String) {
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, title)
        }

        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.update(uri, values, null, null)
    }

    suspend fun deleteEvent(eventId: Long) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        context.contentResolver.delete(uri, null, null)
    }
}
