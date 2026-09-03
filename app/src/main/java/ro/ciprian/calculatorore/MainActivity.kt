package ro.ciprian.calculatorore

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.ciprian.calculatorore.data.AppDatabase
import ro.ciprian.calculatorore.data.AppSetting
import ro.ciprian.calculatorore.data.AppSettingDao
import ro.ciprian.calculatorore.data.WorkEntry
import ro.ciprian.calculatorore.data.WorkEntryDao
import ro.ciprian.calculatorore.data.WorkType
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.get(this)

        setContent {
            MaterialTheme {
                val vm: WorkViewModel = viewModel(
                    factory = WorkViewModel.factory(
                        database.workEntryDao(),
                        database.appSettingDao()
                    )
                )

                WorkApp(vm)
            }
        }
    }
}

class WorkViewModel(
    private val dao: WorkEntryDao,
    private val settingsDao: AppSettingDao
) : ViewModel() {

    val entries: StateFlow<List<WorkEntry>> =
        dao.observeAll().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val settings: StateFlow<AppSetting?> =
        settingsDao.observe().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

    init {
        viewModelScope.launch {
            if (settingsDao.get() == null) {
                settingsDao.save(AppSetting())
            }
        }
    }

    fun saveEntry(
        entry: WorkEntry,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (!validDate(entry.date)) {
            onError("Data invalidă. Folosește formatul AAAA-LL-ZZ.")
            return
        }

        if (
            entry.type == WorkType.ZI.name ||
            entry.type == WorkType.NOAPTE.name
        ) {
            if (entry.totalMinutes <= 0) {
                onError("Durata serviciului trebuie să fie mai mare decât 0.")
                return
            }
        }

        viewModelScope.launch {
            dao.insert(entry)
            onSuccess()
        }
    }

    fun updateEntry(
        entry: WorkEntry,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        if (!validDate(entry.date)) {
            onError("Data invalidă. Folosește formatul AAAA-LL-ZZ.")
            return
        }

        viewModelScope.launch {
            dao.update(entry)
            onSuccess()
        }
    }

    fun deleteEntry(entry: WorkEntry) {
        viewModelScope.launch {
            dao.delete(entry)
        }
    }

    fun saveSettings(
        monthlyNormHours: Int
    ) {
        val safeHours = monthlyNormHours.coerceAtLeast(0)

        viewModelScope.launch {
            settingsDao.save(
                AppSetting(
                    id = 1,
                    monthlyNormMinutes = safeHours * 60,
                    dailyStandardMinutes = 8 * 60
                )
            )
        }
    }

    fun monthEntries(
        year: Int,
        month: Int
    ): Flow<List<WorkEntry>> {

        val ym = YearMonth.of(year, month)

        return dao.observeMonth(
            ym.atDay(1).toString(),
            ym.atEndOfMonth().toString()
        )
    }

    companion object {

        fun validDate(value: String): Boolean {
            return runCatching {
                LocalDate.parse(value)
            }.isSuccess
        }

        fun factory(
            dao: WorkEntryDao,
            settingsDao: AppSettingDao
        ): ViewModelProvider.Factory {

            return object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return WorkViewModel(
                        dao,
                        settingsDao
                    ) as T
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkApp(vm: WorkViewModel) {

    var selectedTab by remember {
        mutableIntStateOf(0)
    }

    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text("Calculator Ore Muncă")
                }
            )
        },

        bottomBar = {

            NavigationBar {

                val labels = listOf(
                    "Adaugă",
                    "Istoric",
                    "Lunar"
                )

                labels.forEachIndexed { index, label ->

                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                        },
                        icon = {},
                        label = {
                            Text(label)
                        }
                    )
                }
            }
        }

    ) { paddingValues ->

        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {

            when (selectedTab) {

                0 -> AddScreen(vm)

                1 -> HistoryScreen(vm)

                2 -> MonthlyScreen(vm)
            }
        }
    }
}

@Composable
fun AddScreen(vm: WorkViewModel) {

    var date by remember {
        mutableStateOf(
            LocalDate.now().toString()
        )
    }

    var start by remember {
        mutable
