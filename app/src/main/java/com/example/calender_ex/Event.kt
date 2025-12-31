package com.example.calender_ex

data class EventUiModel(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val description: String?,
    val calendarId: Long,
    val calendarName: String
)

data class Event(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long
)

data class CalendarInfo(
    val id: Long,
    val name: String,
    val accountName: String,
    val accountType: String
)
