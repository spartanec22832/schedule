package com.sfedu.schedule

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController

// ---------- Данные ----------

data class ScheduleEntry(var subject: String)

data class DaySchedule(
    val dayName: String,
    val entries: SnapshotStateList<ScheduleEntry> = mutableStateListOf()
)

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme { AppNavGraph() }
        }
    }
}

// ---------- Нав. функции ----------

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@SuppressLint("UnrememberedMutableState")
@Composable
fun AppNavGraph() {
    val initial = listOf(
        DaySchedule("Понедельник", mutableStateListOf(ScheduleEntry("Математика"), ScheduleEntry("Русский язык"))),
        DaySchedule("Вторник", mutableStateListOf(ScheduleEntry("Физика"))),
        DaySchedule("Среда", mutableStateListOf(ScheduleEntry("Химия"))),
        DaySchedule("Четверг", mutableStateListOf(ScheduleEntry("История"))),
        DaySchedule("Пятница", mutableStateListOf(ScheduleEntry("География"))),
        DaySchedule("Суббота", mutableStateListOf(ScheduleEntry("Литература")))
    )
    val scheduleList = remember { mutableStateListOf<DaySchedule>().apply { addAll(initial) } }

    val takeMap = remember {
        mutableStateMapOf<String, SnapshotStateMap<String, SnapshotStateList<String>>>().apply {
            scheduleList.forEach { put(it.dayName, mutableStateMapOf()) }
        }
    }

    val navController = rememberNavController()

    Scaffold(
        bottomBar = { BottomNavBar(navController) }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "schedule",
            modifier = Modifier.padding(padding)
        ) {
            composable("schedule") {
                ScheduleMainScreen(
                    scheduleList = scheduleList,
                    onDayClick = { idx -> navController.navigate("editor/$idx") }
                )
            }
            composable(
                "editor/{dayIndex}",
                arguments = listOf(navArgument("dayIndex") { type = NavType.IntType })
            ) { backStack ->
                val dayIndex = backStack.arguments?.getInt("dayIndex") ?: 0
                ScheduleEditorScreen(
                    daySchedule = scheduleList[dayIndex],
                    onSave = { updated ->
                        scheduleList[dayIndex] = updated
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable("takeWithYou") {
                TakeWithYouScreen(
                    scheduleList = scheduleList,
                    takeWithYouMap = takeMap
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(navController: androidx.navigation.NavHostController) {
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = current == "schedule" || current?.startsWith("editor/") == true,
            onClick = {
                navController.navigate("schedule") {
                    popUpTo("schedule") { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.Default.Home, contentDescription = "Расписание") },
            label = { Text("Расписание") }
        )
        NavigationBarItem(
            selected = current == "takeWithYou",
            onClick = {
                navController.navigate("takeWithYou") {
                    popUpTo("schedule") { inclusive = false }
                    launchSingleTop = true
                }
            },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Взять с собой") },
            label = { Text("Взять с собой") }
        )
    }
}

// ---------- Экраны ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMainScreen(
    scheduleList: List<DaySchedule>,
    onDayClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Расписание", fontSize = 28.sp, fontFamily = FontFamily.Cursive) }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            scheduleList.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEachIndexed { colIndex, day ->
                        val absoluteIndex = rowIndex * 2 + colIndex
                        DayCard(
                            daySchedule = day,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            onDayClick = { onDayClick(absoluteIndex) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun DayCard(
    daySchedule: DaySchedule,
    modifier: Modifier = Modifier,
    onDayClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clickable { onDayClick() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                daySchedule.dayName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            daySchedule.entries.forEach {
                Text("- ${it.subject}")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    daySchedule: DaySchedule,
    onSave: (DaySchedule) -> Unit,
    onCancel: () -> Unit
) {
    var count by remember { mutableStateOf(daySchedule.entries.size) }
    val entries = remember { mutableStateListOf<ScheduleEntry>().apply { addAll(daySchedule.entries) } }

    LaunchedEffect(count) {
        val diff = count - entries.size
        when {
            diff > 0 -> repeat(diff) { entries.add(ScheduleEntry("")) }
            diff < 0 -> repeat(-diff) { if (entries.isNotEmpty()) entries.removeLast() }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Редактор: ${daySchedule.dayName}") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = count.toString(),
                onValueChange = { input -> input.toIntOrNull()?.let { c -> if (c >= 0) count = c } },
                label = { Text("Количество уроков") }
            )
            Spacer(Modifier.height(16.dp))
            entries.forEachIndexed { idx, entry ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = entry.subject,
                        onValueChange = { new -> entries[idx] = ScheduleEntry(new.trim()) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Предмет") }
                    )
                    IconButton(
                        onClick = {
                            entries.removeAt(idx)
                            count = entries.size
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить запись")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = {
                    entries.add(ScheduleEntry(""))
                    count = entries.size
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить запись")
                Spacer(Modifier.width(4.dp))
                Text("Добавить запись")
            }
            Spacer(Modifier.height(24.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = onCancel) { Text("Отмена") }
                Button(onClick = { onSave(daySchedule.copy(entries = entries)) }) { Text("Сохранить") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeWithYouScreen(
    scheduleList: List<DaySchedule>,
    takeWithYouMap: MutableMap<String, SnapshotStateMap<String, SnapshotStateList<String>>>
) {
    var selectedDay by remember { mutableStateOf(scheduleList.first().dayName) }
    val daySchedule = scheduleList.find { it.dayName == selectedDay }
    val dayMap = takeWithYouMap.getOrPut(selectedDay) { mutableStateMapOf() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Взять с собой") }) }
    ) { padding ->
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            item {
                DayDropdown(
                    daysOfWeek = scheduleList.map { it.dayName },
                    selectedDay = selectedDay,
                    onDaySelected = { selectedDay = it }
                )
                Spacer(Modifier.height(16.dp))
            }
            if (daySchedule == null) {
                item { Text("Нет данных для $selectedDay") }
            } else {
                items(daySchedule.entries) { entry ->
                    SubjectTakeRow(
                        subject = entry.subject,
                        itemList = dayMap.getOrPut(entry.subject) { mutableStateListOf() }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SubjectTakeRow(
    subject: String,
    itemList: SnapshotStateList<String>
) {
    var newItem by remember { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            Text(subject, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            itemList.forEachIndexed { idx, item ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("- $item", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { itemList.removeAt(idx) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = newItem,
                onValueChange = { newItem = it },
                label = { Text("Что взять?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(4.dp))
            Button(onClick = {
                if (newItem.isNotBlank()) {
                    itemList.add(newItem.trim())
                    newItem = ""
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
                Spacer(Modifier.width(4.dp))
                Text("Добавить")
            }
        }
    }
}

@Composable
fun DayDropdown(
    daysOfWeek: List<String>,
    selectedDay: String,
    onDaySelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { expanded = true }) { Text(selectedDay) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        daysOfWeek.forEach { day ->
            DropdownMenuItem(text = { Text(day) }, onClick = { onDaySelected(day); expanded = false })
        }
    }
}

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(), typography = Typography(), content = content)
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    MyApplicationTheme { AppNavGraph() }
}