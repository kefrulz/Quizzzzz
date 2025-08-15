package com.amg.quiz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.random.Random

data class QuizItem(
    val question: String,
    val options: List<String>,
    val correctIndex: Int?
)

data class PresentedItem(
    val question: String,
    val options: List<String>,
    val correctIndexInShuffled: Int?
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val all = loadQuestions(this, "questions_amg_pitesti_2025_with_answers.json")

        setContent {
            MaterialTheme {
                var seed by remember { mutableStateOf(System.currentTimeMillis()) }
                var session by remember(seed) { mutableStateOf(startSession(all, seed)) }
                var idx by remember { mutableStateOf(0) }
                val total = session.size
                val answers = remember(seed) { mutableStateListOf<Int?>(*Array(total){ null }) }
                var showSummary by remember { mutableStateOf(false) }

                Surface(Modifier.fillMaxSize()) {
                    if (showSummary) {
                        ResultsScreen(
                            items = session,
                            selected = answers.toList(),
                            onRetake = {
                                seed = System.currentTimeMillis()
                                session = startSession(all, seed)
                                idx = 0
                                answers.clear()
                                repeat(session.size) { answers.add(null) }
                                showSummary = false
                            }
                        )
                    } else {
                        QuizScreen(
                            item = session[idx],
                            qNumber = idx + 1,
                            total = total,
                            selected = answers[idx],
                            onSelect = { sel -> answers[idx] = sel },
                            onPrev = { if (idx > 0) idx-- },
                            onNext = { if (idx < total - 1) idx++ },
                            onFinish = { showSummary = true }
                        )
                    }
                }
            }
        }
    }

    private fun startSession(all: List<QuizItem>, seed: Long): List<PresentedItem> {
        val rng = Random(seed)
        val pick = all.shuffled(rng).take(45)
        return pick.map { qi ->
            val shuffled = qi.options.shuffled(Random(seed + qi.question.hashCode()))
            val correctInShuffled = qi.correctIndex?.let { origIdx ->
                val correctText = qi.options.getOrNull(origIdx)
                shuffled.indexOf(correctText).takeIf { it >= 0 }
            }
            PresentedItem(
                question = qi.question,
                options = shuffled,
                correctIndexInShuffled = correctInShuffled
            )
        }
    }
}

@Composable
fun QuizScreen(
    item: PresentedItem,
    qNumber: Int,
    total: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Întrebarea $qNumber din $total", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(item.question, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item.options.forEachIndexed { i, opt ->
                ElevatedCard(
                    onClick = { onSelect(i) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (selected == i) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = opt,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onPrev,
                enabled = qNumber > 1
            ) { Text("Înapoi") }

            if (qNumber < total) {
                Button(onClick = onNext) { Text("Înainte") }
            } else {
                Button(onClick = onFinish) { Text("Finalizează") }
            }
        }
    }
}

@Composable
fun ResultsScreen(items: List<PresentedItem>, selected: List<Int?>, onRetake: () -> Unit) {
    val total = items.size
    val correct = items.indices.count { i ->
        val sel = selected[i]
        val corr = items[i].correctIndexInShuffled
        sel != null && corr != null && sel == corr
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Rezultate", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Scor: $correct / $total", fontSize = 18.sp)
        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items.forEachIndexed { i, it ->
                val sel = selected[i]
                val corr = it.correctIndexInShuffled
                val isCorrect = sel != null && corr != null && sel == corr

                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Q${i + 1}. ${it.question}", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        if (sel != null) Text("Răspunsul tău: ${it.options[sel]}")
                        if (corr != null) Text("Răspuns corect: ${it.options[corr]}")
                        Spacer(Modifier.height(4.dp))
                        Text(if (isCorrect) "✅ Corect" else "❌ Incorect")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onRetake,
            modifier = Modifier.align(Alignment.End)
        ) { Text("Reia testul (45 aleator)") }
    }
}

fun loadQuestions(context: Context, assetName: String): List<QuizItem> {
    val json = context.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { it.readText() }
    val type = object : com.google.gson.reflect.TypeToken<List<QuizItem>>() {}.type
    return Gson().fromJson<List<QuizItem>>(json, type).filter { it.question.isNotBlank() && it.options.size >= 2 }
}
