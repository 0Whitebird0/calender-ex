package com.example.calender_ex

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.provider.CalendarContract
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.util.Calendar

class CalendarViewModel : ViewModel() {

    private val _events = MutableLiveData<List<EventUiModel>>()
    val events: LiveData<List<EventUiModel>> = _events

    private val _calendars = MutableLiveData<List<CalendarInfo>>()
    val calendars: LiveData<List<CalendarInfo>> = _calendars

    fun loadEventsForCurrentMonth(contentResolver: ContentResolver) {
        viewModelScope.launch {
            val eventsList = mutableListOf<EventUiModel>()
            val calendar = Calendar.getInstance()
            val startTime = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
            val endTime = calendar.apply { add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis

            val uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.CALENDAR_ID,
                CalendarContract.Events.CALENDAR_DISPLAY_NAME
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ?"
            val selectionArgs = arrayOf(startTime.toString(), endTime.toString())

            val cursor = contentResolver.query(uri, projection, selection, selectionArgs, null)
            cursor?.use {
                val idCol = it.getColumnIndex(CalendarContract.Events._ID)
                val titleCol = it.getColumnIndex(CalendarContract.Events.TITLE)
                val startTimeCol = it.getColumnIndex(CalendarContract.Events.DTSTART)
                val endTimeCol = it.getColumnIndex(CalendarContract.Events.DTEND)
                val descriptionCol = it.getColumnIndex(CalendarContract.Events.DESCRIPTION)
                val calendarIdCol = it.getColumnIndex(CalendarContract.Events.CALENDAR_ID)
                val calendarNameCol = it.getColumnIndex(CalendarContract.Events.CALENDAR_DISPLAY_NAME)

                while (it.moveToNext()) {
                    eventsList.add(
                        EventUiModel(
                            id = it.getLong(idCol),
                            title = it.getString(titleCol),
                            startTime = it.getLong(startTimeCol),
                            endTime = it.getLong(endTimeCol),
                            description = it.getString(descriptionCol),
                            calendarId = it.getLong(calendarIdCol),
                            calendarName = it.getString(calendarNameCol)
                        )
                    )
                }
            }
            _events.postValue(eventsList)
        }
    }

    fun loadCalendars(contentResolver: ContentResolver) {
        viewModelScope.launch {
            val calendarsList = mutableListOf<CalendarInfo>()
            val uri = CalendarContract.Calendars.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            )

            val cursor = contentResolver.query(uri, projection, null, null, null)
            cursor?.use {
                val idCol = it.getColumnIndex(CalendarContract.Calendars._ID)
                val nameCol = it.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountNameCol = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val accountTypeCol = it.getColumnIndex(CalendarContract.Calendars.ACCOUNT_TYPE)

                while (it.moveToNext()) {
                    calendarsList.add(
                        CalendarInfo(
                            id = it.getLong(idCol),
                            name = it.getString(nameCol),
                            accountName = it.getString(accountNameCol),
                            accountType = it.getString(accountTypeCol)
                        )
                    )
                }
            }
            _calendars.postValue(calendarsList)
        }
    }

    fun insertEvent(contentResolver: ContentResolver, calendarId: Long, title: String, startTime: Long, endTime: Long) {
        viewModelScope.launch {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, endTime)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Seoul")
            }
            contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            loadEventsForCurrentMonth(contentResolver)
        }
    }

    fun updateEvent(contentResolver: ContentResolver, eventId: Long, newTitle: String) {
        viewModelScope.launch {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, newTitle)
            }
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            contentResolver.update(uri, values, null, null)
            loadEventsForCurrentMonth(contentResolver)
        }
    }

    fun deleteEvent(contentResolver: ContentResolver, eventId: Long) {
        viewModelScope.launch {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            contentResolver.delete(uri, null, null)
            loadEventsForCurrentMonth(contentResolver)
        }
    }
}
