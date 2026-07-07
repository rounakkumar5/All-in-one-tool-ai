package com.example

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.Expense
import com.example.data.Habit
import com.example.data.HistoryItem
import com.example.data.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

sealed class Screen {
    object Dash : Screen()
    object Calculator : Screen()
    object Notes : Screen()
    object Habits : Screen()
    object Budget : Screen()
    data class ToolDetail(val toolId: String, val title: String) : Screen()
}

class MainViewModel : ViewModel() {
    
    // Database Setup
    private lateinit var db: AppDatabase
    private var isInitialized = false

    val notesState = MutableStateFlow<List<Note>>(emptyList())
    val habitsState = MutableStateFlow<List<Habit>>(emptyList())
    val expensesState = MutableStateFlow<List<Expense>>(emptyList())
    val historyState = MutableStateFlow<List<HistoryItem>>(emptyList())

    fun initializeDb(context: Context) {
        if (isInitialized) return
        db = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "ai_multitool_db"
        )
        .fallbackToDestructiveMigration()
        .build()

        isInitialized = true
        observeDb()
    }

    private fun observeDb() {
        viewModelScope.launch {
            db.appDao().getAllNotes().collect { notesState.value = it }
        }
        viewModelScope.launch {
            db.appDao().getAllHabits().collect { habitsState.value = it }
        }
        viewModelScope.launch {
            db.appDao().getAllExpenses().collect { expensesState.value = it }
        }
        viewModelScope.launch {
            db.appDao().getHistory().collect { historyState.value = it }
        }

        // Add pre-configured default habits if empty
        viewModelScope.launch {
            db.appDao().getAllHabits().collect { list ->
                if (list.isEmpty()) {
                    db.appDao().insertHabit(Habit(name = "Drink 8 glasses of water", isCompletedToday = false, streak = 2))
                    db.appDao().insertHabit(Habit(name = "Morning exercise (15m)", isCompletedToday = false, streak = 5))
                    db.appDao().insertHabit(Habit(name = "Read 10 pages of a book", isCompletedToday = false, streak = 0))
                }
            }
        }
    }

    // UI Configuration States
    val currentScreen = MutableStateFlow<Screen>(Screen.Dash)
    val searchQuery = MutableStateFlow("")
    val useProductionAds = MutableStateFlow(false)
    val adStatusText = MutableStateFlow("Ready to load ads")

    // AI & Chat States
    val aiLoading = MutableStateFlow(false)
    val aiError = MutableStateFlow<String?>(null)
    val aiResponse = MutableStateFlow("")
    val chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList()) // Pair of Content to IsUserMessage

    // Calculator Tool State
    val calcInput = MutableStateFlow("")
    val calcResult = MutableStateFlow("")

    // Notes States
    val noteTitleInput = MutableStateFlow("")
    val noteContentInput = MutableStateFlow("")
    val noteCategoryInput = MutableStateFlow("General")
    val noteColorInput = MutableStateFlow("#FF1E1E2E")
    val editingNote = MutableStateFlow<Note?>(null)

    // BMI States
    val bmiWeight = MutableStateFlow("")
    val bmiHeight = MutableStateFlow("")
    val bmiResult = MutableStateFlow<Double?>(null)
    val bmiAdvice = MutableStateFlow("")

    // Currency States
    val currencyAmount = MutableStateFlow("100")
    val currencyFrom = MutableStateFlow("USD")
    val currencyTo = MutableStateFlow("EUR")
    val currencyConvertedResult = MutableStateFlow("")

    // Unit Converter States
    val unitCategory = MutableStateFlow("Length") // Length, Weight, Temp
    val unitFromValue = MutableStateFlow("")
    val unitToValue = MutableStateFlow("")
    val unitFromUnit = MutableStateFlow("m")
    val unitToUnit = MutableStateFlow("km")

    // Age Calculator States
    val selectedBirthDate = MutableStateFlow<Long?>(null)
    val ageResultCalculated = MutableStateFlow("")

    // Split & Tip Calculator States
    val splitBillAmount = MutableStateFlow("")
    val splitTipPercentage = MutableStateFlow("15")
    val splitPeopleCount = MutableStateFlow("2")
    val splitResultText = MutableStateFlow("")

    // Decision Wheel States
    val wheelInput = MutableStateFlow("Pizza, Burger, Salads, Sushi, Pasta")
    val chosenDecision = MutableStateFlow("")
    val isWheelSpinning = MutableStateFlow(false)

    // Password Tool States
    val pwdLength = MutableStateFlow(12)
    val pwdIncludeUpper = MutableStateFlow(true)
    val pwdIncludeNumbers = MutableStateFlow(true)
    val pwdIncludeSymbols = MutableStateFlow(true)
    val pwdGenerated = MutableStateFlow("")

    // World Times List
    val wordClockTimes = MutableStateFlow<Map<String, String>>(emptyMap())

    // Direct API Caller Helper
    fun callGemini(prompt: String, onComplete: (String) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            aiLoading.value = true
            aiError.value = null
            aiResponse.value = ""
            try {
                // Read official key securely injected via BuildConfig (by secrets gradle plugin)
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    throw IllegalStateException("API key is not configured yet. Please input your Gemini API Key in AI Studio Secrets panel.")
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )

                val callResponse = RetrofitClient.service.generateContent(apiKey, request)
                val textOutput = callResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "AI presented an empty response. Please rephrase."
                
                withContext(Dispatchers.Main) {
                    aiResponse.value = textOutput
                    onComplete(textOutput)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    aiError.value = e.message ?: "Unknown communication failure occurs."
                    aiResponse.value = "Unable to fetch AI answer: ${e.message}"
                }
            } finally {
                aiLoading.value = false
            }
        }
    }

    // AI Calculator Resolver
    fun askAiToSolveExpression() {
        val expr = calcInput.value
        if (expr.isEmpty()) return
        val prompt = "Explain step-by-step how to solve this math problem: $expr. Give a clear, friendly, and structured mathematical tutorial."
        callGemini(prompt) { output ->
            viewModelScope.launch {
                db.appDao().insertHistoryItem(
                    HistoryItem(
                        toolType = "AI Calculator",
                        title = "Solved: $expr",
                        result = "Offline: ${calcResult.value}\n\nAI: $output"
                    )
                )
            }
        }
    }

    // AI Translation Helper
    val translateInputText = MutableStateFlow("")
    val translateTargetLang = MutableStateFlow("Spanish")
    val translateOutputText = MutableStateFlow("")

    fun translateText() {
        val txt = translateInputText.value
        val lang = translateTargetLang.value
        if (txt.isEmpty()) return
        aiLoading.value = true
        val prompt = "Translate the following text into $lang. Return ONLY the translated text, no extra messages: \"$txt\""
        callGemini(prompt) { output ->
            translateOutputText.value = output
        }
    }

    // Local Mathematical Evaluation Function
    fun evaluateCalcExpression() {
        val expr = calcInput.value
        if (expr.isEmpty()) {
            calcResult.value = ""
            return
        }
        try {
            val resultValue = parseAndEvaluate(expr)
            // Format Double nicely
            calcResult.value = if (resultValue % 1.0 == 0.0) {
                resultValue.toLong().toString()
            } else {
                String.format(Locale.US, "%.5f", resultValue).trimEnd('0').trimEnd('.')
            }
            
            // Insert into local history
            viewModelScope.launch {
                db.appDao().insertHistoryItem(
                    HistoryItem(
                        toolType = "Calculator",
                        title = expr,
                        result = calcResult.value
                    )
                )
            }
        } catch (e: Exception) {
            calcResult.value = "Error"
        }
    }

    private fun parseAndEvaluate(expression: String): Double {
        val clean = expression.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
        return ExpressionEvaluator(clean).parse()
    }

    private class ExpressionEvaluator(private val clean: String) {
        private var pos = -1
        private var ch = -1

        private fun nextChar() {
            ch = if (++pos < clean.length) clean[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val r = parseExpression()
            if (pos < clean.length) throw RuntimeException("Unexpected leftover input: " + ch.toChar())
            return r
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Division by zero")
                    x /= divisor
                }
                else break
            }
            return x
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                x = clean.substring(startPos, pos).toDouble()
            } else if (ch >= 'a'.code && ch <= 'z'.code) {
                while (ch >= 'a'.code && ch <= 'z'.code) nextChar()
                val func = clean.substring(startPos, pos)
                x = parseFactor()
                x = when (func) {
                    "sqrt" -> sqrt(x)
                    "sin" -> sin(Math.toRadians(x))
                    "cos" -> cos(Math.toRadians(x))
                    "tan" -> tan(Math.toRadians(x))
                    "log" -> log10(x)
                    "ln" -> ln(x)
                    else -> throw RuntimeException("Unknown function: $func")
                }
            } else {
                throw RuntimeException("Unexpected character: " + ch.toChar())
            }

            if (eat('^'.code)) x = x.pow(parseFactor())

            return x
        }
    }

    // AI Notes Processing
    fun summarizeCurrentEditingNote() {
        val title = noteTitleInput.value
        val content = noteContentInput.value
        if (content.isEmpty()) return
        val prompt = "Summarize this note in 3 dynamic key bullets and list actions. Title: $title. Content: $content"
        callGemini(prompt) { output ->
            noteContentInput.value = noteContentInput.value + "\n\n--- AI SUMMARY ---\n" + output
        }
    }

    fun optimizeNoteGrammar() {
        val content = noteContentInput.value
        if (content.isEmpty()) return
        val prompt = "Revise and dramatically enrich this note text to be highly professional, elegant and polished, maintaining original context: \"$content\""
        callGemini(prompt) { output ->
            noteContentInput.value = output
        }
    }

    // Note operations
    fun saveNote() {
        val title = noteTitleInput.value.ifEmpty { "Untitled Notes" }
        val content = noteContentInput.value
        if (content.isEmpty()) return

        val currentEditing = editingNote.value
        viewModelScope.launch {
            if (currentEditing != null) {
                db.appDao().insertNote(
                    currentEditing.copy(
                        title = title,
                        content = content,
                        category = noteCategoryInput.value,
                        colorHex = noteColorInput.value,
                        timestamp = System.currentTimeMillis()
                    )
                )
            } else {
                db.appDao().insertNote(
                    Note(
                        title = title,
                        content = content,
                        category = noteCategoryInput.value,
                        colorHex = noteColorInput.value
                    )
                )
            }
            // Clear inputs
            noteTitleInput.value = ""
            noteContentInput.value = ""
            editingNote.value = null
        }
    }

    fun startEditNote(note: Note) {
        editingNote.value = note
        noteTitleInput.value = note.title
        noteContentInput.value = note.content
        noteCategoryInput.value = note.category
        noteColorInput.value = note.colorHex
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            db.appDao().deleteNoteById(noteId)
        }
    }

    // Habit operations
    fun addHabit(name: String) {
        if (name.isEmpty()) return
        viewModelScope.launch {
            db.appDao().insertHabit(Habit(name = name))
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val completed = !habit.isCompletedToday
            val streak = if (completed) habit.streak + 1 else max(0, habit.streak - 1)
            db.appDao().updateHabit(
                habit.copy(
                    isCompletedToday = completed,
                    streak = streak,
                    lastCompletedTimestamp = if (completed) now else habit.lastCompletedTimestamp
                )
            )
        }
    }

    fun deleteHabit(habitId: Int) {
        viewModelScope.launch {
            db.appDao().deleteHabitById(habitId)
        }
    }

    // Budget Tracker ops
    val budgetTitle = MutableStateFlow("")
    val budgetAmount = MutableStateFlow("")
    val budgetType = MutableStateFlow("Expense") // Income or Expense
    val budgetCategory = MutableStateFlow("Food")

    fun addExpense() {
        val title = budgetTitle.value.ifEmpty { "Transaction" }
        val amt = budgetAmount.value.toDoubleOrNull() ?: 0.0
        if (amt <= 0.0) return

        viewModelScope.launch {
            db.appDao().insertExpense(
                Expense(
                    title = title,
                    amount = amt,
                    type = budgetType.value,
                    category = budgetCategory.value
                )
            )
            // Reset
            budgetTitle.value = ""
            budgetAmount.value = ""
        }
    }

    fun deleteExpense(id: Int) {
        viewModelScope.launch {
            db.appDao().deleteExpenseById(id)
        }
    }

    // AI Chat Convo Panel
    val chatInputText = MutableStateFlow("")
    fun submitChatToAi() {
        val text = chatInputText.value.trim()
        if (text.isEmpty()) return

        val currentList = chatHistory.value.toMutableList()
        currentList.add(Pair(text, true)) // Add User turn
        chatHistory.value = currentList
        chatInputText.value = ""

        // Build conversation dynamic prompt to retain history context
        var chatPrompt = "The following is a persistent helpful assistant conversation. Respond beautifully.\n"
        currentList.forEach { turn ->
            chatPrompt += if (turn.second) "User: " else "AI: "
            chatPrompt += turn.first + "\n"
        }
        chatPrompt += "AI:"

        callGemini(chatPrompt) { response ->
            val updated = chatHistory.value.toMutableList()
            updated.add(Pair(response, false)) // Add AI response
            chatHistory.value = updated
        }
    }

    fun clearChat() {
        chatHistory.value = emptyList()
    }

    // BMI Calculator Resolver
    fun calcBmiMetric() {
        val w = bmiWeight.value.toDoubleOrNull() ?: return
        val h = bmiHeight.value.toDoubleOrNull() ?: return // height in cm
        if (w <= 0.0 || h <= 0.0) return

        val heightMeters = h / 100.0
        val bmiVal = w / (heightMeters * heightMeters)
        bmiResult.value = bmiVal

        val adviceStr = when {
            bmiVal < 18.5 -> "Underweight. We recommend a balanced, nutrition-rich calorie surplus and standard building exercises."
            bmiVal < 25.0 -> "Healthy Weight! Perfect proportions. Maintain your steady dietary balance and active lifestyle."
            bmiVal < 30.0 -> "Overweight. Simple changes can yield great results—incorporate portion controls and cardio 3x weekly."
            else -> "Obese. Prioritize active cardiovascular schedules, mindful caloric deficits, and consult a professional clinician."
        }
        bmiAdvice.value = adviceStr

        viewModelScope.launch {
            db.appDao().insertHistoryItem(
                HistoryItem(
                    toolType = "BMI Tool",
                    title = "BMI Result",
                    result = String.format(Locale.US, "Weight: %.1fkg, Height: %.1fcm. BMI: %.1f -> %s", w, h, bmiVal, adviceStr)
                )
            )
        }
    }

    // Currency Conversion
    fun convertCurrency() {
        val amt = currencyAmount.value.toDoubleOrNull() ?: 1.0
        val from = currencyFrom.value
        val to = currencyTo.value

        // Mock conversion rates (Offline fallback but robust looking metrics)
        val rates = mapOf(
            "USD" to 1.0,
            "EUR" to 0.92,
            "GBP" to 0.79,
            "JPY" to 157.4,
            "CAD" to 1.37,
            "AUD" to 1.51,
            "INR" to 83.5
        )

        val usdValue = amt / (rates[from] ?: 1.0)
        val targetValue = usdValue * (rates[to] ?: 1.0)

        val resText = String.format(Locale.US, "%.2f %s = %.2f %s", amt, from, targetValue, to)
        currencyConvertedResult.value = resText

        viewModelScope.launch {
            db.appDao().insertHistoryItem(
                HistoryItem(toolType = "Currency", title = "$from to $to Converter", result = resText)
            )
        }
    }

    // Unit conversions
    fun convertUnits() {
        val value = unitFromValue.value.toDoubleOrNull() ?: return
        val cat = unitCategory.value
        val from = unitFromUnit.value
        val to = unitToUnit.value

        var resultVal = 0.0
        if (cat == "Length") {
            // Base to Meter
            val meters = when (from) {
                "m" -> value
                "km" -> value * 1000.0
                "cm" -> value / 100.0
                "Inch" -> value * 0.0254
                "Foot" -> value * 0.3048
                else -> value
            }
            // Meter to standard to
            resultVal = when (to) {
                "m" -> meters
                "km" -> meters / 1000.0
                "cm" -> meters * 100.0
                "Inch" -> meters / 0.0254
                "Foot" -> meters / 0.3048
                else -> meters
            }
        } else if (cat == "Weight") {
            // Base to kg
            val kgs = when (from) {
                "kg" -> value
                "g" -> value / 1000.0
                "lb" -> value * 0.453592
                "oz" -> value * 0.0283495
                else -> value
            }
            resultVal = when (to) {
                "kg" -> kgs
                "g" -> kgs * 1000.0
                "lb" -> kgs / 0.453592
                "oz" -> kgs / 0.0283495
                else -> kgs
            }
        } else if (cat == "Temp") {
            resultVal = when {
                from == "C" && to == "F" -> (value * 9 / 5) + 32
                from == "C" && to == "K" -> value + 273.15
                from == "F" && to == "C" -> (value - 32) * 5 / 9
                from == "F" && to == "K" -> ((value - 32) * 5 / 9) + 273.15
                from == "K" && to == "C" -> value - 273.15
                from == "K" && to == "F" -> ((value - 273.15) * 9 / 5) + 32
                else -> value
            }
        }

        val resStr = String.format(Locale.US, "%.4f %s = %.4f %s", value, from, resultVal, to).trimEnd('0').trimEnd('.')
        unitToValue.value = resStr
    }

    // Split & Tip Solver
    fun calculateSplit() {
        val bill = splitBillAmount.value.toDoubleOrNull() ?: 0.0
        val tipPercent = splitTipPercentage.value.toDoubleOrNull() ?: 15.0
        val people = splitPeopleCount.value.toIntOrNull() ?: 1
        if (bill <= 0.0 || people <= 0) return

        val totalTip = bill * (tipPercent / 100.0)
        val totalBill = bill + totalTip
        val perPerson = totalBill / people

        val outMsg = String.format(
            Locale.US,
            "Total Bill: $%.2f (Tip: $%.2f)\nPer Person Split: $%.2f (for %d people)",
            totalBill, totalTip, perPerson, people
        )
        splitResultText.value = outMsg
    }

    // Decision wheel roll
    fun rollDecision() {
        val items = wheelInput.value.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (items.isEmpty()) return
        chosenDecision.value = "Choosing randomly..."
        isWheelSpinning.value = true
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200) // Spin animation feeling
            val selected = items.random()
            chosenDecision.value = "Selected Option:\n\uD83C\uDF89 $selected \uD83C\uDF89"
            isWheelSpinning.value = false

            db.appDao().insertHistoryItem(
                HistoryItem(toolType = "Decision", title = "Rolled Wheel", result = "Selected: $selected from [${items.joinToString()}]")
            )
        }
    }

    // Build Passcodes
    fun generatePasswordSecret() {
        val length = pwdLength.value
        val chars = StringBuilder()
        chars.append("abcdefghijklmnopqrstuvwxyz")
        if (pwdIncludeUpper.value) chars.append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
        if (pwdIncludeNumbers.value) chars.append("0123456789")
        if (pwdIncludeSymbols.value) chars.append("!@#$%^&*()_+=-[]{}|;:,.<>?")

        if (chars.isEmpty()) {
            pwdGenerated.value = ""
            return
        }

        val randomGen = SecureRandom()
        val pass = StringBuilder()
        for (i in 0 until length) {
            val idx = randomGen.nextInt(chars.length)
            pass.append(chars[idx])
        }
        pwdGenerated.value = pass.toString()
    }

    // Load World Clock times dynamically
    fun updateWorldTimes() {
        val sdf = SimpleDateFormat("HH:mm (EEE)", Locale.US)
        val results = mutableMapOf<String, String>()
        
        val zones = mapOf(
            "New York (EST)" to "America/New_York",
            "London (GMT)" to "Europe/London",
            "Paris / Berlin" to "Europe/Paris",
            "Dubai (GST)" to "Asia/Dubai",
            "New Delhi (IST)" to "Asia/Kolkata",
            "Tokyo (JST)" to "Asia/Tokyo",
            "Sydney (AEST)" to "Australia/Sydney",
            "Singapore" to "Asia/Singapore"
        )

        zones.forEach { (label, zoneId) ->
            sdf.timeZone = TimeZone.getTimeZone(zoneId)
            results[label] = sdf.format(Date())
        }
        wordClockTimes.value = results
    }

    // AI Study Buddy / Flashcards Generator
    val studyTopic = MutableStateFlow("")
    val studyResultList = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    fun generateFlashcards() {
        val topic = studyTopic.value.trim()
        if (topic.isEmpty()) return
        aiLoading.value = true
        val prompt = "Generate exactly 4 critical learning Flashcards about this topic: \"$topic\". Output them styled clearly with Question and Answer pairings."
        callGemini(prompt) { output ->
            // Parse custom Q&As or output list simply
            val pairs = mutableListOf<Pair<String, String>>()
            val sections = output.split(Regex("(?=Question:|Q:|Question\\d+:|Q\\d+:)"))
            sections.forEach { section ->
                if (section.contains("Answer:") || section.contains("A:")) {
                    val splitParts = section.split(Regex("(?=Answer:|A:)"))
                    val q = splitParts.getOrNull(0)?.replace(Regex("(Question:|Q:|\\d+\\.\\s*)"), "")?.trim() ?: ""
                    val a = splitParts.getOrNull(1)?.replace(Regex("(Answer:|A:)"), "")?.trim() ?: ""
                    if (q.isNotEmpty() && a.isNotEmpty()) {
                        pairs.add(Pair(q, a))
                    }
                }
            }
            if (pairs.isEmpty()) {
                pairs.add(Pair("Topic Study Guide", output))
            }
            studyResultList.value = pairs
        }
    }

    // AI Goal Planner Checklist
    val goalPlannerTopic = MutableStateFlow("")
    val goalPlannerSteps = MutableStateFlow<List<String>>(emptyList())

    fun generateGoalPlanner() {
        val topic = goalPlannerTopic.value.trim()
        if (topic.isEmpty()) return
        aiLoading.value = true
        val prompt = "Create a beautifully structured daily habit workflow or checklist of exactly 5 steps to accomplish this objective: \"$topic\". Output only the numbered steps, 1 line per step."
        callGemini(prompt) { output ->
            val steps = output.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() && (it.first().isDigit() || it.startsWith("-") || it.startsWith("*")) }
                .map { it.replace(Regex("^(\\d+\\.\\s*|-|\\*)\\s*"), "") }
            goalPlannerSteps.value = steps.ifEmpty { listOf(output) }
        }
    }

    fun applyGoalStepAsHabit(stepName: String) {
        if (stepName.trim().isEmpty()) return
        addHabit(stepName.trim())
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            db.appDao().clearHistory()
        }
    }
}
