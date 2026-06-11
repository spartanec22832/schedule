package com.sfedu.schedule.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sfedu.schedule.data.ScheduleRepository
import com.sfedu.schedule.model.ScheduleAppData
import com.sfedu.schedule.model.ScheduleEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScheduleUiState(
    val isLoading: Boolean = true,
    val appData: ScheduleAppData = ScheduleAppData.default()
)

class ScheduleViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = ScheduleRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appDataFlow.collect { savedData ->
                _uiState.value = ScheduleUiState(
                    isLoading = false,
                    appData = savedData
                )
            }
        }
    }

    fun updateDaySchedule(dayIndex: Int, updatedEntries: List<ScheduleEntry>) {
        val currentData = _uiState.value.appData
        val currentSchedule = currentData.scheduleList

        if (dayIndex !in currentSchedule.indices) return

        val currentDay = currentSchedule[dayIndex]
        val updatedDay = currentDay.copy(entries = updatedEntries)

        val updatedSchedule = currentSchedule.toMutableList().apply {
            this[dayIndex] = updatedDay
        }

        val newSubjects = updatedEntries
            .map { it.subject }
            .filter { it.isNotBlank() }
            .toSet()

        val oldDayNotes = currentData.takeWithYouMap[currentDay.dayName].orEmpty()

        val cleanedDayNotes = oldDayNotes.filterKeys { subject ->
            subject in newSubjects
        }

        val updatedTakeWithYouMap = currentData.takeWithYouMap.toMutableMap().apply {
            this[currentDay.dayName] = cleanedDayNotes
        }

        saveData(
            currentData.copy(
                scheduleList = updatedSchedule,
                takeWithYouMap = updatedTakeWithYouMap
            )
        )
    }

    fun addTakeWithYouItem(
        dayName: String,
        subject: String,
        item: String
    ) {
        val trimmedItem = item.trim()
        if (trimmedItem.isBlank()) return

        val currentData = _uiState.value.appData

        val updatedDayMap = currentData.takeWithYouMap[dayName]
            .orEmpty()
            .toMutableMap()

        val updatedItems = updatedDayMap[subject]
            .orEmpty()
            .toMutableList()
            .apply {
                add(trimmedItem)
            }

        updatedDayMap[subject] = updatedItems

        val updatedTakeWithYouMap = currentData.takeWithYouMap.toMutableMap().apply {
            this[dayName] = updatedDayMap
        }

        saveData(
            currentData.copy(
                takeWithYouMap = updatedTakeWithYouMap
            )
        )
    }

    fun removeTakeWithYouItem(
        dayName: String,
        subject: String,
        itemIndex: Int
    ) {
        val currentData = _uiState.value.appData

        val updatedDayMap = currentData.takeWithYouMap[dayName]
            .orEmpty()
            .toMutableMap()

        val currentItems = updatedDayMap[subject].orEmpty()

        if (itemIndex !in currentItems.indices) return

        val updatedItems = currentItems.toMutableList().apply {
            removeAt(itemIndex)
        }

        updatedDayMap[subject] = updatedItems

        val updatedTakeWithYouMap = currentData.takeWithYouMap.toMutableMap().apply {
            this[dayName] = updatedDayMap
        }

        saveData(
            currentData.copy(
                takeWithYouMap = updatedTakeWithYouMap
            )
        )
    }

    private fun saveData(appData: ScheduleAppData) {
        _uiState.value = _uiState.value.copy(appData = appData)

        viewModelScope.launch {
            repository.saveAppData(appData)
        }
    }
}