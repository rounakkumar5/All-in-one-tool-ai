package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.LoadAdError
import android.app.Activity
import com.example.data.Note
import com.example.data.Habit
import com.example.data.Expense
import com.example.data.HistoryItem
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize the Google Mobile Ads SDK
        MobileAds.initialize(this) {}
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel()
                val context = LocalContext.current
                
                // Initialize Database on first lifecycle call
                LaunchedEffect(Unit) {
                    viewModel.initializeDb(context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppLayout(viewModel)
                }
            }
        }
    }
}

// Data class to represent our catalog of tools
data class UtilityTool(
    val id: String,
    val title: String,
    val description: String,
    val category: String, // "AI Helpers", "Calculators", "Productivity", "Convenience"
    val icon: ImageVector,
    val isAiPowered: Boolean = false,
    val colorAccent: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: MainViewModel) {
    val currentScreenState by viewModel.currentScreen.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiError by viewModel.aiError.collectAsState()
    val apiResponseText by viewModel.aiResponse.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (currentScreenState is Screen.Dash) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✦",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column {
                                Text(
                                    text = "OmniTool AI",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "15 Tools Connected",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(
                            text = when (val s = currentScreenState) {
                                is Screen.Dash -> "OmniTool AI"
                                is Screen.Calculator -> "AI Smart Calculator"
                                is Screen.Notes -> "Notebook Organizer"
                                is Screen.Habits -> "Streak Habit Tracker"
                                is Screen.Budget -> "Budget Cashbook"
                                is Screen.ToolDetail -> s.title
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreenState !is Screen.Dash) {
                        IconButton(
                            modifier = Modifier.testTag("app_bar_back_button"),
                            onClick = { viewModel.currentScreen.value = Screen.Dash }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Return to main dashboard",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    if (currentScreenState is Screen.Dash) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "JD",
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        if (aiLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            IconButton(onClick = { viewModel.currentScreen.value = Screen.Dash }) {
                                Icon(Icons.Default.Home, "Go Home", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentScreenState is Screen.Dash,
                    onClick = { viewModel.currentScreen.value = Screen.Dash },
                    icon = { Icon(Icons.Default.Widgets, contentDescription = "Dash") },
                    label = { Text("Hub") },
                    modifier = Modifier.testTag("nav_hub_item")
                )
                NavigationBarItem(
                    selected = currentScreenState is Screen.Calculator,
                    onClick = { viewModel.currentScreen.value = Screen.Calculator },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculator") },
                    label = { Text("AI Calculator") },
                    modifier = Modifier.testTag("nav_calculator_item")
                )
                NavigationBarItem(
                    selected = currentScreenState is Screen.Notes,
                    onClick = { viewModel.currentScreen.value = Screen.Notes },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Notebook") },
                    label = { Text("Notebook") },
                    modifier = Modifier.testTag("nav_notes_item")
                )
                NavigationBarItem(
                    selected = currentScreenState is Screen.Habits,
                    onClick = { viewModel.currentScreen.value = Screen.Habits },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Habits") },
                    label = { Text("Habits") },
                    modifier = Modifier.testTag("nav_habits_item")
                )
                NavigationBarItem(
                    selected = currentScreenState is Screen.Budget,
                    onClick = { viewModel.currentScreen.value = Screen.Budget },
                    icon = { Icon(Icons.Default.Payments, contentDescription = "Budget") },
                    label = { Text("Budget") },
                    modifier = Modifier.testTag("nav_budget_item")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                        )
                    )
            ) {
                AnimatedContent(
                    targetState = currentScreenState,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { targetScreen ->
                    when (targetScreen) {
                        is Screen.Dash -> DashboardScreen(viewModel)
                        is Screen.Calculator -> SmartCalculatorScreen(viewModel)
                        is Screen.Notes -> NotebookScreen(viewModel)
                        is Screen.Habits -> HabitsTrackerScreen(viewModel)
                        is Screen.Budget -> BudgetScreen(viewModel)
                        is Screen.ToolDetail -> ToolDetailDispatcher(targetScreen.toolId, viewModel)
                    }
                }
            }
            
            // Persistent AdMob test banner at the bottom of the screens!
            AdmobBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface),
                viewModel = viewModel
            )
        }
    }
}

// LIST OF ALL 15 UTILITY TOOLS FOR REGISTRY
fun createCatalog(colors: ColorScheme): List<UtilityTool> {
    return listOf(
        // Category 1: AI POWERED ENGINES
        UtilityTool("ai_chat", "Gemini Chat Companion", "Ask anything to your real-time AI assistant", "AI Helpers", Icons.Default.Chat, true, colors.primary),
        UtilityTool("ai_translator", "AI Translator Pro", "Translate text instantly to top world languages", "AI Helpers", Icons.Default.Translate, true, colors.primary),
        UtilityTool("ai_study", "AI Flashcard Generator", "Generate conceptual study Q&As and flashcards", "AI Helpers", Icons.Default.School, true, colors.primary),
        UtilityTool("ai_goals", "AI Habit Goal Planner", "Make atomic actionable checklists for any purpose", "AI Helpers", Icons.Default.Lightbulb, true, colors.primary),
        
        // Category 2: HEALTH & COGNITIVE
        UtilityTool("bmi_calc", "BMI Health Adviser", "Calculate Body Mass Index and get clinical guidance", "Calculators", Icons.Default.Favorite, false, Color(0xFF34D399)),
        UtilityTool("split_calc", "Tip & Split Splitter", "Divide restaurant logs and tip formulas instantly", "Calculators", Icons.Default.Restaurant, false, Color(0xFF34D399)),
        UtilityTool("currency_calc", "Live Currency Exchange", "Convert top currencies using modern reference tables", "Calculators", Icons.Default.AttachMoney, false, Color(0xFF34D399)),
        UtilityTool("unit_calc", "Comprehensive Converter", "Transform dimensions, lengths, kilos and temperatures", "Calculators", Icons.Default.CompareArrows, false, Color(0xFF34D399)),
        UtilityTool("age_calc", "Chronological Age Solver", "Verify exact birth milestones down to seconds", "Calculators", Icons.Default.Cake, false, Color(0xFF34D399)),

        // Category 3: DAILY UTILITIES
        UtilityTool("decision_maker", "Random Decision Spinner", "Spin details or roll labels to bypass choosing bias", "Convenience", Icons.Default.Casino, false, Color(0xFFFBBF24)),
        UtilityTool("stopwatch", "Athletic Stopwatch & Laps", "A perfect interval precision timer with lap offsets", "Convenience", Icons.Default.Timer, false, Color(0xFFFBBF24)),
        UtilityTool("world_clocks", "Global Time Zone Registry", "Map exact hours in Tokyo, London, Singapore and York", "Convenience", Icons.Default.Public, false, Color(0xFFFBBF24)),
        UtilityTool("password_gen", "Cryptographic Pass Key Maker", "Establish complex uncrackable secure passwords", "Convenience", Icons.Default.VpnKey, false, Color(0xFFFBBF24)),
        UtilityTool("history_log", "Calculation Activity Logs", "Review previous calculations and diagnostic updates", "Convenience", Icons.Default.History, false, Color(0xFFFBBF24)),
        UtilityTool("admob_monetization", "AdMob Ad Space", "Configure AdMob banner, load interstitial and rewarded ads", "Convenience", Icons.Default.Campaign, false, Color(0xFFFBBF24))
    )
}

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val colors = MaterialTheme.colorScheme
    val toolsRegistry = remember(colors) { createCatalog(colors) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val filteredTools = remember(searchQuery, toolsRegistry) {
        if (searchQuery.isEmpty()) {
            toolsRegistry
        } else {
            toolsRegistry.filter {
                it.title.lowercase().contains(searchQuery.lowercase()) ||
                it.description.lowercase().contains(searchQuery.lowercase()) ||
                it.category.lowercase().contains(searchQuery.lowercase())
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Dashboard Greeting Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Welcome to AI ToolKit",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Your daily productive powerhouse",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Magical AI decoration",
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Stats strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val notesCount by viewModel.notesState.collectAsState()
                        val habitsCount by viewModel.habitsState.collectAsState()
                        
                        DashboardStatItem(
                            count = "${notesCount.size}",
                            label = "Notes Saved",
                            icon = Icons.Default.Book
                        )
                        DashboardStatItem(
                            count = "${habitsCount.filter { it.isCompletedToday }.size}/${habitsCount.size}",
                            label = "Habits Done",
                            icon = Icons.Default.CheckCircle
                        )
                        DashboardStatItem(
                            count = "15+",
                            label = "Smart Tools",
                            icon = Icons.Default.Speed
                        )
                    }
                }
            }
        }

        // Search Bar Row
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input"),
                placeholder = { Text("Search 15+ tool catalogs...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search query")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                singleLine = true
            )
        }

        // AI POWERED SECTIONS
        val aiTools = filteredTools.filter { it.category == "AI Helpers" }
        if (aiTools.isNotEmpty()) {
            item {
                Text(
                    text = "🤖 AI CORE ASSISTANTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp, start = 4.dp),
                    letterSpacing = 1.sp
                )
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 280.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(aiTools) { tool ->
                        DashboardToolCard(tool = tool) {
                            viewModel.currentScreen.value = Screen.ToolDetail(tool.id, tool.title)
                        }
                    }
                }
            }
        }

        // NUMERICAL CALCULATORS
        val calcTools = filteredTools.filter { it.category == "Calculators" }
        if (calcTools.isNotEmpty()) {
            item {
                Text(
                    text = "🧮 SMART CALCULATORS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
                    letterSpacing = 1.sp
                )
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(calcTools) { tool ->
                        DashboardToolCard(tool = tool) {
                            viewModel.currentScreen.value = Screen.ToolDetail(tool.id, tool.title)
                        }
                    }
                }
            }
        }

        // AMBIENT DAILY CONVENIENCE
        val utilityTools = filteredTools.filter { it.category == "Convenience" }
        if (utilityTools.isNotEmpty()) {
            item {
                Text(
                    text = "⚙️ UTILITY SUITE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
                    letterSpacing = 1.sp
                )
            }
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.heightIn(max = 560.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = false
                ) {
                    items(utilityTools) { tool ->
                        DashboardToolCard(tool = tool) {
                            viewModel.currentScreen.value = Screen.ToolDetail(tool.id, tool.title)
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DashboardStatItem(count: String, label: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        Icon(icon, contentDescription = label, size = 16.dp, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(count, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun DashboardToolCard(tool: UtilityTool, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable(onClick = onClick)
            .testTag("tool_card_${tool.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(tool.colorAccent.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = tool.colorAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (tool.isAiPowered) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AI", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Column {
                Text(
                    text = tool.title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = tool.description,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 11.sp
                )
            }
        }
    }
}

// Icon helper workaround to support size modifiers properly
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = Modifier.size(size)
    )
}

// VIEW DISPATCHER FOR ALL 15 SPECIALTY TOOLS
@Composable
fun ToolDetailDispatcher(toolId: String, viewModel: MainViewModel) {
    when (toolId) {
        "ai_chat" -> AiChatScreen(viewModel)
        "ai_translator" -> AiTranslatorScreen(viewModel)
        "ai_study" -> AiStudyBuddyScreen(viewModel)
        "ai_goals" -> AiGoalPlannerScreen(viewModel)
        "bmi_calc" -> BmiCalculatorScreen(viewModel)
        "split_calc" -> TipSplitScreen(viewModel)
        "currency_calc" -> CurrencyConverterScreen(viewModel)
        "unit_calc" -> UnitConverterScreen(viewModel)
        "age_calc" -> AgeCalculatorScreen(viewModel)
        "decision_maker" -> DecisionWheelScreen(viewModel)
        "stopwatch" -> StopwatchScreen(viewModel)
        "world_clocks" -> WorldClocksScreen(viewModel)
        "password_gen" -> PasswordGeneratorScreen(viewModel)
        "history_log" -> HistoryLogsScreen(viewModel)
        "admob_monetization" -> AdmobMonetizationScreen(viewModel)
        else -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This custom tool is currently loading or offline.")
            }
        }
    }
}

// --------------------- CORE UTILITY SCREEN: CALCULATOR ---------------------
@Composable
fun SmartCalculatorScreen(viewModel: MainViewModel) {
    val calcInput by viewModel.calcInput.collectAsState()
    val calcResult by viewModel.calcResult.collectAsState()
    val aiResponseText by viewModel.aiResponse.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var showExplanation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Digital Screen Display Card
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().testTag("calc_screen_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = calcInput.ifEmpty { "0" },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    textAlign = TextAlign.End,
                    modifier = Modifier.testTag("calc_input_readout")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (calcResult.isNotEmpty() && calcResult != "Error") {
                        Button(
                            onClick = {
                                showExplanation = true
                                viewModel.askAiToSolveExpression()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("ask_ai_calc_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.Default.AutoAwesome, "Ask AI", size = 12.dp, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Explain with AI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    Text(
                        text = calcResult.let { if (it.isEmpty() || it == "Error") it else "= $it" },
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.testTag("calc_result_readout")
                    )
                }
            }
        }

        // Button Matrix Row
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val rows = listOf(
                listOf("sin", "cos", "tan", "ln"),
                listOf("(", ")", "^", "sqrt"),
                listOf("C", "÷", "×", "⌫"),
                listOf("7", "8", "9", "−"),
                listOf("4", "5", "6", "+"),
                listOf("1", "2", "3", "="),
                listOf("0", ".", "log", "π")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.8f)
                        ) {
                            CalculatorButton(key = key) {
                                when (key) {
                                    "C" -> {
                                        viewModel.calcInput.value = ""
                                        viewModel.calcResult.value = ""
                                        showExplanation = false
                                    }
                                    "⌫" -> {
                                        val cur = viewModel.calcInput.value
                                        if (cur.isNotEmpty()) {
                                            viewModel.calcInput.value = cur.dropLast(1)
                                            viewModel.evaluateCalcExpression()
                                        }
                                    }
                                    "=" -> {
                                        viewModel.evaluateCalcExpression()
                                    }
                                    "sin", "cos", "tan", "log", "ln", "sqrt" -> {
                                        viewModel.calcInput.value += "$key("
                                    }
                                    "π" -> {
                                        viewModel.calcInput.value += "3.14159"
                                        viewModel.evaluateCalcExpression()
                                    }
                                    else -> {
                                        viewModel.calcInput.value += key
                                        viewModel.evaluateCalcExpression()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Expandable explanation log card
        if (showExplanation) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_explanation_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, "AI Solver", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text("AI Walkthrough Tutorial", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showExplanation = false }) {
                            Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    if (aiLoading) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp))
                            Text("Gemini is solving equation step-by-step...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text(
                            text = aiResponseText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("ai_calc_explanation_text")
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButton(key: String, onClick: () -> Unit) {
    val isAction = key in listOf("C", "÷", "×", "−", "+", "=", "⌫")
    val isScientist = key in listOf("sin", "cos", "tan", "ln", "log", "sqrt", "^", "(", ")", "π")
    
    val bg = when {
        key == "=" -> MaterialTheme.colorScheme.primary
        isAction -> MaterialTheme.colorScheme.secondaryContainer
        isScientist -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surface
    }

    val tc = when {
        key == "=" -> MaterialTheme.colorScheme.onPrimary
        isAction -> MaterialTheme.colorScheme.onSecondaryContainer
        isScientist -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .testTag("calc_key_$key"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = bg),
        contentPadding = PaddingValues(0.dp),
        border = if (!isAction && key != "=") BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) else null
    ) {
        Text(
            text = key,
            fontSize = if (isScientist) 11.sp else 16.sp,
            fontWeight = FontWeight.Bold,
            color = tc
        )
    }
}

// --------------------- CORE UTILITY SCREEN: NOTEBOOK ---------------------
@Composable
fun NotebookScreen(viewModel: MainViewModel) {
    val notes by viewModel.notesState.collectAsState()
    val noteTitle by viewModel.noteTitleInput.collectAsState()
    val noteContent by viewModel.noteContentInput.collectAsState()
    val noteCategory by viewModel.noteCategoryInput.collectAsState()
    val noteColorHex by viewModel.noteColorInput.collectAsState()
    val editingNote by viewModel.editingNote.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    var showWriterDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Your Saved Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Button(
                onClick = {
                    viewModel.editingNote.value = null
                    viewModel.noteTitleInput.value = ""
                    viewModel.noteContentInput.value = ""
                    viewModel.noteCategoryInput.value = "General"
                    viewModel.noteColorInput.value = "#FF1E1E2E"
                    showWriterDialog = true
                },
                modifier = Modifier.testTag("add_note_fab_trigger")
            ) {
                Icon(Icons.Default.Add, "Write Notes")
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Note")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notes.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Book, "No Notes", size = 48.dp, tint = MaterialTheme.colorScheme.outline)
                    Text("Your notebook is empty. Create a note above!", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.startEditNote(note)
                                showWriterDialog = true
                            }
                            .testTag("note_entry_${note.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                color = Color(android.graphics.Color.parseColor(note.colorHex)),
                                                shape = CircleShape
                                            )
                                    )
                                    Text(note.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(note.category, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = note.content,
                                fontSize = 12.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(note.timestamp)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                IconButton(
                                    onClick = { viewModel.deleteNote(note.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_note_button_${note.id}")
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Note", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog to create and modify notes with AI Tools
    if (showWriterDialog) {
        AlertDialog(
            onDismissRequest = { showWriterDialog = false },
            title = {
                Text(if (editingNote != null) "Modify Note" else "Create Slate Note", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = noteTitle,
                        onValueChange = { viewModel.noteTitleInput.value = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth().testTag("note_title_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = noteContent,
                        onValueChange = { viewModel.noteContentInput.value = it },
                        label = { Text("Note Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .testTag("note_content_input"),
                        maxLines = 10
                    )

                    // AI Chips row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        InputChip(
                            selected = false,
                            onClick = { viewModel.summarizeCurrentEditingNote() },
                            label = { Text("Summarize with AI", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, "AI", modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("note_ai_summarize_chip")
                        )
                        InputChip(
                            selected = false,
                            onClick = { viewModel.optimizeNoteGrammar() },
                            label = { Text("Enrich Grammar AI", fontSize = 10.sp) },
                            leadingIcon = { Icon(Icons.Default.AutoAwesome, "AI", modifier = Modifier.size(12.dp)) },
                            modifier = Modifier.testTag("note_ai_grammar_chip")
                        )
                    }

                    // Category Tag Row
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("General", "Personal", "Work", "Idea", "Task", "AI Summary").forEach { cat ->
                            FilterChip(
                                selected = noteCategory == cat,
                                onClick = { viewModel.noteCategoryInput.value = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Color tag row
                    Text("Label Tag Color", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("#FF1E1E2E", "#FF34D399", "#FFBB86FC", "#FFFBBF24", "#FFF87171").forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                                    .border(
                                        width = if (noteColorHex == hex) 2.dp else 1.dp,
                                        color = if (noteColorHex == hex) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.noteColorInput.value = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNote()
                        showWriterDialog = false
                    },
                    modifier = Modifier.testTag("save_note_confirm_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWriterDialog = false }) {
                    Text("Discard")
                }
            }
        )
    }
}

// --------------------- CORE UTILITY SCREEN: HABITS ---------------------
@Composable
fun HabitsTrackerScreen(viewModel: MainViewModel) {
    val habits by viewModel.habitsState.collectAsState()
    var newHabitName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Daily Habit Progress Track", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        
        // Input bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newHabitName,
                onValueChange = { newHabitName = it },
                placeholder = { Text("Water intake, exercise, study...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f).testTag("habit_name_input"),
                singleLine = true
            )
            Button(
                onClick = {
                    if (newHabitName.isNotEmpty()) {
                        viewModel.addHabit(newHabitName)
                        newHabitName = ""
                    }
                },
                modifier = Modifier.align(Alignment.CenterVertically).testTag("add_habit_button")
            ) {
                Icon(Icons.Default.Add, "Add Habit")
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (habits.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No custom habits yet. Add some above!", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(habits) { habit ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (habit.isCompletedToday) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Checkbox(
                                    checked = habit.isCompletedToday,
                                    onCheckedChange = { viewModel.toggleHabit(habit) },
                                    modifier = Modifier.testTag("habit_checkbox_${habit.id}")
                                )
                                Column {
                                    Text(
                                        text = habit.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (habit.isCompletedToday) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.LocalFireDepartment, "Streak", size = 11.dp, tint = Color(0xFFFBBF24))
                                        Text("Streak: ${habit.streak} days", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                            IconButton(
                                onClick = { viewModel.deleteHabit(habit.id) },
                                modifier = Modifier.testTag("delete_habit_button_${habit.id}")
                            ) {
                                Icon(Icons.Default.Delete, "Delete Habit", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------- CORE UTILITY SCREEN: BUDGET JOURNAL ---------------------
@Composable
fun BudgetScreen(viewModel: MainViewModel) {
    val expenses by viewModel.expensesState.collectAsState()
    
    val budgetTitle by viewModel.budgetTitle.collectAsState()
    val budgetAmount by viewModel.budgetAmount.collectAsState()
    val budgetType by viewModel.budgetType.collectAsState()
    val budgetCategory by viewModel.budgetCategory.collectAsState()

    val totalIncome = remember(expenses) { expenses.filter { it.type == "Income" }.sumOf { it.amount } }
    val totalExpense = remember(expenses) { expenses.filter { it.type == "Expense" }.sumOf { it.amount } }
    val netBalance = totalIncome - totalExpense

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Daily Budget & Expenses Diary", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        // Summary Metric Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Available Balance", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                Text(
                    text = String.format(Locale.US, "$%.2f", netBalance),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = if (netBalance >= 0.0) Color(0xFF34D399) else Color(0xFFF87171)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Total Income", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "$%.2f", totalIncome), fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                    }
                    Column {
                        Text("Total Spend", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Text(String.format(Locale.US, "$%.2f", totalExpense), fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                    }
                }
            }
        }

        // Quick Adds Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add New Cash Flow Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = budgetTitle,
                        onValueChange = { viewModel.budgetTitle.value = it },
                        placeholder = { Text("Coffee, Taxi, Salary...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f).height(48.dp).testTag("budget_title_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = budgetAmount,
                        onValueChange = { viewModel.budgetAmount.value = it },
                        placeholder = { Text("Amount ($)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("budget_amount_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = budgetType == "Expense",
                            onClick = { viewModel.budgetType.value = "Expense" },
                            label = { Text("Expense", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = budgetType == "Income",
                            onClick = { viewModel.budgetType.value = "Income" },
                            label = { Text("Income", fontSize = 10.sp) }
                        )
                    }

                    Button(
                        onClick = { viewModel.addExpense() },
                        modifier = Modifier.testTag("add_budget_confirm_button")
                    ) {
                        Text("Add", fontSize = 11.sp)
                    }
                }
            }
        }

        // Transaction list container
        Text("Transaction History", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        if (expenses.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No budget listings created yet.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                expenses.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(item.timestamp)),
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = String.format(Locale.US, "%s$%.2f", if (item.type == "Income") "+" else "-", item.amount),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = if (item.type == "Income") Color(0xFF34D399) else Color(0xFFF87171)
                                )
                                IconButton(
                                    onClick = { viewModel.deleteExpense(item.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_budget_button_${item.id}")
                                ) {
                                    Icon(Icons.Default.Delete, "Delete Entry", size = 14.dp, tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 1: AI CHAT COMPANION ---------------------
@Composable
fun AiChatScreen(viewModel: MainViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsState()
    val chatInput by viewModel.chatInputText.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gemini AI Chat Room", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = { viewModel.clearChat() }) {
                Icon(Icons.Default.DeleteSweep, "Clear History", tint = MaterialTheme.colorScheme.outline)
            }
        }

        // Response history container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            if (chatHistory.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, "AI", size = 32.dp, tint = MaterialTheme.colorScheme.primary)
                    Text("Powered by Gemini 3.5-flash", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Type any math, design, code, or personal question!", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(chatHistory) { bubble ->
                        val isUser = bubble.second
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 14.dp
                                        )
                                    )
                                    .background(
                                        if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .padding(12.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = bubble.first,
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dynamic typing area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = chatInput,
                onValueChange = { viewModel.chatInputText.value = it },
                placeholder = { Text("Type query...", fontSize = 12.sp) },
                modifier = Modifier.weight(1f).testTag("chat_input_message"),
                singleLine = true
            )
            IconButton(
                onClick = { viewModel.submitChatToAi() },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(48.dp)
                    .testTag("chat_send_button"),
                enabled = !aiLoading
            ) {
                Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 2: TRANSLATOR ---------------------
@Composable
fun AiTranslatorScreen(viewModel: MainViewModel) {
    val inputTxt by viewModel.translateInputText.collectAsState()
    val tLang by viewModel.translateTargetLang.collectAsState()
    val outTxt by viewModel.translateOutputText.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    val copyClip = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AI Multi-Language Translator", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = inputTxt,
            onValueChange = { viewModel.translateInputText.value = it },
            modifier = Modifier.fillMaxWidth().height(110.dp).testTag("translate_input_text"),
            placeholder = { Text("Enter words, notes, or whole paragraphs to translate...") }
        )

        Text("Target Language", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf("Spanish", "French", "German", "Hindi", "Japanese", "Arabic", "Mandarin").forEach { lang ->
                FilterChip(
                    selected = tLang == lang,
                    onClick = { viewModel.translateTargetLang.value = lang },
                    label = { Text(lang) }
                )
            }
        }

        Button(
            onClick = { viewModel.translateText() },
            modifier = Modifier.fillMaxWidth().testTag("translate_button"),
            enabled = !aiLoading && inputTxt.isNotEmpty()
        ) {
            Icon(Icons.Default.Translate, "Translate")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Translate with AI")
        }

        if (outTxt.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Translation ($tLang)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = {
                            copyClip.setText(AnnotatedString(outTxt))
                            Toast.makeText(ctx, "Translated text copied!", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.ContentCopy, "Copy text", size = 14.dp, tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Text(outTxt, fontSize = 14.sp)
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 3: STUDY TUTOR FLASHCARDS ---------------------
@Composable
fun AiStudyBuddyScreen(viewModel: MainViewModel) {
    val topic by viewModel.studyTopic.collectAsState()
    val flashcards by viewModel.studyResultList.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Study Buddy Flashcard Machine", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = topic,
            onValueChange = { viewModel.studyTopic.value = it },
            placeholder = { Text("Biology, Photosynthesis, Thermodynamics...", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().testTag("study_topic_input"),
            label = { Text("Subject / Subtopic") },
            singleLine = true
        )

        Button(
            onClick = { viewModel.generateFlashcards() },
            modifier = Modifier.fillMaxWidth().testTag("generate_study_button"),
            enabled = !aiLoading && topic.isNotEmpty()
        ) {
            Icon(Icons.Default.AutoAwesome, "Study")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Compile 4 Flashcards")
        }

        if (aiLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            flashcards.forEachIndexed { i, card ->
                var revealed by remember(card) { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { revealed = !revealed },
                    colors = CardDefaults.cardColors(
                        containerColor = if (revealed) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Flashcard #${i + 1}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = if (revealed) "Answer:\n${card.second}" else "Question:\n${card.first}",
                            fontWeight = if (revealed) FontWeight.Normal else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (revealed) "Tap to see question" else "Tap card to reveal answer",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 4: AI GOAL WORKFLOW PLANNER ---------------------
@Composable
fun AiGoalPlannerScreen(viewModel: MainViewModel) {
    val topic by viewModel.goalPlannerTopic.collectAsState()
    val stepsList by viewModel.goalPlannerSteps.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("AI Custom Goal Builder", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = topic,
            onValueChange = { viewModel.goalPlannerTopic.value = it },
            placeholder = { Text("E.g., Learn Spanish in 30 days, Build healthy sleep", fontSize = 12.sp) },
            label = { Text("What is your life goal?") },
            modifier = Modifier.fillMaxWidth().testTag("goal_workflow_input"),
            singleLine = true
        )

        Button(
            onClick = { viewModel.generateGoalPlanner() },
            modifier = Modifier.fillMaxWidth().testTag("generate_workflow_button"),
            enabled = !aiLoading && topic.isNotEmpty()
        ) {
            Icon(Icons.Default.TrendingUp, "Build")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Extract 5 Step Tracker Workflow")
        }

        if (aiLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            stepsList.forEachIndexed { idx, step ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Step #${idx + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(step, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        IconButton(onClick = {
                            viewModel.applyGoalStepAsHabit(step)
                            Toast.makeText(ctx, "Applied to Daily Habits!", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.testTag("apply_goal_step_$idx")) {
                            Icon(Icons.Default.PlaylistAdd, "Apply Habit", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 5: BMI ADVISER ---------------------
@Composable
fun BmiCalculatorScreen(viewModel: MainViewModel) {
    val weight by viewModel.bmiWeight.collectAsState()
    val height by viewModel.bmiHeight.collectAsState()
    val res by viewModel.bmiResult.collectAsState()
    val advice by viewModel.bmiAdvice.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("BMI Body Ratio Adviser", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = weight,
                onValueChange = { viewModel.bmiWeight.value = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("bmi_weight_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = height,
                onValueChange = { viewModel.bmiHeight.value = it },
                label = { Text("Height (cm)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("bmi_height_input"),
                singleLine = true
            )
        }

        Button(
            onClick = { viewModel.calcBmiMetric() },
            modifier = Modifier.fillMaxWidth().testTag("calc_bmi_button")
        ) {
            Icon(Icons.Default.Favorite, "BMI")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Verify BMI Metric")
        }

        res?.let { bmi ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Calculated Body Ratio", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(
                        text = String.format(Locale.US, "BMI: %.1f", bmi),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black
                    )
                    Divider()
                    Text(advice, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 6: TIP & BILL SPLITTER ---------------------
@Composable
fun TipSplitScreen(viewModel: MainViewModel) {
    val bAmt by viewModel.splitBillAmount.collectAsState()
    val tipPct by viewModel.splitTipPercentage.collectAsState()
    val pCount by viewModel.splitPeopleCount.collectAsState()
    val resultMsg by viewModel.splitResultText.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Tip Planner & Bill Splitter", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = bAmt,
            onValueChange = { viewModel.splitBillAmount.value = it },
            label = { Text("Bill Total ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("split_bill_input"),
            singleLine = true
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = tipPct,
                onValueChange = { viewModel.splitTipPercentage.value = it },
                label = { Text("Tip Percentage %") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("split_tip_input"),
                singleLine = true
            )
            OutlinedTextField(
                value = pCount,
                onValueChange = { viewModel.splitPeopleCount.value = it },
                label = { Text("Splitting People") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).testTag("split_people_input"),
                singleLine = true
            )
        }

        Button(
            onClick = { viewModel.calculateSplit() },
            modifier = Modifier.fillMaxWidth().testTag("split_calc_button")
        ) {
            Icon(Icons.Default.Restaurant, "Split")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Calculate Bill Partition")
        }

        if (resultMsg.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
            ) {
                Text(
                    text = resultMsg,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 7: CURRENCY EXCHANGER ---------------------
@Composable
fun CurrencyConverterScreen(viewModel: MainViewModel) {
    val amt by viewModel.currencyAmount.collectAsState()
    val from by viewModel.currencyFrom.collectAsState()
    val to by viewModel.currencyTo.collectAsState()
    val outRes by viewModel.currencyConvertedResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Currency Exchange Converter", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = amt,
            onValueChange = { viewModel.currencyAmount.value = it },
            label = { Text("Exchanging Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("currency_amount_input"),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("From Currency", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("USD", "EUR", "GBP", "JPY", "INR").forEach { cur ->
                        FilterChip(
                            selected = from == cur,
                            onClick = { viewModel.currencyFrom.value = cur },
                            label = { Text(cur, fontSize = 10.sp) }
                        )
                    }
                }
            }

            Icon(Icons.Default.ArrowForward, "To", tint = MaterialTheme.colorScheme.outline)

            Column(modifier = Modifier.weight(1f)) {
                Text("To Currency", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("USD", "EUR", "GBP", "JPY", "INR").forEach { cur ->
                        FilterChip(
                            selected = to == cur,
                            onClick = { viewModel.currencyTo.value = cur },
                            label = { Text(cur, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.convertCurrency() },
            modifier = Modifier.fillMaxWidth().testTag("convert_currency_button")
        ) {
            Icon(Icons.Default.AttachMoney, "Exchange")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Convert Currency")
        }

        if (outRes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = outRes,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 8: COMPREHENSIVE CONVERTER ---------------------
@Composable
fun UnitConverterScreen(viewModel: MainViewModel) {
    val cat by viewModel.unitCategory.collectAsState()
    val fromValue by viewModel.unitFromValue.collectAsState()
    val toValue by viewModel.unitToValue.collectAsState()
    val fromUnit by viewModel.unitFromUnit.collectAsState()
    val toUnit by viewModel.unitToUnit.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Multi-Metric Converter", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        // Select Category Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Length", "Weight", "Temp").forEach { item ->
                FilterChip(
                    selected = cat == item,
                    onClick = {
                        viewModel.unitCategory.value = item
                        viewModel.unitToValue.value = ""
                        // Defaults set
                        viewModel.unitFromUnit.value = when (item) {
                            "Length" -> "m"
                            "Weight" -> "kg"
                            else -> "C"
                        }
                        viewModel.unitToUnit.value = when (item) {
                            "Length" -> "km"
                            "Weight" -> "g"
                            else -> "F"
                        }
                    },
                    label = { Text(item) }
                )
            }
        }

        OutlinedTextField(
            value = fromValue,
            onValueChange = {
                viewModel.unitFromValue.value = it
                viewModel.convertUnits()
            },
            label = { Text("Input value") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth().testTag("unit_input_field"),
            singleLine = true
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.0f)) {
                Text("From Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val units = when (cat) {
                        "Length" -> listOf("m", "km", "cm", "Inch", "Foot")
                        "Weight" -> listOf("kg", "g", "lb", "oz")
                        else -> listOf("C", "F", "K")
                    }
                    units.forEach { u ->
                        FilterChip(
                            selected = fromUnit == u,
                            onClick = {
                                viewModel.unitFromUnit.value = u
                                viewModel.convertUnits()
                            },
                            label = { Text(u, fontSize = 10.sp) }
                        )
                    }
                }
            }

            Icon(Icons.Default.CompareArrows, "Convert to", tint = MaterialTheme.colorScheme.secondary)

            Column(modifier = Modifier.weight(1.0f)) {
                Text("To Unit", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    val units = when (cat) {
                        "Length" -> listOf("m", "km", "cm", "Inch", "Foot")
                        "Weight" -> listOf("kg", "g", "lb", "oz")
                        else -> listOf("C", "F", "K")
                    }
                    units.forEach { u ->
                        FilterChip(
                            selected = toUnit == u,
                            onClick = {
                                viewModel.unitToUnit.value = u
                                viewModel.convertUnits()
                            },
                            label = { Text(u, fontSize = 10.sp) }
                        )
                    }
                }
            }
        }

        if (toValue.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = toValue,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 9: AGE CALCULATOR ---------------------
@Composable
fun AgeCalculatorScreen(viewModel: MainViewModel) {
    val selectDate by viewModel.selectedBirthDate.collectAsState()
    val calcStr by viewModel.ageResultCalculated.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Chronological Age Solver", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Input a dynamic birth date timestamp to verify precise calendar milestones details.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)

        // Simple input helper box to trigger Calendar dialogue
        Button(
            onClick = {
                val calendar = Calendar.getInstance()
                android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val cal = Calendar.getInstance()
                        cal.set(year, month, dayOfMonth)
                        viewModel.selectedBirthDate.value = cal.timeInMillis
                        
                        // Calculate metrics
                        val now = Calendar.getInstance()
                        var ageYears = now.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
                        if (now.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) {
                            ageYears--
                        }
                        val diffDays = (now.timeInMillis - cal.timeInMillis) / (1000 * 60 * 60 * 24)
                        viewModel.ageResultCalculated.value = "Age: $ageYears Years Old\n(Total days lived: $diffDays days)"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            },
            modifier = Modifier.fillMaxWidth().testTag("pick_birth_date_button")
        ) {
            Icon(Icons.Default.CalendarToday, "DatePicker")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Select Date of Birth")
        }

        selectDate?.let { date ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Milestone Calculated", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(calcStr, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Birth Date TimeStamp: $date ms", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 10: DECISION SPIN WHEEL ---------------------
@Composable
fun DecisionWheelScreen(viewModel: MainViewModel) {
    val choicesInput by viewModel.wheelInput.collectAsState()
    val chosenOption by viewModel.chosenDecision.collectAsState()
    val spinning by viewModel.isWheelSpinning.collectAsState()

    val rotationAngle by animateFloatAsState(
        targetValue = if (spinning) 720f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing),
        label = "WheelRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Random Decision Spin Wheel", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        OutlinedTextField(
            value = choicesInput,
            onValueChange = { viewModel.wheelInput.value = it },
            label = { Text("Comma-separated options list") },
            modifier = Modifier.fillMaxWidth().testTag("wheel_options_input"),
            placeholder = { Text("E.g. Pizza, Burger, Pasta, Salad") }
        )

        // Graphical spinning wheel mockup
        Box(
            modifier = Modifier
                .size(160.dp)
                .rotate(rotationAngle)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0xFF34D399),
                            Color(0xFFBB86FC),
                            Color(0xFFFBBF24),
                            Color(0xFFF87171),
                            Color(0xFF34D399)
                        )
                    ),
                    CircleShape
                )
                .border(4.dp, MaterialTheme.colorScheme.onBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Casino, "Decision center", size = 28.dp, tint = Color.Black)
        }

        Button(
            onClick = { viewModel.rollDecision() },
            modifier = Modifier.fillMaxWidth().testTag("spin_wheel_button"),
            enabled = !spinning && choicesInput.isNotEmpty()
        ) {
            Icon(Icons.Default.Casino, "Casino")
            Spacer(modifier = Modifier.width(4.dp))
            Text("Spin Wheel Decision")
        }

        if (chosenOption.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = chosenOption,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 11: ATHLETIC STOPWATCH ---------------------
@Composable
fun StopwatchScreen(viewModel: MainViewModel) {
    var timerRunning by remember { mutableStateOf(false) }
    var timeCountMs by remember { mutableStateOf(0L) }
    val lapTimes = remember { mutableStateListOf<String>() }

    // Coroutine Clock ticking
    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (timerRunning) {
                delay(30)
                timeCountMs += 30
            }
        }
    }

    val displayTimeStr = remember(timeCountMs) {
        val totalSecs = timeCountMs / 1000
        val hr = totalSecs / 3600
        val min = (totalSecs % 3600) / 60
        val sec = totalSecs % 60
        val ms = (timeCountMs % 1000) / 10
        String.format(Locale.US, "%02d:%02d:%02d.%02d", hr, min, sec, ms)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Workout stopwatch Timer", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Card(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = displayTimeStr,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("stopwatch_time_readout")
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { timerRunning = !timerRunning },
                modifier = Modifier.weight(1f).testTag("stopwatch_toggle_button")
            ) {
                Text(if (timerRunning) "Pause" else "Start")
            }
            Button(
                onClick = {
                    if (timerRunning) {
                        lapTimes.add(0, displayTimeStr)
                    }
                },
                modifier = Modifier.weight(1f).testTag("stopwatch_lap_button"),
                enabled = timerRunning
            ) {
                Text("Lap Split")
            }
            Button(
                onClick = {
                    timerRunning = false
                    timeCountMs = 0L
                    lapTimes.clear()
                },
                modifier = Modifier.weight(1f).testTag("stopwatch_reset_button")
            ) {
                Text("Reset")
            }
        }

        Text("Registered Laps Chronology", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        LazyColumn(
            modifier = Modifier.weight(1.0f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(lapTimes) { lap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "Lap Record: $lap",
                        modifier = Modifier.padding(12.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 12: GLOBAL WORLD CLOCKS ---------------------
@Composable
fun WorldClocksScreen(viewModel: MainViewModel) {
    val worldTimes by viewModel.wordClockTimes.collectAsState()

    // Clock update triggers
    LaunchedEffect(Unit) {
        while (true) {
            viewModel.updateWorldTimes()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Global Time Zone Clock list", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val list = worldTimes.toList()
            items(list) { (label, value) ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Standard UTC Offset Mapping", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Text(
                            text = value,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

// --------------------- REGISTRY SCREEN 13: PASSWORD CREATOR ---------------------
@Composable
fun PasswordGeneratorScreen(viewModel: MainViewModel) {
    val pwdLength by viewModel.pwdLength.collectAsState()
    val incUpper by viewModel.pwdIncludeUpper.collectAsState()
    val incNumbers by viewModel.pwdIncludeNumbers.collectAsState()
    val incSymbols by viewModel.pwdIncludeSymbols.collectAsState()
    val generatedToken by viewModel.pwdGenerated.collectAsState()

    val clipboard = LocalClipboardManager.current
    val ctx = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Cryptographic Password Generator", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        Card(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
                if (generatedToken.isEmpty()) {
                    Text("Click 'Compile Secure Pass' bottom", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = generatedToken,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("gen_password_text")
                        )
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(generatedToken))
                            Toast.makeText(ctx, "Password stored to Clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, "Copy passphrase")
                        }
                    }
                }
            }
        }

        Text("Password Length: $pwdLength characters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Slider(
            value = pwdLength.toFloat(),
            onValueChange = { viewModel.pwdLength.value = it.toInt() },
            valueRange = 8f..32f,
            steps = 24,
            modifier = Modifier.testTag("password_len_slider")
        )

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = incUpper, onCheckedChange = { viewModel.pwdIncludeUpper.value = it })
            Text("Include Uppercase Characters (A-Z)", fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = incNumbers, onCheckedChange = { viewModel.pwdIncludeNumbers.value = it })
            Text("Include Numerical Digits (0-9)", fontSize = 12.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Checkbox(checked = incSymbols, onCheckedChange = { viewModel.pwdIncludeSymbols.value = it })
            Text("Include Complex Symbols (!@#$)", fontSize = 12.sp)
        }

        Button(
            onClick = { viewModel.generatePasswordSecret() },
            modifier = Modifier.fillMaxWidth().testTag("compile_password_button")
        ) {
            Icon(Icons.Default.VpnKey, "Key")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Compile Secure Pass")
        }
    }
}

// --------------------- REGISTRY SCREEN 14: SYSTEM HISTORIC LOGS ---------------------
@Composable
fun HistoryLogsScreen(viewModel: MainViewModel) {
    val history by viewModel.historyState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Calculation Activity Logs", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            IconButton(onClick = { viewModel.clearAllHistory() }) {
                Icon(Icons.Default.Delete, "Delete calculations history", tint = MaterialTheme.colorScheme.error)
            }
        }

        if (history.isEmpty()) {
            Box(modifier = Modifier.weight(1.0f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No math or utility calculations logged yet.", color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1.0f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(log.toolType, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                                Text(
                                    text = SimpleDateFormat("MMM d, yyyy HH:mm:ss", Locale.US).format(Date(log.timestamp)),
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(log.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(log.result, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

// ==================== GOOGLE ADMOB INTEGRATION SHIELD ====================

@Composable
fun AdmobBanner(modifier: Modifier = Modifier, viewModel: MainViewModel) {
    val useProduction by viewModel.useProductionAds.collectAsState()
    val testId = "ca-app-pub-3940256099942544/6300978111"
    val productionId = "ca-app-pub-2186974340923235/7216523886"
    val activeId = if (useProduction) productionId else testId
    
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = activeId
                adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        viewModel.adStatusText.value = "Banner ad loaded successfully!"
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        viewModel.adStatusText.value = "Banner failed to load: ${error.message} (Error Code: ${error.code})"
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            if (adView.adUnitId != activeId) {
                adView.adUnitId = activeId
                adView.adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdLoaded() {
                        viewModel.adStatusText.value = "Banner ad loaded successfully!"
                    }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        viewModel.adStatusText.value = "Banner failed to load: ${error.message} (Error Code: ${error.code})"
                    }
                }
                adView.loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun AdmobMonetizationScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val useProduction by viewModel.useProductionAds.collectAsState()
    val adStatusText by viewModel.adStatusText.collectAsState()
    
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    
    var isInterstitialLoading by remember { mutableStateOf(false) }
    var isRewardedLoading by remember { mutableStateOf(false) }
    
    var rewardPoints by remember { mutableStateOf(0) }

    // Constants for Banner/Interstitial/Rewarded
    val testBannerId = "ca-app-pub-3940256099942544/6300978111"
    val prodBannerId = "ca-app-pub-2186974340923235/7216523886"
    
    val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"
    val prodInterstitialId = "ca-app-pub-2186974340923235/4852003436"
    
    val testRewardedId = "ca-app-pub-3940256099942544/5224354917"
    val prodRewardedId = "ca-app-pub-3940256099942544/5224354917" // Still using test rewarded video since real rewarded video needs verified devices

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Campaign,
                        contentDescription = "Monetization Hub",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AdMob Monetization Hub",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Configure and test Google Mobile Ads. You can switch between Sandbox/Test Ads and Live Production Ads to verify implementation.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Mode Switcher Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (useProduction) 
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) 
                else 
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                1.dp, 
                if (useProduction) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) 
                else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (useProduction) "PRODUCTION ADS MODE" else "SANDBOX TEST ADS MODE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (useProduction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (useProduction) 
                                "Using your custom production Ad Unit IDs. Real ads will start showing once Google approves your account configuration (usually 24-48 hours)."
                            else 
                                "Using Google sandbox test keys. This will ALWAYS successfully display test banner & interactive ads immediately so you know the integration is working!",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = useProduction,
                        onCheckedChange = { isChecked ->
                            viewModel.useProductionAds.value = isChecked
                            viewModel.adStatusText.value = "Switched to ${if (isChecked) "Production" else "Sandbox Test"} ads"
                            // Clear existing loaded ads so they reload in the new mode
                            interstitialAd = null
                            rewardedAd = null
                        }
                    )
                }
            }
        }

        // Stats Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Your Ad Rewards", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                    Text("$rewardPoints OmniPoints", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f), CircleShape)
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, "Points", tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(24.dp))
                }
            }
        }

        // Live Banner Section
        Text(
            text = "1. LIVE BANNER AD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Banner Ad View",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (useProduction) "ID: ca-app-.../7216523886" else "ID: Test Sandbox Banner",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                // Real AdView component
                AdmobBanner(modifier = Modifier.padding(vertical = 4.dp), viewModel = viewModel)
            }
        }

        // Interstitial Ad Section
        Text(
            text = "2. INTERSTITIAL AD (FULL SCREEN)",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Interstitial ads are full-screen ads that cover the interface of their host app. Usually shown at natural transition points (e.g. when changing screens or finishing calculations).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isInterstitialLoading = true
                            viewModel.adStatusText.value = "Loading Interstitial..."
                            val adRequest = AdRequest.Builder().build()
                            val currentId = if (useProduction) prodInterstitialId else testInterstitialId
                            InterstitialAd.load(
                                context,
                                currentId,
                                adRequest,
                                object : InterstitialAdLoadCallback() {
                                    override fun onAdLoaded(ad: InterstitialAd) {
                                        interstitialAd = ad
                                        isInterstitialLoading = false
                                        viewModel.adStatusText.value = "Interstitial Ad loaded successfully!"
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        interstitialAd = null
                                        isInterstitialLoading = false
                                        viewModel.adStatusText.value = "Failed to load Interstitial: ${error.message} (Code: ${error.code})"
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isInterstitialLoading && interstitialAd == null
                    ) {
                        if (isInterstitialLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("1. Load Interstitial", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            interstitialAd?.let { ad ->
                                ad.show(context as Activity)
                                interstitialAd = null
                                viewModel.adStatusText.value = "Interstitial Ad watched successfully!"
                            } ?: run {
                                viewModel.adStatusText.value = "Please load the ad first"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = interstitialAd != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("2. Show Ad", fontSize = 11.sp)
                    }
                }
            }
        }

        // Rewarded Ad Section
        Text(
            text = "3. REWARDED VIDEO AD",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Rewarded ads reward users with in-app assets or points for interacting with video ads. Watch the full test video to earn +10 OmniPoints!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            isRewardedLoading = true
                            viewModel.adStatusText.value = "Loading Rewarded Video..."
                            val adRequest = AdRequest.Builder().build()
                            val currentId = if (useProduction) prodRewardedId else testRewardedId
                            RewardedAd.load(
                                context,
                                currentId,
                                adRequest,
                                object : RewardedAdLoadCallback() {
                                    override fun onAdLoaded(ad: RewardedAd) {
                                        rewardedAd = ad
                                        isRewardedLoading = false
                                        viewModel.adStatusText.value = "Rewarded Video loaded successfully!"
                                    }

                                    override fun onAdFailedToLoad(error: LoadAdError) {
                                        rewardedAd = null
                                        isRewardedLoading = false
                                        viewModel.adStatusText.value = "Failed to load Rewarded: ${error.message} (Code: ${error.code})"
                                    }
                                }
                            )
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isRewardedLoading && rewardedAd == null
                    ) {
                        if (isRewardedLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("1. Load Video", fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = {
                            rewardedAd?.let { ad ->
                                ad.show(context as Activity) { rewardItem ->
                                    val amount = rewardItem.amount
                                    rewardPoints += amount
                                    viewModel.adStatusText.value = "Rewarded: +$amount points earned!"
                                }
                                rewardedAd = null
                            } ?: run {
                                viewModel.adStatusText.value = "Please load the video first"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = rewardedAd != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("2. Show & Get Reward", fontSize = 11.sp)
                    }
                }
            }
        }

        // Status bar logs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (adStatusText.contains("Failed") || adStatusText.contains("Error") || adStatusText.contains("failed")) Color.Red else Color.Green,
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Status: $adStatusText",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Troubleshooting Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Why production ads might not show immediately (Common AdMob Rules):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "• Account Approval: New AdMob accounts take up to 48 hours to be reviewed and approved by Google. No ads will load until approved.\n" +
                           "• Error Code 3 (No Fill): This means there is no ad inventory available for your new ID yet. This is completely normal and resolves once traffic grows.\n" +
                           "• Debug Build Restrictions: Google AdMob often restricts live production ads inside unregistered debug environments or emulators to prevent click fraud. Use Sandbox Test Ads for testing.",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

