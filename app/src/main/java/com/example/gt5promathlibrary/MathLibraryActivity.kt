package com.example.gt5promathlibrary
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.huawei.wearengine.HiWear
import com.huawei.wearengine.auth.AuthCallback
import com.huawei.wearengine.auth.Permission
import com.huawei.wearengine.client.WearEngineClient
import com.huawei.wearengine.device.Device
import com.huawei.wearengine.p2p.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.*

/**
 * Biblioteca Matemática para Huawei GT 5 Pro
 * Incluye funciones avanzadas, sincronización y gestión de fórmulas
 */
class MathLibraryActivity : Activity() {

    companion object {
        private const val TAG = "MathLibraryActivity"
        private const val PREF_NAME = "MathLibraryPrefs"
        private const val SYNC_MESSAGE_TYPE = 0x1001
        private const val FORMULA_REQUEST_TYPE = 0x1002
    }

    // UI Components
    private lateinit var categorySpinner: Spinner
    private lateinit var searchEditText: EditText
    private lateinit var formulaRecyclerView: RecyclerView
    private lateinit var calculatorContainer: LinearLayout
    private lateinit var resultTextView: TextView
    private lateinit var expressionEditText: EditText
    private lateinit var syncButton: Button
    private lateinit var addFormulaButton: Button

    // Data
    private lateinit var mathLibrary: MathLibrary
    private lateinit var formulaAdapter: FormulaAdapter
    private lateinit var wearEngineClient: WearEngineClient
    private var connectedDevice: Device? = null
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_math_library)

        initializeComponents()
        setupUI()
        initializeWearEngine()
        loadFormulas()
    }

    private fun initializeComponents() {
        mathLibrary = MathLibrary(this)

        // Initialize UI components
        categorySpinner = findViewById(R.id.category_spinner)
        searchEditText = findViewById(R.id.search_edit_text)
        formulaRecyclerView = findViewById(R.id.formula_recycler_view)
        calculatorContainer = findViewById(R.id.calculator_container)
        resultTextView = findViewById(R.id.result_text_view)
        expressionEditText = findViewById(R.id.expression_edit_text)
        syncButton = findViewById(R.id.sync_button)
        addFormulaButton = findViewById(R.id.add_formula_button)
    }

    private fun setupUI() {
        // Setup category spinner
        val categories = mathLibrary.getCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterFormulas()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Setup search functionality
        searchEditText.setOnEditorActionListener { _, _, _ ->
            filterFormulas()
            true
        }

        // Setup RecyclerView
        formulaAdapter = FormulaAdapter { formula ->
            expressionEditText.setText(formula.expression)
            showFormulaDetails(formula)
        }
        formulaRecyclerView.layoutManager = LinearLayoutManager(this)
        formulaRecyclerView.adapter = formulaAdapter

        // Setup calculator buttons
        setupCalculatorButtons()

        // Setup action buttons
        syncButton.setOnClickListener { syncWithDevice() }
        addFormulaButton.setOnClickListener { showAddFormulaDialog() }

        findViewById<Button>(R.id.calculate_button).setOnClickListener {
            calculateExpression()
        }
    }

    private fun setupCalculatorButtons() {
        val buttonIds = arrayOf(
            R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
            R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9,
            R.id.btn_plus, R.id.btn_minus, R.id.btn_multiply, R.id.btn_divide,
            R.id.btn_equals, R.id.btn_clear, R.id.btn_dot, R.id.btn_parenthesis_open,
            R.id.btn_parenthesis_close, R.id.btn_sin, R.id.btn_cos, R.id.btn_tan,
            R.id.btn_log, R.id.btn_ln, R.id.btn_sqrt, R.id.btn_power, R.id.btn_pi,
            R.id.btn_e, R.id.btn_factorial
        )

        buttonIds.forEach { id ->
            findViewById<Button>(id)?.setOnClickListener { button ->
                onCalculatorButtonClick(button as Button)
            }
        }
    }

    private fun onCalculatorButtonClick(button: Button) {
        val currentText = expressionEditText.text.toString()
        val buttonText = button.text.toString()

        when (button.id) {
            R.id.btn_clear -> {
                expressionEditText.setText("")
                resultTextView.text = "0"
            }
            R.id.btn_equals -> calculateExpression()
            else -> {
                val newText = when (buttonText) {
                    "π" -> currentText + "π"
                    "e" -> currentText + "e"
                    "√" -> currentText + "sqrt("
                    "x!" -> currentText + "!"
                    "x²" -> currentText + "^2"
                    "sin" -> currentText + "sin("
                    "cos" -> currentText + "cos("
                    "tan" -> currentText + "tan("
                    "log" -> currentText + "log("
                    "ln" -> currentText + "ln("
                    else -> currentText + buttonText
                }
                expressionEditText.setText(newText)
                expressionEditText.setSelection(newText.length)
            }
        }
    }

    private fun calculateExpression() {
        val expression = expressionEditText.text.toString()
        if (expression.isEmpty()) return

        try {
            val result = mathLibrary.evaluateExpression(expression)
            resultTextView.text = mathLibrary.formatResult(result)

            // Save to history
            mathLibrary.addToHistory(expression, result)

        } catch (e: Exception) {
            resultTextView.text = "Error: ${e.message}"
            Log.e(TAG, "Error calculating expression: $expression", e)
        }
    }

    private fun filterFormulas() {
        val category = categorySpinner.selectedItem.toString()
        val searchQuery = searchEditText.text.toString()
        val filteredFormulas = mathLibrary.searchFormulas(searchQuery, category)
        formulaAdapter.updateFormulas(filteredFormulas)
    }

    private fun loadFormulas() {
        val formulas = mathLibrary.getAllFormulas()
        formulaAdapter.updateFormulas(formulas)
    }

    private fun showFormulaDetails(formula: MathFormula) {
        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle(formula.name)
            .setMessage("Categoría: ${formula.category}\n\n" +
                    "Expresión: ${formula.expression}\n\n" +
                    "Descripción: ${formula.description}\n\n" +
                    "Variables: ${formula.variables.joinToString(", ")}")
            .setPositiveButton("Usar") { _, _ ->
                expressionEditText.setText(formula.expression)
            }
            .setNegativeButton("Editar") { _, _ ->
                showEditFormulaDialog(formula)
            }
            .setNeutralButton("Eliminar") { _, _ ->
                deleteFormula(formula)
            }
            .create()
        dialog.show()
    }

    private fun showAddFormulaDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_formula, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.formula_name_edit_text)
        val expressionEditText = dialogView.findViewById<EditText>(R.id.formula_expression_edit_text)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.formula_description_edit_text)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.formula_category_spinner)
        val variablesEditText = dialogView.findViewById<EditText>(R.id.formula_variables_edit_text)

        val categories = mathLibrary.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categorySpinner.adapter = adapter

        android.app.AlertDialog.Builder(this)
            .setTitle("Agregar Fórmula")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val formula = MathFormula(
                    id = System.currentTimeMillis().toString(),
                    name = nameEditText.text.toString(),
                    expression = expressionEditText.text.toString(),
                    description = descriptionEditText.text.toString(),
                    category = categorySpinner.selectedItem.toString(),
                    variables = variablesEditText.text.toString().split(",").map { it.trim() },
                    createdAt = Date(),
                    isFavorite = false
                )
                mathLibrary.addFormula(formula)
                loadFormulas()
                syncWithDevice()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditFormulaDialog(formula: MathFormula) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_formula, null)
        val nameEditText = dialogView.findViewById<EditText>(R.id.formula_name_edit_text)
        val expressionEditText = dialogView.findViewById<EditText>(R.id.formula_expression_edit_text)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.formula_description_edit_text)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.formula_category_spinner)
        val variablesEditText = dialogView.findViewById<EditText>(R.id.formula_variables_edit_text)

        // Pre-fill with existing data
        nameEditText.setText(formula.name)
        expressionEditText.setText(formula.expression)
        descriptionEditText.setText(formula.description)
        variablesEditText.setText(formula.variables.joinToString(", "))

        val categories = mathLibrary.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categorySpinner.adapter = adapter
        categorySpinner.setSelection(categories.indexOf(formula.category))

        android.app.AlertDialog.Builder(this)
            .setTitle("Editar Fórmula")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val updatedFormula = formula.copy(
                    name = nameEditText.text.toString(),
                    expression = expressionEditText.text.toString(),
                    description = descriptionEditText.text.toString(),
                    category = categorySpinner.selectedItem.toString(),
                    variables = variablesEditText.text.toString().split(",").map { it.trim() },
                    updatedAt = Date()
                )
                mathLibrary.updateFormula(updatedFormula)
                loadFormulas()
                syncWithDevice()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteFormula(formula: MathFormula) {
        android.app.AlertDialog.Builder(this)
            .setTitle("Eliminar Fórmula")
            .setMessage("¿Estás seguro de que quieres eliminar '${formula.name}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                mathLibrary.deleteFormula(formula.id)
                loadFormulas()
                syncWithDevice()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Wear Engine Integration
    private fun initializeWearEngine() {
        try {
            wearEngineClient = HiWear.getWearEngineClient(this)
            checkPermissions()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Wear Engine", e)
            showToast("Error al inicializar Wear Engine")
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Permission.DEVICE_MANAGER,
            Permission.NOTIFY,
            Permission.SENSOR
        )

        wearEngineClient.checkPermissions(permissions).addOnSuccessListener { result ->
            if (result) {
                connectToDevice()
            } else {
                requestPermissions()
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error checking permissions", e)
        }
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Permission.DEVICE_MANAGER,
            Permission.NOTIFY,
            Permission.SENSOR
        )

        wearEngineClient.requestPermissions(this, permissions, object : AuthCallback {
            override fun onOk(permissions: Array<out Permission>?) {
                connectToDevice()
            }

            override fun onCancel() {
                showToast("Permisos requeridos para la sincronización")
            }
        })
    }

    private fun connectToDevice() {
        wearEngineClient.deviceClient.bondedDevices.addOnSuccessListener { devices ->
            if (devices.isNotEmpty()) {
                connectedDevice = devices.first()
                isConnected = true
                setupP2PConnection()
                updateSyncButtonState()
                showToast("Conectado a ${connectedDevice?.name}")
            } else {
                showToast("No se encontraron dispositivos vinculados")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error getting bonded devices", e)
            showToast("Error al conectar con el dispositivo")
        }
    }

    private fun setupP2PConnection() {
        connectedDevice?.let { device ->
            val p2pClient = wearEngineClient.getP2pClient(device)

            p2pClient.registerReceiver(object : Receiver {
                override fun onReceiveMessage(message: Message?) {
                    message?.let { handleReceivedMessage(it) }
                }
            }).addOnSuccessListener {
                Log.d(TAG, "P2P receiver registered successfully")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error registering P2P receiver", e)
            }
        }
    }

    private fun handleReceivedMessage(message: Message) {
        when (message.type) {
            SYNC_MESSAGE_TYPE -> {
                val data = String(message.data)
                try {
                    val jsonData = JSONObject(data)
                    handleSyncData(jsonData)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing sync message", e)
                }
            }
            FORMULA_REQUEST_TYPE -> {
                sendFormulasToDevice()
            }
        }
    }

    private fun handleSyncData(jsonData: JSONObject) {
        try {
            val formulasArray = jsonData.getJSONArray("formulas")
            val receivedFormulas = mutableListOf<MathFormula>()

            for (i in 0 until formulasArray.length()) {
                val formulaJson = formulasArray.getJSONObject(i)
                val formula = MathFormula.fromJson(formulaJson)
                receivedFormulas.add(formula)
            }

            // Merge formulas
            mathLibrary.mergeFormulas(receivedFormulas)

            runOnUiThread {
                loadFormulas()
                showToast("Sincronización completada")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling sync data", e)
            runOnUiThread {
                showToast("Error en la sincronización")
            }
        }
    }

    private fun syncWithDevice() {
        if (!isConnected || connectedDevice == null) {
            showToast("No hay dispositivo conectado")
            return
        }

        try {
            val formulas = mathLibrary.getAllFormulas()
            val jsonData = JSONObject()
            val formulasArray = JSONArray()

            formulas.forEach { formula ->
                formulasArray.put(formula.toJson())
            }

            jsonData.put("formulas", formulasArray)
            jsonData.put("timestamp", System.currentTimeMillis())

            val message = Message.Builder()
                .setType(SYNC_MESSAGE_TYPE)
                .setData(jsonData.toString().toByteArray())
                .build()

            val p2pClient = wearEngineClient.getP2pClient(connectedDevice!!)
            p2pClient.send(message).addOnSuccessListener {
                showToast("Sincronización enviada")
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error sending sync data", e)
                showToast("Error al sincronizar")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error preparing sync data", e)
            showToast("Error al preparar la sincronización")
        }
    }

    private fun sendFormulasToDevice() {
        syncWithDevice()
    }

    private fun updateSyncButtonState() {
        syncButton.isEnabled = isConnected
        syncButton.text = if (isConnected) "Sincronizar" else "Desconectado"
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            connectedDevice?.let { device ->
                wearEngineClient.getP2pClient(device).unregisterReceiver()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Wear Engine", e)
        }
    }
}

/**
 * Clase principal de la biblioteca matemática
 */
class MathLibrary(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "MathLibraryPrefs"
        private const val FORMULAS_KEY = "formulas"
        private const val HISTORY_KEY = "history"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val formulas = mutableListOf<MathFormula>()
    private val history = mutableListOf<CalculationHistory>()

    init {
        loadData()
        initializeDefaultFormulas()
    }

    private fun loadData() {
        // Load formulas
        val formulasJson = prefs.getString(FORMULAS_KEY, "[]")
        try {
            val jsonArray = JSONArray(formulasJson)
            for (i in 0 until jsonArray.length()) {
                val formula = MathFormula.fromJson(jsonArray.getJSONObject(i))
                formulas.add(formula)
            }
        } catch (e: Exception) {
            Log.e("MathLibrary", "Error loading formulas", e)
        }

        // Load history
        val historyJson = prefs.getString(HISTORY_KEY, "[]")
        try {
            val jsonArray = JSONArray(historyJson)
            for (i in 0 until jsonArray.length()) {
                val historyItem = CalculationHistory.fromJson(jsonArray.getJSONObject(i))
                history.add(historyItem)
            }
        } catch (e: Exception) {
            Log.e("MathLibrary", "Error loading history", e)
        }
    }

    private fun saveData() {
        val editor = prefs.edit()

        // Save formulas
        val formulasArray = JSONArray()
        formulas.forEach { formula ->
            formulasArray.put(formula.toJson())
        }
        editor.putString(FORMULAS_KEY, formulasArray.toString())

        // Save history
        val historyArray = JSONArray()
        history.forEach { item ->
            historyArray.put(item.toJson())
        }
        editor.putString(HISTORY_KEY, historyArray.toString())

        editor.apply()
    }

    private fun initializeDefaultFormulas() {
        if (formulas.isEmpty()) {
            val defaultFormulas = listOf(
                MathFormula(
                    id = "1",
                    name = "Área del Círculo",
                    expression = "π * r^2",
                    description = "Calcula el área de un círculo dado su radio",
                    category = "Geometría",
                    variables = listOf("r"),
                    createdAt = Date(),
                    isFavorite = true
                ),
                MathFormula(
                    id = "2",
                    name = "Teorema de Pitágoras",
                    expression = "sqrt(a^2 + b^2)",
                    description = "Calcula la hipotenusa de un triángulo rectángulo",
                    category = "Geometría",
                    variables = listOf("a", "b"),
                    createdAt = Date(),
                    isFavorite = true
                ),
                MathFormula(
                    id = "3",
                    name = "Fórmula Cuadrática",
                    expression = "(-b ± sqrt(b^2 - 4*a*c)) / (2*a)",
                    description = "Solución de ecuaciones cuadráticas",
                    category = "Álgebra",
                    variables = listOf("a", "b", "c"),
                    createdAt = Date(),
                    isFavorite = false
                ),
                MathFormula(
                    id = "4",
                    name = "Volumen de Esfera",
                    expression = "(4/3) * π * r^3",
                    description = "Calcula el volumen de una esfera",
                    category = "Geometría",
                    variables = listOf("r"),
                    createdAt = Date(),
                    isFavorite = false
                ),
                MathFormula(
                    id = "5",
                    name = "Interés Compuesto",
                    expression = "P * (1 + r/n)^(n*t)",
                    description = "Calcula el interés compuesto",
                    category = "Finanzas",
                    variables = listOf("P", "r", "n", "t"),
                    createdAt = Date(),
                    isFavorite = false
                )
            )

            formulas.addAll(defaultFormulas)
            saveData()
        }
    }

    fun evaluateExpression(expression: String): Double {
        var expr = expression.replace("π", PI.toString())
            .replace("e", E.toString())

        // Handle basic mathematical functions
        expr = handleMathFunctions(expr)

        // Simple expression evaluator (for production, use a proper math parser)
        return evaluateSimpleExpression(expr)
    }

    private fun handleMathFunctions(expr: String): String {
        var result = expr

        // Handle sqrt
        result = result.replace(Regex("sqrt\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            sqrt(value).toString()
        }

        // Handle sin, cos, tan
        result = result.replace(Regex("sin\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            sin(value).toString()
        }

        result = result.replace(Regex("cos\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            cos(value).toString()
        }

        result = result.replace(Regex("tan\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            tan(value).toString()
        }

        // Handle log and ln
        result = result.replace(Regex("log\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            log10(value).toString()
        }

        result = result.replace(Regex("ln\\(([^)]+)\\)")) { matchResult ->
            val value = evaluateSimpleExpression(matchResult.groupValues[1])
            ln(value).toString()
        }

        return result
    }

    private fun evaluateSimpleExpression(expr: String): Double {
        // This is a simplified evaluator. For production, use a proper expression parser
        try {
            // Remove spaces
            val cleanExpr = expr.replace(" ", "")

            // Handle power operations
            var result = cleanExpr
            result = result.replace(Regex("([0-9.]+)\\^([0-9.]+)")) { matchResult ->
                val base = matchResult.groupValues[1].toDouble()
                val exponent = matchResult.groupValues[2].toDouble()
                base.pow(exponent).toString()
            }

            // Handle factorial
            result = result.replace(Regex("([0-9]+)!")) { matchResult ->
                val num = matchResult.groupValues[1].toInt()
                factorial(num).toString()
            }

            // Basic arithmetic evaluation (simplified)
            return evaluateArithmetic(result)

        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid expression: $expr")
        }
    }

    private fun evaluateArithmetic(expr: String): Double {
        // This is a very basic arithmetic evaluator
        // For production use, implement a proper expression parser

        try {
            // Handle parentheses first
            var result = expr
            while (result.contains("(")) {
                val start = result.lastIndexOf("(")
                val end = result.indexOf(")", start)
                val subExpr = result.substring(start + 1, end)
                val subResult = evaluateArithmetic(subExpr)
                result = result.substring(0, start) + subResult + result.substring(end + 1)
            }

            // Handle multiplication and division
            result = handleOperations(result, arrayOf("*", "/"))

            // Handle addition and subtraction
            result = handleOperations(result, arrayOf("+", "-"))

            return result.toDouble()

        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid arithmetic expression: $expr")
        }
    }

    private fun handleOperations(expr: String, operators: Array<String>): String {
        var result = expr

        for (op in operators) {
            while (result.contains(op)) {
                val opIndex = result.indexOf(op)
                if (opIndex == 0 && op == "-") {
                    // Handle negative numbers
                    val nextOp = findNextOperator(result, 1)
                    if (nextOp == -1) break
                    continue
                }

                val leftNum = extractLeftNumber(result, opIndex)
                val rightNum = extractRightNumber(result, opIndex)

                val leftValue = leftNum.toDouble()
                val rightValue = rightNum.toDouble()

                val operationResult = when (op) {
                    "+" -> leftValue + rightValue
                    "-" -> leftValue - rightValue
                    "*" -> leftValue * rightValue
                    "/" -> leftValue / rightValue
                    else -> throw IllegalArgumentException("Unknown operator: $op")
                }

                val startIndex = opIndex - leftNum.length
                val endIndex = opIndex + 1 + rightNum.length

                result = result.substring(0, startIndex) + operationResult + result.substring(endIndex)
            }
        }

        return result
    }

    private fun extractLeftNumber(expr: String, opIndex: Int): String {
        var start = opIndex - 1
        while (start >= 0 && (expr[start].isDigit() || expr[start] == '.')) {
            start--
        }
        return expr.substring(start + 1, opIndex)
    }

    private fun extractRightNumber(expr: String, opIndex: Int): String {
        var end = opIndex + 1
        if (end < expr.length && expr[end] == '-') {
            end++ // Handle negative numbers
        }
        while (end < expr.length && (expr[end].isDigit() || expr[end] == '.')) {
            end++
        }
        return expr.substring(opIndex + 1, end)
    }

    private fun findNextOperator(expr: String, startIndex: Int): Int {
        val operators = arrayOf("+", "-", "*", "/")
        for (i in startIndex until expr.length) {
            if (operators.contains(expr[i].toString())) {
                return i
            }
        }
        return -1
    }

    private fun factorial(n: Int): Long {
        if (n < 0) throw IllegalArgumentException("Factorial is not defined for negative numbers")
        if (n == 0 || n == 1) return 1
        var result = 1L
        for (i in 2..n) {
            result *= i
        }
        return result
    }

    fun formatResult(result: Double): String {
        return when {
            result == result.toLong().toDouble() -> result.toLong().toString()
            result.isInfinite() -> "∞"
            result.isNaN() -> "Error"
            else -> String.format("%.6f", result).trimEnd('0').trimEnd('.')
        }
    }

    fun addToHistory(expression: String, result: Double) {
        val historyItem = CalculationHistory(
            id = System.currentTimeMillis().toString(),
            expression = expression,
            result = result,
            timestamp = Date()
        )
        history.add(0, historyItem) // Add to beginning

        // Keep only last 100 items
        if (history.size > 100) {
            history.removeAt(history.size - 1)
        }

        saveData()
    }

    fun getHistory(): List<CalculationHistory> = history.toList()

    fun clearHistory() {
        history.clear()
        saveData()
    }

    // Formula CRUD operations
    fun getAllFormulas(): List<MathFormula> = formulas.toList()

    fun addFormula(formula: MathFormula) {
        formulas.add(formula)
        saveData()
    }

    fun updateFormula(formula: MathFormula) {
        val index = formulas.indexOfFirst { it.id == formula.id }
        if (index != -1) {
            formulas[index] = formula
            saveData()
        }
    }

    fun deleteFormula(formulaId: String) {
        formulas.removeAll { it.id == formulaId }
        saveData()
    }

    fun getFormulaById(id: String): MathFormula? {
        return formulas.find { it.id == id }
    }

    fun searchFormulas(query: String, category: String = "Todas"): List<MathFormula> {
        var filtered = if (category == "Todas") {
            formulas
        } else {
            formulas.filter { it.category == category }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter { formula ->
                formula.name.contains(query, ignoreCase = true) ||
                        formula.description.contains(query, ignoreCase = true) ||
                        formula.expression.contains(query, ignoreCase = true) ||
                        formula.variables.any { it.contains(query, ignoreCase = true) }
            }
        }

        return filtered.sortedWith(compareByDescending<MathFormula> { it.isFavorite }
            .thenBy { it.name })
    }

    fun getCategories(): List<String> {
        val categories = mutableSetOf("Todas")
        categories.addAll(formulas.map { it.category })
        return categories.toList().sorted()
    }

    fun getFavoriteFormulas(): List<MathFormula> {
        return formulas.filter { it.isFavorite }
    }

    fun toggleFavorite(formulaId: String) {
        val formula = formulas.find { it.id == formulaId }
        formula?.let {
            val updated = it.copy(isFavorite = !it.isFavorite)
            updateFormula(updated)
        }
    }

    fun mergeFormulas(remoteFormulas: List<MathFormula>) {
        for (remoteFormula in remoteFormulas) {
            val existingFormula = formulas.find { it.id == remoteFormula.id }

            if (existingFormula == null) {
                // New formula from remote
                formulas.add(remoteFormula)
            } else {
                // Check which is newer
                val remoteTime = remoteFormula.updatedAt ?: remoteFormula.createdAt
                val localTime = existingFormula.updatedAt ?: existingFormula.createdAt

                if (remoteTime.after(localTime)) {
                    // Remote is newer, update local
                    updateFormula(remoteFormula)
                }
            }
        }
        saveData()
    }

    fun exportFormulas(): String {
        val jsonArray = JSONArray()
        formulas.forEach { formula ->
            jsonArray.put(formula.toJson())
        }
        return jsonArray.toString()
    }

    fun importFormulas(jsonString: String): Boolean {
        try {
            val jsonArray = JSONArray(jsonString)
            val importedFormulas = mutableListOf<MathFormula>()

            for (i in 0 until jsonArray.length()) {
                val formula = MathFormula.fromJson(jsonArray.getJSONObject(i))
                importedFormulas.add(formula)
            }

            mergeFormulas(importedFormulas)
            return true

        } catch (e: Exception) {
            Log.e("MathLibrary", "Error importing formulas", e)
            return false
        }
    }
}

/**
 * Data class para representar una fórmula matemática
 */
data class MathFormula(
    val id: String,
    val name: String,
    val expression: String,
    val description: String,
    val category: String,
    val variables: List<String>,
    val createdAt: Date,
    val updatedAt: Date? = null,
    val isFavorite: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("name", name)
        json.put("expression", expression)
        json.put("description", description)
        json.put("category", category)
        json.put("variables", JSONArray(variables))
        json.put("createdAt", createdAt.time)
        updatedAt?.let { json.put("updatedAt", it.time) }
        json.put("isFavorite", isFavorite)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): MathFormula {
            val variablesArray = json.getJSONArray("variables")
            val variables = mutableListOf<String>()
            for (i in 0 until variablesArray.length()) {
                variables.add(variablesArray.getString(i))
            }

            val createdAt = Date(json.getLong("createdAt"))
            val updatedAt = if (json.has("updatedAt")) {
                Date(json.getLong("updatedAt"))
            } else null

            return MathFormula(
                id = json.getString("id"),
                name = json.getString("name"),
                expression = json.getString("expression"),
                description = json.getString("description"),
                category = json.getString("category"),
                variables = variables,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isFavorite = json.optBoolean("isFavorite", false)
            )
        }
    }
}

/**
 * Data class para el historial de cálculos
 */
data class CalculationHistory(
    val id: String,
    val expression: String,
    val result: Double,
    val timestamp: Date
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("id", id)
        json.put("expression", expression)
        json.put("result", result)
        json.put("timestamp", timestamp.time)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): CalculationHistory {
            return CalculationHistory(
                id = json.getString("id"),
                expression = json.getString("expression"),
                result = json.getDouble("result"),
                timestamp = Date(json.getLong("timestamp"))
            )
        }
    }
}

/**
 * Adapter para el RecyclerView de fórmulas
 */
class FormulaAdapter(
    private val onFormulaClick: (MathFormula) -> Unit
) : RecyclerView.Adapter<FormulaAdapter.FormulaViewHolder>() {

    private var formulas = listOf<MathFormula>()

    fun updateFormulas(newFormulas: List<MathFormula>) {
        formulas = newFormulas
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): FormulaViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_formula, parent, false)
        return FormulaViewHolder(view)
    }

    override fun onBindViewHolder(holder: FormulaViewHolder, position: Int) {
        holder.bind(formulas[position])
    }

    override fun getItemCount(): Int = formulas.size

    inner class FormulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.formula_name)
        private val expressionTextView: TextView = itemView.findViewById(R.id.formula_expression)
        private val categoryTextView: TextView = itemView.findViewById(R.id.formula_category)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favorite_icon)

        fun bind(formula: MathFormula) {
            nameTextView.text = formula.name
            expressionTextView.text = formula.expression
            categoryTextView.text = formula.category

            favoriteIcon.visibility = if (formula.isFavorite) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                onFormulaClick(formula)
            }
        }
    }
}

/**
 * Layout definitions for the activities and dialogs
 * These would typically be in separate XML files
 */

// R.layout.activity_math_library
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <!-- Search and Filter Section -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:layout_marginBottom="16dp">

        <Spinner
            android:id="@+id/category_spinner"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginEnd="8dp" />

        <EditText
            android:id="@+id/search_edit_text"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="2"
            android:hint="Buscar fórmulas..."
            android:inputType="text"
            android:imeOptions="actionSearch" />

    </LinearLayout>

    <!-- Formulas List -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/formula_recycler_view"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:layout_marginBottom="16dp" />

    <!-- Calculator Section -->
    <LinearLayout
        android:id="@+id/calculator_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:background="@drawable/calculator_background"
        android:padding="16dp">

        <!-- Expression Input -->
        <EditText
            android:id="@+id/expression_edit_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Ingresa una expresión..."
            android:textSize="18sp"
            android:layout_marginBottom="8dp" />

        <!-- Result Display -->
        <TextView
            android:id="@+id/result_text_view"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="0"
            android:textSize="24sp"
            android:textStyle="bold"
            android:gravity="end"
            android:layout_marginBottom="16dp"
            android:background="@drawable/result_background"
            android:padding="12dp" />

        <!-- Calculator Buttons Grid -->
        <GridLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:columnCount="4"
            android:rowCount="6">

            <!-- Row 1: Functions -->
            <Button android:id="@+id/btn_sin" android:text="sin" />
            <Button android:id="@+id/btn_cos" android:text="cos" />
            <Button android:id="@+id/btn_tan" android:text="tan" />
            <Button android:id="@+id/btn_clear" android:text="C" />

            <!-- Row 2: More Functions -->
            <Button android:id="@+id/btn_log" android:text="log" />
            <Button android:id="@+id/btn_ln" android:text="ln" />
            <Button android:id="@+id/btn_sqrt" android:text="√" />
            <Button android:id="@+id/btn_power" android:text="x²" />

            <!-- Row 3: Constants and Operations -->
            <Button android:id="@+id/btn_pi" android:text="π" />
            <Button android:id="@+id/btn_e" android:text="e" />
            <Button android:id="@+id/btn_factorial" android:text="x!" />
            <Button android:id="@+id/btn_divide" android:text="÷" />

            <!-- Row 4: Numbers -->
            <Button android:id="@+id/btn_7" android:text="7" />
            <Button android:id="@+id/btn_8" android:text="8" />
            <Button android:id="@+id/btn_9" android:text="9" />
            <Button android:id="@+id/btn_multiply" android:text="×" />

            <!-- Row 5: Numbers -->
            <Button android:id="@+id/btn_4" android:text="4" />
            <Button android:id="@+id/btn_5" android:text="5" />
            <Button android:id="@+id/btn_6" android:text="6" />
            <Button android:id="@+id/btn_minus" android:text="-" />

            <!-- Row 6: Numbers -->
            <Button android:id="@+id/btn_1" android:text="1" />
            <Button android:id="@+id/btn_2" android:text="2" />
            <Button android:id="@+id/btn_3" android:text="3" />
            <Button android:id="@+id/btn_plus" android:text="+" />

            <!-- Row 7: Final row -->
            <Button android:id="@+id/btn_0" android:text="0" android:layout_columnSpan="2" />
            <Button android:id="@+id/btn_dot" android:text="." />
            <Button android:id="@+id/btn_equals" android:text="=" />

        </GridLayout>

        <!-- Action Buttons -->
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:layout_marginTop="16dp">

            <Button
                android:id="@+id/calculate_button"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Calcular"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/add_formula_button"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="+ Fórmula"
                android:layout_marginEnd="8dp" />

            <Button
                android:id="@+id/sync_button"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="Sincronizar" />

        </LinearLayout>

    </LinearLayout>

</LinearLayout>
*/

// R.layout.item_formula
/*
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp"
    android:background="?android:attr/selectableItemBackground"
    android:clickable="true"
    android:focusable="true">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal">

        <LinearLayout
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">

            <TextView
                android:id="@+id/formula_name"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="16sp"
                android:textStyle="bold"
                android:textColor="@android:color/black" />

            <TextView
                android:id="@+id/formula_expression"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="14sp"
                android:textColor="@android:color/darker_gray"
                android:fontFamily="monospace"
                android:layout_marginTop="4dp" />

            <TextView
                android:id="@+id/formula_category"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:textSize="12sp"
                android:textColor="@android:color/secondary_text_light"
                android:layout_marginTop="4dp" />

        </LinearLayout>

        <ImageView
            android:id="@+id/favorite_icon"
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:src="@android:drawable/btn_star_big_on"
            android:layout_gravity="center_vertical"
            android:visibility="gone" />

    </LinearLayout>

</LinearLayout>
*/

// R.layout.dialog_add_formula
/*
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="16dp">

        <EditText
            android:id="@+id/formula_name_edit_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Nombre de la fórmula"
            android:layout_marginBottom="12dp" />

        <EditText
            android:id="@+id/formula_expression_edit_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Expresión matemática"
            android:fontFamily="monospace"
            android:layout_marginBottom="12dp" />

        <EditText
            android:id="@+id/formula_description_edit_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Descripción"
            android:minLines="2"
            android:layout_marginBottom="12dp" />

        <Spinner
            android:id="@+id/formula_category_spinner"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginBottom="12dp" />

        <EditText
            android:id="@+id/formula_variables_edit_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:hint="Variables (separadas por comas)"
            android:layout_marginBottom="12dp" />

    </LinearLayout>

</ScrollView>
*/