package com.example.keydates

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import com.example.keydates.databinding.ActivityMainBinding // Adjust the package name

data class Event(
    val id: String,
    var title: String,
    var description: String,
    var day: Int,
    var month: Int,
    var year: Int,
    var hour: Int,
    var minute: Int
)


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: EventAdapter
    private val sharedPreferences by lazy { getSharedPreferences("events", Context.MODE_PRIVATE) }
    private val gson by lazy { Gson() }
    private val eventList = mutableListOf<Event>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadEvents()

        adapter = EventAdapter(eventList, this::onEditEvent, this::onDeleteEvent)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.addEventButton.setOnClickListener {
            EventDialogFragment { event -> addOrUpdateEvent(event) }.show(supportFragmentManager, "EventDialog")
        }
    }

    private fun loadEvents() {
        val json = sharedPreferences.getString("eventList", null)
        val type = object : TypeToken<MutableList<Event>>() {}.type
        json?.let { eventList.addAll(gson.fromJson(it, type)) }
    }

    private fun saveEvents() {
        sharedPreferences.edit().putString("eventList", gson.toJson(eventList)).apply()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addOrUpdateEvent(event: Event) {
        val index = eventList.indexOfFirst { it.id == event.id }
        if (index != -1) {
            eventList[index] = event
        } else {
            eventList.add(event)
        }
        adapter.notifyDataSetChanged()
        saveEvents()
        scheduleNotification(event)
    }

    private fun onEditEvent(event: Event) {
        EventDialogFragment(event) { updatedEvent -> addOrUpdateEvent(updatedEvent) }
            .show(supportFragmentManager, "EventDialog")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onDeleteEvent(event: Event) {
        eventList.remove(event)
        adapter.notifyDataSetChanged()
        saveEvents()
    }

    @SuppressLint("MissingPermission")
    private fun scheduleNotification(event: Event) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("eventTitle", event.title)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, event.id.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(event.year, event.month - 1, event.day, event.hour, event.minute, 0)
        }
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
    }
}

