package com.sfedu.schedule.model

import kotlinx.serialization.Serializable

@Serializable
data class ScheduleEntry(
    val subject: String
)

@Serializable
data class DaySchedule(
    val dayName: String,
    val entries: List<ScheduleEntry> = emptyList()
)

@Serializable
data class ScheduleAppData(
    val scheduleList: List<DaySchedule> = defaultSchedule(),
    val takeWithYouMap: Map<String, Map<String, List<String>>> = emptyMap()
) {
    companion object {
        fun defaultSchedule(): List<DaySchedule> {
            return listOf(
                DaySchedule(
                    dayName = "Понедельник",
                    entries = listOf(
                        ScheduleEntry("Математика"),
                        ScheduleEntry("Русский язык")
                    )
                ),
                DaySchedule(
                    dayName = "Вторник",
                    entries = listOf(
                        ScheduleEntry("Физика")
                    )
                ),
                DaySchedule(
                    dayName = "Среда",
                    entries = listOf(
                        ScheduleEntry("Химия")
                    )
                ),
                DaySchedule(
                    dayName = "Четверг",
                    entries = listOf(
                        ScheduleEntry("История")
                    )
                ),
                DaySchedule(
                    dayName = "Пятница",
                    entries = listOf(
                        ScheduleEntry("География")
                    )
                ),
                DaySchedule(
                    dayName = "Суббота",
                    entries = listOf(
                        ScheduleEntry("Литература")
                    )
                )
            )
        }

        fun default(): ScheduleAppData {
            return ScheduleAppData(
                scheduleList = defaultSchedule(),
                takeWithYouMap = emptyMap()
            )
        }
    }
}