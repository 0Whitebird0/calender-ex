package com.example.calender_ex

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.calender_ex.databinding.ActivityMainBinding
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.view.MonthDayBinder
import com.kizitonwose.calendar.view.MonthHeaderFooterBinder
import com.kizitonwose.calendar.view.ViewContainer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var calendarViewModel: CalendarViewModel
    private lateinit var eventAdapter: EventAdapter

    private var eventsByDate = mapOf<LocalDate, List<EventUiModel>>()
    private var selectedDate: LocalDate? = LocalDate.now()
    private val today = LocalDate.now()

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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, 1001)
        } else {
            setupCalendarScreen()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            setupCalendarScreen()
        } else {
            Toast.makeText(this, "캘린더 권한이 필요합니다", Toast.LENGTH_LONG).show()
        }
    }

    inner class DayViewContainer(view: View) : ViewContainer(view) {
        val dayText: TextView = view.findViewById(R.id.calendarDayText)
        val eventTextView: TextView = view.findViewById(R.id.eventTextView)
        val eventTextView2: TextView = view.findViewById(R.id.eventTextView2)
        lateinit var day: CalendarDay

        init {
            view.setOnClickListener {
                if (day.position == DayPosition.MonthDate) {
                    if (selectedDate != day.date) {
                        val oldDate = selectedDate
                        selectedDate = day.date
                        binding.calendarView.notifyDateChanged(day.date)
                        oldDate?.let { binding.calendarView.notifyDateChanged(it) }
                    }
                }
            }
            view.setOnLongClickListener {
                if (day.position == DayPosition.MonthDate) {
                    selectedDate = day.date
                    showAddEventDialog()
                }
                true
            }
        }
    }

    inner class MonthViewContainer(view: View) : ViewContainer(view) {
        val textView: TextView = view.findViewById(R.id.calendarHeaderText)
    }

    private fun setupCalendarScreen() {
        eventAdapter = EventAdapter(
            onEdit = { event -> showEditDialog(event) },
            onDelete = { event -> calendarViewModel.deleteEvent(contentResolver, event.id) }
        )
        binding.recyclerEvents.adapter = eventAdapter
        binding.recyclerEvents.layoutManager = LinearLayoutManager(this)

        binding.calendarView.dayBinder = object : MonthDayBinder<DayViewContainer> {
            override fun create(view: View) = DayViewContainer(view)
            override fun bind(container: DayViewContainer, day: CalendarDay) {
                container.day = day
                val dayText = container.dayText
                val eventTextView = container.eventTextView
                val eventTextView2 = container.eventTextView2
                dayText.text = day.date.dayOfMonth.toString()

                dayText.background = null
                eventTextView.visibility = View.GONE
                eventTextView2.visibility = View.GONE

                if (day.position == DayPosition.MonthDate) {
                    // Set text color based on selection and day of week
                    when {
                        day.date == selectedDate -> {
                            dayText.setTextColor(Color.WHITE)
                            dayText.setBackgroundResource(R.drawable.today_background)
                        }
                        day.date.dayOfWeek == DayOfWeek.SUNDAY -> {
                            dayText.setTextColor(Color.RED)
                        }
                        day.date.dayOfWeek == DayOfWeek.SATURDAY -> {
                            dayText.setTextColor(Color.BLUE)
                        }
                        else -> {
                            dayText.setTextColor(Color.BLACK)
                        }
                    }

                    // Show event title
                    val events = eventsByDate[day.date]
                    if (!events.isNullOrEmpty()) {
                        eventTextView.text = events.first().title
                        eventTextView.visibility = View.VISIBLE

                        // Show second event for testing
                        eventTextView2.text = "팀 회의"
                        eventTextView2.visibility = View.VISIBLE
                    }

                } else { // Dates in other months
                    dayText.setTextColor(Color.GRAY)
                }
            }
        }

        binding.calendarView.monthHeaderBinder = object : MonthHeaderFooterBinder<MonthViewContainer> {
            override fun create(view: View) = MonthViewContainer(view)
            override fun bind(container: MonthViewContainer, month: CalendarMonth) {
                val formatter = DateTimeFormatter.ofPattern("yyyy년 MMMM")
                container.textView.text = month.yearMonth.format(formatter)
            }
        }

        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)
        binding.calendarView.setup(startMonth, endMonth, DayOfWeek.SUNDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        binding.btnAddEvent.setOnClickListener { showAddEventDialog() }
        binding.btnShowEvents.setOnClickListener {
            calendarViewModel.loadEventsForCurrentMonth(contentResolver)
        }

        calendarViewModel.events.observe(this) { events ->
            eventAdapter.submitList(events)
            eventsByDate = events.groupBy {
                java.time.Instant.ofEpochMilli(it.startTime).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
            }
            binding.calendarView.notifyCalendarChanged()
        }

        calendarViewModel.calendars.observe(this) {
            // Not used for now
        }

        calendarViewModel.loadCalendars(contentResolver)
        calendarViewModel.loadEventsForCurrentMonth(contentResolver)
    }

    private fun showAddEventDialog() {
        val input = EditText(this).apply {
            hint = "일정 제목"
            setPadding(48.dpToPx(), 24.dpToPx(), 48.dpToPx(), 24.dpToPx())
        }
        AlertDialog.Builder(this)
            .setTitle("일정 추가 (${selectedDate ?: LocalDate.now()})")
            .setView(input)
            .setPositiveButton("추가") { _, _ ->
                val title = input.text.toString()
                if (title.isNotEmpty()) {
                    val dateToUse = selectedDate ?: LocalDate.now()
                    val startTime = dateToUse.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endTime = startTime + 60 * 60 * 1000 // 1 hour later

                    val firstCalendar = calendarViewModel.calendars.value?.firstOrNull()
                    firstCalendar?.let { calendar ->
                        calendarViewModel.insertEvent(contentResolver, calendar.id, title, startTime, endTime)
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