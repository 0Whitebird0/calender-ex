package com.example.calender_ex

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calender_ex.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
//1번 테스트
    private lateinit var binding: ActivityMainBinding
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        calendarViewModel = ViewModelProvider(this)[CalendarViewModel::class.java]
        requestCalendarPermissions()
    }

    private fun requestCalendarPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        } else {
            setupCalendarScreen()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupCalendarScreen()
        } else {
            Toast.makeText(this, "캘린더 권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupCalendarScreen() {
        eventAdapter = EventAdapter(
            onEdit = { event -> showEditDialog(event) },
            onDelete = { event ->
                calendarViewModel.deleteEvent(contentResolver, event.id)
            }
        )

        binding.recyclerEvents.adapter = eventAdapter
        binding.recyclerEvents.layoutManager = LinearLayoutManager(this)

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // In a real app, you would load events for the selected date.
            // For this example, we'll just reload the current month's events.
            calendarViewModel.loadEventsForCurrentMonth(contentResolver)
        }

        binding.btnAddEvent.setOnClickListener { showAddEventDialog() }
        binding.btnShowEvents.setOnClickListener {
            calendarViewModel.loadEventsForCurrentMonth(contentResolver)
        }

        calendarViewModel.events.observe(this) { events ->
            eventAdapter.submitList(events)
        }

        calendarViewModel.calendars.observe(this) {
            // You could use this list to let the user choose a calendar
        }

        // Initial data load
        calendarViewModel.loadCalendars(contentResolver)
        calendarViewModel.loadEventsForCurrentMonth(contentResolver)
    }

    private fun showAddEventDialog() {
        val input = EditText(this).apply {
            hint = "일정 제목"
            setPadding(48.dpToPx(), 24.dpToPx(), 48.dpToPx(), 24.dpToPx())
        }

        AlertDialog.Builder(this)
            .setTitle("일정 추가")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val title = input.text.toString()
                if (title.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    val endTime = now + 60 * 60 * 1000 // 1 hour later

                    val firstCalendar = calendarViewModel.calendars.value?.firstOrNull()
                    firstCalendar?.let { calendar ->
                        calendarViewModel.insertEvent(contentResolver, calendar.id, title, now, endTime)
                    } ?: Toast.makeText(this, "캘린더를 먼저 로드하세요", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showEditDialog(event: EventUiModel) {
        val input = EditText(this).apply {
            hint = "새 제목"
            setText(event.title)
            setPadding(48.dpToPx(), 24.dpToPx(), 48.dpToPx(), 24.dpToPx())
        }
        AlertDialog.Builder(this)
            .setTitle("일정 수정")
            .setView(input)
            .setPositiveButton("수정") { _, _ ->
                val newTitle = input.text.toString()
                if (newTitle.isNotEmpty()) {
                    calendarViewModel.updateEvent(contentResolver, event.id, newTitle)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}