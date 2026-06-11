package com.sfedu.schedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sfedu.schedule.model.DaySchedule
import com.sfedu.schedule.model.ScheduleAppData
import com.sfedu.schedule.model.ScheduleEntry
import com.sfedu.schedule.ui.theme.Lab5BogovskiyTheme
import com.sfedu.schedule.viewmodel.ScheduleViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Lab5BogovskiyTheme {
                AppNavGraph()
            }
        }
    }
}

@Composable
fun AppNavGraph(
    scheduleViewModel: ScheduleViewModel = viewModel()
) {
    val uiState by scheduleViewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isLoading) {
        LoadingScreen()
        return
    }

    val appData = uiState.appData
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
                    scheduleList = appData.scheduleList,
                    onDayClick = { dayIndex ->
                        navController.navigate("editor/$dayIndex")
                    }
                )
            }

            composable(
                route = "editor/{dayIndex}",
                arguments = listOf(
                    navArgument("dayIndex") {
                        type = NavType.IntType
                    }
                )
            ) { backStack ->
                val dayIndex = backStack.arguments?.getInt("dayIndex") ?: 0
                val daySchedule = appData.scheduleList.getOrNull(dayIndex)

                if (daySchedule == null) {
                    Text("День не найден")
                } else {
                    ScheduleEditorScreen(
                        daySchedule = daySchedule,
                        onSave = { updatedEntries ->
                            scheduleViewModel.updateDaySchedule(
                                dayIndex = dayIndex,
                                updatedEntries = updatedEntries
                            )
                            navController.popBackStack()
                        },
                        onCancel = {
                            navController.popBackStack()
                        }
                    )
                }
            }

            composable("takeWithYou") {
                TakeWithYouScreen(
                    scheduleList = appData.scheduleList,
                    takeWithYouMap = appData.takeWithYouMap,
                    onAddItem = scheduleViewModel::addTakeWithYouItem,
                    onRemoveItem = scheduleViewModel::removeTakeWithYouItem
                )
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
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
                    popUpTo("schedule") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Расписание"
                )
            },
            label = {
                Text("Расписание")
            }
        )

        NavigationBarItem(
            selected = current == "takeWithYou",
            onClick = {
                navController.navigate("takeWithYou") {
                    popUpTo("schedule") {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.List,
                    contentDescription = "Сборы"
                )
            },
            label = {
                Text("Сборы")
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMainScreen(
    scheduleList: List<DaySchedule>,
    onDayClick: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Расписание",
                        fontSize = 28.sp,
                        fontFamily = FontFamily.Cursive
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            scheduleList.chunked(2).forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEachIndexed { columnIndex, day ->
                        val absoluteIndex = rowIndex * 2 + columnIndex

                        DayCard(
                            daySchedule = day,
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            onDayClick = {
                                onDayClick(absoluteIndex)
                            }
                        )
                    }

                    if (row.size == 1) {
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
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
            .heightIn(min = 120.dp)
            .clickable {
                onDayClick()
            },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = daySchedule.dayName,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (daySchedule.entries.isEmpty()) {
                Text(
                    text = "Уроков нет",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                daySchedule.entries.take(4).forEach { entry ->
                    Text(
                        text = "• ${entry.subject}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (daySchedule.entries.size > 4) {
                    Text(
                        text = "ещё ${daySchedule.entries.size - 4}...",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    daySchedule: DaySchedule,
    onSave: (List<ScheduleEntry>) -> Unit,
    onCancel: () -> Unit
) {
    var count by remember(daySchedule) {
        mutableIntStateOf(daySchedule.entries.size)
    }

    val entries = remember(daySchedule) {
        mutableStateListOf<ScheduleEntry>().apply {
            addAll(daySchedule.entries)
        }
    }

    LaunchedEffect(count) {
        val diff = count - entries.size

        when {
            diff > 0 -> {
                repeat(diff) {
                    entries.add(ScheduleEntry(""))
                }
            }

            diff < 0 -> {
                repeat(-diff) {
                    if (entries.isNotEmpty()) {
                        entries.removeAt(entries.lastIndex)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Редактор: ${daySchedule.dayName}")
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = count.toString(),
                onValueChange = { input ->
                    input.toIntOrNull()?.let { newCount ->
                        if (newCount >= 0) {
                            count = newCount
                        }
                    }
                },
                label = {
                    Text("Количество уроков")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = entry.subject,
                        onValueChange = { newSubject ->
                            entries[index] = ScheduleEntry(newSubject)
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text("Предмет")
                        }
                    )

                    IconButton(
                        onClick = {
                            entries.removeAt(index)
                            count = entries.size
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить запись"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    entries.add(ScheduleEntry(""))
                    count = entries.size
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить запись"
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text("Добавить запись")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onCancel
                ) {
                    Text("Отмена")
                }

                Button(
                    onClick = {
                        val cleanedEntries = entries
                            .map { entry ->
                                entry.copy(subject = entry.subject.trim())
                            }
                            .filter { entry ->
                                entry.subject.isNotBlank()
                            }

                        onSave(cleanedEntries)
                    }
                ) {
                    Text("Сохранить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeWithYouScreen(
    scheduleList: List<DaySchedule>,
    takeWithYouMap: Map<String, Map<String, List<String>>>,
    onAddItem: (dayName: String, subject: String, item: String) -> Unit,
    onRemoveItem: (dayName: String, subject: String, itemIndex: Int) -> Unit
) {
    var selectedDay by remember(scheduleList) {
        mutableStateOf(scheduleList.firstOrNull()?.dayName.orEmpty())
    }

    val daySchedule = scheduleList.find { day ->
        day.dayName == selectedDay
    }

    val dayMap = takeWithYouMap[selectedDay].orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Сборы")
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            item {
                DayDropdown(
                    daysOfWeek = scheduleList.map { it.dayName },
                    selectedDay = selectedDay,
                    onDaySelected = { day ->
                        selectedDay = day
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            if (daySchedule == null) {
                item {
                    Text("Нет данных для выбранного дня")
                }
            } else if (daySchedule.entries.isEmpty()) {
                item {
                    Text("В этот день уроков нет")
                }
            } else {
                items(daySchedule.entries) { entry ->
                    SubjectTakeRow(
                        subject = entry.subject,
                        itemList = dayMap[entry.subject].orEmpty(),
                        onAddItem = { item ->
                            onAddItem(selectedDay, entry.subject, item)
                        },
                        onRemoveItem = { itemIndex ->
                            onRemoveItem(selectedDay, entry.subject, itemIndex)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SubjectTakeRow(
    subject: String,
    itemList: List<String>,
    onAddItem: (String) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    var newItem by remember {
        mutableStateOf("")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = subject,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (itemList.isEmpty()) {
                Text(
                    text = "Пока ничего не добавлено",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                itemList.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "• $item",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        IconButton(
                            onClick = {
                                onRemoveItem(index)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Удалить"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = newItem,
                onValueChange = { value ->
                    newItem = value
                },
                label = {
                    Text("Что взять?")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (newItem.isNotBlank()) {
                        onAddItem(newItem)
                        newItem = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Добавить"
                )

                Spacer(modifier = Modifier.width(4.dp))

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
    var expanded by remember {
        mutableStateOf(false)
    }

    OutlinedButton(
        onClick = {
            expanded = true
        }
    ) {
        Text(
            text = selectedDay.ifBlank {
                "Выберите день"
            }
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            expanded = false
        }
    ) {
        daysOfWeek.forEach { day ->
            DropdownMenuItem(
                text = {
                    Text(day)
                },
                onClick = {
                    onDaySelected(day)
                    expanded = false
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewApp() {
    Lab5BogovskiyTheme {
        ScheduleMainScreen(
            scheduleList = ScheduleAppData.defaultSchedule(),
            onDayClick = {}
        )
    }
}